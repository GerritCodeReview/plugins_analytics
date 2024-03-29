// Copyright (C) 2019 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.analytics.common

import com.google.common.cache.CacheLoader
import com.google.gerrit.entities.Project
import com.google.gerrit.extensions.api.projects.CommentLinkInfo
import com.google.gerrit.server.git.GitRepositoryManager
import com.google.gerrit.server.project.ProjectCache
import com.google.inject.Inject
import com.googlesource.gerrit.plugins.analytics.{AnalyticsConfig, CommitInfo, IssueInfo}
import org.eclipse.jgit.diff.{DiffFormatter, RawTextComparator}
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.{CanonicalTreeParser, EmptyTreeIterator}
import org.eclipse.jgit.util.io.DisabledOutputStream

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import scala.util.Using
import scala.util.matching.Regex

class CommitsStatisticsLoader @Inject() (
  gitRepositoryManager: GitRepositoryManager,
  projectCache: ProjectCache,
  botLikeExtractor: BotLikeExtractor,
  config: AnalyticsConfig,
  ignoreFileSuffixFilter: IgnoreFileSuffixFilter
) extends CacheLoader[CommitsStatisticsCacheKey, CommitsStatistics] {
  import CommitsStatisticsLoader._

  override def load(cacheKey: CommitsStatisticsCacheKey): CommitsStatistics = {
    import RevisionBrowsingSupport._

    val objectId = cacheKey.commitId
    val nameKey = Project.nameKey(cacheKey.projectName)
    val commentInfoList: Seq[CommentLinkInfo] =
      if(config.isExtractIssues) projectCache.get(nameKey).toScala.toList.flatMap(_.getCommentLinks.asScala) else Seq.empty
    val replacers = commentInfoList.flatMap(info =>
      Option(info.link).map(link => Replacer(info.`match`.r, link))
    )

    Using.Manager { use =>
      val repo = use(gitRepositoryManager.openRepository(nameKey))

      // I can imagine this kind of statistics is already being available in Gerrit but couldn't understand how to access it
      // which Injection can be useful for this task?
      val rw = use(new RevWalk(repo))
      val reader = repo.newObjectReader()
      val commit = rw.parseCommit(objectId)
      val commitMessage = commit.getFullMessage

      val oldTree = {
        // protects against initial commit
        if (commit.getParentCount == 0)
          new EmptyTreeIterator
        else
          new CanonicalTreeParser(null, reader, rw.parseCommit(commit.getParent(0).getId).getTree)
      }

      val newTree = new CanonicalTreeParser(null, reader, commit.getTree)

      val df = new DiffFormatter(DisabledOutputStream.INSTANCE)
      df.setRepository(repo)
      df.setPathFilter(ignoreFileSuffixFilter)
      df.setDiffComparator(RawTextComparator.DEFAULT)
      df.setDetectRenames(true)
      val diffs = df.scan(oldTree, newTree).asScala

      val lines = (for {
        diff <- diffs
        edit <- df.toFileHeader(diff).toEditList.asScala
      } yield Lines(edit.getEndA - edit.getBeginA, edit.getEndB - edit.getBeginB)).fold(Lines(0, 0))(_ + _)

      val files: Set[String] = diffs.map(df.toFileHeader(_).getNewPath).toSet

      val commitInfo = CommitInfo(objectId.getName, commit.getAuthorIdent.getWhen.getTime, commit.isMerge, botLikeExtractor.isBotLike(files), files)
      val commitsStats = CommitsStatistics(lines.added, lines.deleted, commitInfo.merge, commitInfo.botLike, List(commitInfo), extractIssues(commitMessage, replacers).toList)

      commitsStats
    }
  }.get

  private def extractIssues(commitMessage: String, replacers: Seq[Replacer]): Seq[IssueInfo] =
    replacers.flatMap {
      case Replacer(pattern, replaced) =>
        pattern.findAllIn(commitMessage)
          .map(code => {
            val transformed = pattern.replaceAllIn(code, replaced)
            IssueInfo(code, transformed)
          })
    }
}

object CommitsStatisticsLoader {
  final private case class Replacer(pattern: Regex, replaced: String)

  final private case class Lines(deleted: Int, added: Int) {
    def +(other: Lines) = Lines(deleted + other.deleted, added + other.added)
  }
}
