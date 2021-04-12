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

import com.google.gerrit.entities.Project
import com.googlesource.gerrit.plugins.analytics.{CommitInfo, IssueInfo}
import org.eclipse.jgit.lib.ObjectId

/** Collects overall stats on a series of commits and provides some basic info on the included commits
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
  require(
    commits.forall(_.botLike == isForBotLike),
    s"Creating a stats object with isForBotLike = $isForBotLike but containing commits of different type"
  )
  require(
    commits.forall(_.merge == isForMergeCommits),
    s"Creating a stats object with isMergeCommit = $isForMergeCommits but containing commits of different type"
  )

  private lazy val allFiles: List[String] = commits.flatMap(_.files)

  /** sum of the number of files in each of the included commits
    */
  lazy val numFiles: Int = allFiles.size

  /** number of distinct files the included commits have been touching
    */
  lazy val numDistinctFiles: Int = allFiles.toSet.size

  def isEmpty: Boolean = commits.isEmpty

  // Is not a proper monoid since we cannot sum a MergeCommit with a non merge one but it would overkill to define two classes
  def +(that: CommitsStatistics) = {
    require(
      this.isForMergeCommits == that.isForMergeCommits,
      "Cannot sum a merge commit stats with a non merge commit stats"
    )
    this.copy(
      addedLines = this.addedLines + that.addedLines,
      deletedLines = this.deletedLines + that.deletedLines,
      commits = this.commits ++ that.commits,
      issues = this.issues ++ that.issues
    )
  }
}

object CommitsStatistics {
  val EmptyNonMerge =
    CommitsStatistics(0, 0, false, false, List[CommitInfo](), List[IssueInfo]())
  val EmptyBotNonMerge = EmptyNonMerge.copy(isForBotLike = true)
  val EmptyMerge = EmptyNonMerge.copy(isForMergeCommits = true)
  val EmptyBotMerge = EmptyMerge.copy(isForBotLike = true)
}

class Statistics(
    projectNameKey: Project.NameKey,
    commitStatsCache: CommitsStatisticsCache
) {

  /** Returns up to four different CommitsStatistics object grouping the stats into:
    * Non Merge - Non Bot
    * Merge     - Non Bot
    * Non Merge - Bot
    * Merge     - Bot
    *
    * @param commits
    * @return
    */
  def forCommits(commits: ObjectId*): Iterable[CommitsStatistics] = {

    val stats = commits.map(commitStatsCache.get(projectNameKey.get(), _))

    val (mergeStatsSeq, nonMergeStatsSeq) = stats.partition(_.isForMergeCommits)

    val (mergeBotStatsSeq, mergeNonBotStatsSeq) =
      mergeStatsSeq.partition(_.isForBotLike)
    val (nonMergeBotStatsSeq, nonMergeNonBotStatsSeq) =
      nonMergeStatsSeq.partition(_.isForBotLike)

    List(
      nonMergeNonBotStatsSeq.foldLeft(CommitsStatistics.EmptyNonMerge)(
        _ + _
      ), // Non Merge - Non Bot
      mergeNonBotStatsSeq.foldLeft(CommitsStatistics.EmptyMerge)(
        _ + _
      ), // Merge     - Non Bot
      nonMergeBotStatsSeq.foldLeft(CommitsStatistics.EmptyBotNonMerge)(
        _ + _
      ), // Non Merge - Bot
      mergeBotStatsSeq.foldLeft(CommitsStatistics.EmptyBotMerge)(
        _ + _
      ) // Merge     - Bot
    )
      .filterNot(_.isEmpty)
  }

}
