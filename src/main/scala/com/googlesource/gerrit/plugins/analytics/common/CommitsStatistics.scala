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

import com.googlesource.gerrit.plugins.analytics.CommitInfo
import com.googlesource.gerrit.plugins.analytics.common.ManagedResource.use
import org.eclipse.jgit.diff.{DiffFormatter, RawTextComparator}
import org.eclipse.jgit.lib.{ObjectId, Repository}
import org.eclipse.jgit.revwalk.{RevCommit, RevWalk}
import org.eclipse.jgit.treewalk.{CanonicalTreeParser, EmptyTreeIterator}
import org.eclipse.jgit.util.io.DisabledOutputStream

import scala.collection.JavaConversions._

case class CommitsStatistics(numFiles: Int, addedLines: Int, deletedLines: Int, isForMergeCommits: Boolean, commits: List[CommitInfo]) {
  require(commits.forall(_.merge == isForMergeCommits), s"Creating a stats object with isMergeCommit = $isForMergeCommits but containing commits of different type")

  def isEmpty = commits.isEmpty

  // Is not a proper monoid since we cannot sum a MergeCommit with a non merge one but it would overkill to define two classes
  protected [common] def + (that: CommitsStatistics) = {
    require(this.isForMergeCommits == that.isForMergeCommits, "Cannot sum a merge commit stats with a non merge commit stats")
    this.copy(
      numFiles = this.numFiles + that.numFiles,
      addedLines = this.addedLines + that.addedLines,
      deletedLines = this.deletedLines + that.deletedLines,
      commits = this.commits ++ that.commits
    )
  }
}

object CommitsStatistics {
  val Empty = CommitsStatistics(0, 0, 0, false, List.empty)
  val EmptyMerge = Empty.copy(isForMergeCommits = true)
}

class Statistics(repo: Repository) {

  /**
    * Returns up to two different CommitsStatistics object grouping the stats into merge and non-merge commits
    *
    * @param commits
    * @return
    */
  def forCommits(commits: ObjectId*): Iterable[CommitsStatistics] = {

    val stats = commits.map(forSingleCommit)

    val nonMergeStats = stats.filterNot(_.isForMergeCommits).foldLeft(CommitsStatistics.Empty)(_ + _)
    val mergeStats = stats.filter(_.isForMergeCommits).foldLeft(CommitsStatistics.EmptyMerge)(_ + _)

    List(nonMergeStats, mergeStats).filterNot(_.isEmpty)
  }

  protected def forSingleCommit(objectId: ObjectId): CommitsStatistics = {
    import RevisionBrowsingSupport._

    // I can imagine this kind of statistics is already being available in Gerrit but couldn't understand how to access it
    // which Injection can be useful for this task?
    use(new RevWalk(repo)) { rw =>
      val reader = repo.newObjectReader()
      val commit = rw.parseCommit(objectId)

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
      val diffs = df.scan(oldTree, newTree)
      case class Lines(deleted: Int, added: Int) {
        def +(other: Lines) = Lines(deleted + other.deleted, added + other.added)
      }
      val lines = (for {
        diff <- diffs
        edit <- df.toFileHeader(diff).toEditList
      } yield Lines(edit.getEndA - edit.getBeginA, edit.getEndB - edit.getBeginB)).fold(Lines(0, 0))(_ + _)

      val commitInfo = CommitInfo(objectId.getName, commit.getAuthorIdent.getWhen.getTime, commit.isMerge)

      CommitsStatistics(diffs.size, lines.added, lines.deleted, commitInfo.merge, List(commitInfo))
    }
  }

}
