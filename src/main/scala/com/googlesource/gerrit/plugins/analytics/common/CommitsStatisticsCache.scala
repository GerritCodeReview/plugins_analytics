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

import java.util.concurrent.Callable

import com.google.common.cache.Cache
import com.google.gerrit.extensions.api.projects.CommentLinkInfo
import com.googlesource.gerrit.plugins.analytics.{CommitInfo, IssueInfo}
import com.googlesource.gerrit.plugins.analytics.common.ManagedResource.use
import org.eclipse.jgit.diff.{DiffFormatter, RawTextComparator}
import org.eclipse.jgit.lib.{ObjectId, Repository}
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.{CanonicalTreeParser, EmptyTreeIterator}
import org.eclipse.jgit.util.io.DisabledOutputStream

import scala.util.matching.Regex
import scala.collection.JavaConverters._

trait CommitsStatisticsCache {

  def repo: Repository
  def botLikeExtractor: BotLikeExtractor
  def commentInfoList: List[CommentLinkInfo]

  def get(objectId: ObjectId): CommitsStatistics

  def loader(objectId: ObjectId): CommitsStatistics = {
    import RevisionBrowsingSupport._

    // I can imagine this kind of statistics is already being available in Gerrit but couldn't understand how to access it
    // which Injection can be useful for this task?
    use(new RevWalk(repo)) { rw =>
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
      df.setDiffComparator(RawTextComparator.DEFAULT)
      df.setDetectRenames(true)
      val diffs = df.scan(oldTree, newTree).asScala

      val lines = (for {
        diff <- diffs
        edit <- df.toFileHeader(diff).toEditList.asScala
      } yield Lines(edit.getEndA - edit.getBeginA, edit.getEndB - edit.getBeginB)).fold(Lines(0, 0))(_ + _)

      val files: Set[String] = diffs.map(df.toFileHeader(_).getNewPath).toSet

      val commitInfo = CommitInfo(objectId.getName, commit.getAuthorIdent.getWhen.getTime, commit.isMerge, botLikeExtractor.isBotLike(files), files)
      val commitsStats = CommitsStatistics(lines.added, lines.deleted, commitInfo.merge, commitInfo.botLike, List(commitInfo), extractIssues(commitMessage))

      commitsStats
    }
  }

  private val replacers = commentInfoList.map(info =>
    Replacer(
      info.`match`.r,
      Option(info.link).getOrElse(info.html)))

  private def extractIssues(commitMessage: String): List[IssueInfo] =
    replacers.flatMap {
      case Replacer(pattern, replaced) =>
        pattern.findAllIn(commitMessage)
          .map(code => {
            val transformed = pattern.replaceAllIn(code, replaced)
            IssueInfo(code, transformed)
          })
    }

  final private case class Replacer(pattern: Regex, replaced: String)

  final private case class Lines(deleted: Int, added: Int) {
    def +(other: Lines) = Lines(deleted + other.deleted, added + other.added)
  }
}

object CommitsStatisticsCache {
  final val COMMITS_STATISTICS_CACHE = "commits_statistics_cache"
}

case class CommitsStatisticsCacheImpl (commitStatsCache: Cache[ObjectId, CommitsStatistics],
  repo: Repository,
  botLikeExtractor: BotLikeExtractor,
  commentInfoList: List[CommentLinkInfo]
) extends CommitsStatisticsCache {

  override def get(objectId: ObjectId): CommitsStatistics =
    commitStatsCache.get(objectId, new Callable[CommitsStatistics] {
        override def call(): CommitsStatistics = {
          val stats = loader(objectId)
          commitStatsCache.put(objectId, stats)
          stats
        }
      })
}