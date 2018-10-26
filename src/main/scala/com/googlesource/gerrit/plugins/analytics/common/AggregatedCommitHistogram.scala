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
import com.googlesource.gerrit.plugins.analytics.common.AggregationStrategy.BY_BRANCH
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import org.gitective.core.stat.{CommitHistogram, CommitHistogramFilter, UserCommitActivity}

class AggregatedUserCommitActivity(val key: String, val name: String, val email: String)
  extends UserCommitActivity(name, email)

class AggregatedCommitHistogram(var aggregationStrategy: AggregationStrategy)
  extends CommitHistogram {

  def includeWithBranches(commit: RevCommit, user: PersonIdent, branches: Set[String]): Unit = {

    for ( branch <- branches ) {
      this.aggregationStrategy = BY_BRANCH(branch, aggregationStrategy)
      this.include(commit, user)
    }
  }

  override def include(commit: RevCommit, user: PersonIdent): AggregatedCommitHistogram = {
    val key = aggregationStrategy.mapping(user, commit.getAuthorIdent.getWhen)
    val activity = Option(users.get(key)) match {
      case None =>
        val newActivity = new AggregatedUserCommitActivity(key,
          user.getName, user.getEmailAddress)
        users.put(key, newActivity)
        newActivity
      case Some(foundActivity) => foundActivity
    }
    activity.include(commit, user)
    this
  }

  def getAggregatedUserActivity: Array[AggregatedUserCommitActivity] = {
    users.values.toArray(new Array[AggregatedUserCommitActivity](users.size))
  }
}

object AggregatedCommitHistogram {
  type AggregationStrategyMapping = (PersonIdent, Date) => String
}

abstract class AbstractCommitHistogramFilter(aggregationStrategy: AggregationStrategy)
  extends CommitHistogramFilter {
  val AbstractHistogram = new AggregatedCommitHistogram(aggregationStrategy)

  override def getHistogram = AbstractHistogram
}
