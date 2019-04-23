// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.analytics.common

import com.google.gerrit.extensions.api.projects.CommentLinkInfo
import com.googlesource.gerrit.plugins.analytics.{CommitInfo, IssueInfo}
import com.googlesource.gerrit.plugins.analytics.common.ManagedResource.use
import org.eclipse.jgit.diff.{DiffFormatter, RawTextComparator}
import org.eclipse.jgit.lib._
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.filter.TreeFilter
import org.eclipse.jgit.treewalk.{CanonicalTreeParser, EmptyTreeIterator}
import org.eclipse.jgit.util.io.DisabledOutputStream
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.util.matching.Regex

/**
  * Collects overall stats on a series of commits and provides some basic info on the included commits
  *
  * @param addedLines           sum of the number of line additions in the included commits
  * @param deletedLines         sum of the number of line deletions in the included commits
  * @param isForMergeCommits    true if the current instance is including stats for merge commits and false if
  *                             calculated for NON merge commits. The current code is not generating stats objects for
  *                             a mixture of merge and non-merge commits
  * @param isForBotLike         true if the current instance is including BOT-like commits, false otherwise
  * @param commits              list of commits the stats are calculated for
  */
case class CommitsStatistics(
                              addedLines: Int,
                              deletedLines: Int,
                              isForMergeCommits: Boolean,
                              isForBotLike: Boolean,
                              commits: List[CommitInfo],
                              issues: List[IssueInfo] = Nil
                            ) {
  require(commits.forall(_.botLike == isForBotLike), s"Creating a stats object with isForBotLike = $isForBotLike but containing commits of different type")
  require(commits.forall(_.merge == isForMergeCommits), s"Creating a stats object with isMergeCommit = $isForMergeCommits but containing commits of different type")

  /**
    * sum of the number of files in each of the included commits
    */
  val numFiles: Int = commits.map(_.files.size).sum

  /**
    * number of distinct files the included commits have been touching
    */
  val numDistinctFiles: Int = changedFiles.size

  def isEmpty: Boolean = commits.isEmpty

  def changedFiles: Set[String] = commits.map(_.files.toSet).fold(Set.empty)(_ union _)

  // Is not a proper monoid since we cannot sum a MergeCommit with a non merge one but it would overkill to define two classes
  def + (that: CommitsStatistics) = {
    require(this.isForMergeCommits == that.isForMergeCommits, "Cannot sum a merge commit stats with a non merge commit stats")
    this.copy(
      addedLines = this.addedLines + that.addedLines,
      deletedLines = this.deletedLines + that.deletedLines,
      commits = this.commits ++ that.commits,
      issues = this.issues ++ that.issues
    )
  }
}

object CommitsStatistics {
  val EmptyNonMerge = CommitsStatistics(0, 0, false, false, List[CommitInfo](), List[IssueInfo]())
  val EmptyBotNonMerge = EmptyNonMerge.copy(isForBotLike = true)
  val EmptyMerge = EmptyNonMerge.copy(isForMergeCommits = true)
  val EmptyBotMerge = EmptyMerge.copy(isForBotLike = true)
}

class Statistics(
  repo: Repository,
  botLikeExtractor: BotLikeExtractor,
  treeFilter: TreeFilter = TreeFilter.ALL,
  commentInfoList: java.util.List[CommentLinkInfo] = Nil,
  cache: CacheApi[ObjectId, CommitsStatistics] = new InMemoryCommitStatisticsCache) {

  val log = LoggerFactory.getLogger(classOf[Statistics])
  val replacers = commentInfoList.map(info =>
    Replacer(
      info.`match`.r,
      Option(info.link).getOrElse(info.html)))

  /**
    * Returns up to four different CommitsStatistics object grouping the stats into:
    * Non Merge - Non Bot
    * Merge     - Non Bot
    * Non Merge - Bot
    * Merge     - Bot
    *
    * @param commits
    * @return
    */
  def forCommits(commits: ObjectId*): Iterable[CommitsStatistics] = {

    val stats = commits.map(cache.getOrUpdate(_, forSingleCommit))

    val (mergeStatsSeq, nonMergeStatsSeq) = stats.partition(_.isForMergeCommits)

    val (mergeBotStatsSeq, mergeNonBotStatsSeq) = mergeStatsSeq.partition(_.isForBotLike)
    val (nonMergeBotStatsSeq, nonMergeNonBotStatsSeq) = nonMergeStatsSeq.partition(_.isForBotLike)

    List(
      nonMergeNonBotStatsSeq.foldLeft(CommitsStatistics.EmptyNonMerge)(_ + _), // Non Merge - Non Bot
      mergeNonBotStatsSeq.foldLeft(CommitsStatistics.EmptyMerge)(_ + _),       // Merge     - Non Bot
      nonMergeBotStatsSeq.foldLeft(CommitsStatistics.EmptyBotNonMerge)(_ + _), // Non Merge - Bot
      mergeBotStatsSeq.foldLeft(CommitsStatistics.EmptyBotMerge)(_ + _)        // Merge     - Bot
    )
    .filterNot(_.isEmpty)
  }

  protected def forSingleCommit(objectId: ObjectId): CommitsStatistics = {
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

      use(new DiffFormatter(DisabledOutputStream.INSTANCE)) { df =>
        df.setRepository(repo)
        df.setContext(0)
        df.setDiffComparator(RawTextComparator.DEFAULT)
        df.setPathFilter(treeFilter)
        df.setDetectRenames(true)
        val diffs = df.scan(oldTree, newTree)

        val lines = (for {
          diff <- diffs
          edit <- df.toFileHeader(diff).toEditList
        } yield Lines(edit.getEndA - edit.getBeginA, edit.getEndB - edit.getBeginB)).fold(Lines(0, 0))(_ + _)

        val files: Set[String] = diffs.map(df.toFileHeader(_).getNewPath).toSet

        val commitInfo = CommitInfo(objectId.getName, commit.getAuthorIdent.getWhen.getTime, commit.isMerge, botLikeExtractor.isBotLike(files), files)
        CommitsStatistics(lines.added, lines.deleted, commitInfo.merge, commitInfo.botLike, List(commitInfo), extractIssues(commitMessage))
      }
    }
  }

  def extractIssues(commitMessage: String): List[IssueInfo] = {
    replacers.flatMap {
      case Replacer(pattern, replaced) =>
        pattern.findAllIn(commitMessage)
          .map(code => {
            val transformed = pattern.replaceAllIn(code, replaced)
            IssueInfo(code, transformed)
          })
    }.toList
  }

  case class Replacer(pattern: Regex, replaced: String)

  case class Lines(deleted: Int, added: Int) {
    def +(other: Lines) = Lines(deleted + other.deleted, added + other.added)
  }

}
