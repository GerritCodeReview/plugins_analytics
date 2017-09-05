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

import java.util.Date

import com.googlesource.gerrit.plugins.analytics.common.AggregatedCommitHistogram.AggregationStrategyMapping
import org.eclipse.jgit.diff.{DiffFormatter, RawTextComparator}
import org.eclipse.jgit.lib.{PersonIdent, Repository}
import org.eclipse.jgit.revwalk.{RevCommit, RevWalk}
import org.eclipse.jgit.treewalk.{CanonicalTreeParser, EmptyTreeIterator}
import org.eclipse.jgit.util.io.DisabledOutputStream
import org.gitective.core.stat.{CommitHistogram, CommitHistogramFilter, UserCommitActivity}

import scala.collection.JavaConversions._

class AggregatedUserCommitActivity(val key: String, val name: String, val email: String)
  extends UserCommitActivity(name, email) {

  var nfiles = 0
  var added = 0
  var deleted = 0

  def addStatistics(stats: CommitsStatistics): AggregatedUserCommitActivity = {
    nfiles += stats.numFiles
    added += stats.addedLines
    deleted += stats.deletedLines
    this
  }
}


case class CommitsStatistics(numFiles: Int, addedLines: Int, deletedLines: Int)

class AggregatedCommitHistogram(val aggregationStrategyForUser: AggregationStrategyMapping)
  extends CommitHistogram {

  private def getFileDifferences(repository: Repository, rw: RevWalk, commit: RevCommit): CommitsStatistics = {

    val reader = rw.getObjectReader
    val oldTree = {
      // protects against initial commit
      if (commit.getParentCount == 0)
        new EmptyTreeIterator
      else
        new CanonicalTreeParser(null, reader, rw.parseCommit(commit.getParent(0).getId).getTree)
    }

    val newTree = new CanonicalTreeParser(null, reader, commit.getTree)

    val df = new DiffFormatter(DisabledOutputStream.INSTANCE)

    df.setRepository(repository)
    df.setDiffComparator(RawTextComparator.DEFAULT)
    df.setDetectRenames(true)
    val diffs = df.scan(oldTree, newTree)
    case class Lines(deleted: Int, added: Int)
    val lines = for {
      diff <- diffs
      edit <- df.toFileHeader(diff).toEditList
    } yield Lines(edit.getEndA - edit.getBeginA, edit.getEndB - edit.getBeginB)
    val ret = CommitsStatistics(diffs.size, lines.map(_.added).sum, lines.map(_.deleted).sum)
    ret
  }

  override def include(repository: Repository, rw: RevWalk, commit: RevCommit, user: PersonIdent): AggregatedCommitHistogram = {
    val key = aggregationStrategyForUser(user, commit.getAuthorIdent.getWhen)
    val stats = getFileDifferences(repository, rw, commit)
    // user table defined for base class so have to cast to aggregated subclass
    Option(users.get(key).asInstanceOf[AggregatedUserCommitActivity]).getOrElse {
      val newActivity = new AggregatedUserCommitActivity(key,
        user.getName, user.getEmailAddress)
      users.put(key, newActivity)
      newActivity
    }.addStatistics(stats)
      .include(commit, user)
    this
  }

  def getAggregatedUserActivity: Array[AggregatedUserCommitActivity] = {
    users.values.toArray(new Array[AggregatedUserCommitActivity](users.size))
  }
}

object AggregatedCommitHistogram {
  type AggregationStrategyMapping = (PersonIdent, Date) => String

  def apply(aggregationStrategy: AggregationStrategyMapping) =
    new AggregatedCommitHistogram(aggregationStrategy)
}

abstract class AbstractCommitHistogramFilter(aggregationStrategyMapping: AggregationStrategyMapping)
  extends CommitHistogramFilter {
  val AbstractHistogram = new AggregatedCommitHistogram(aggregationStrategyMapping)

  override def getHistogram = AbstractHistogram
}
