// Copyright (C) 2017 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.analytics.test

import java.util.Date

import com.googlesource.gerrit.plugins.analytics.common.{AggregatedHistogramFilterByDates, AggregationStrategy}
import org.eclipse.jgit.revwalk.RevCommit
import org.gitective.core.CommitFinder
import org.scalatest.{FlatSpec, Matchers}

class AggregationSpec extends FlatSpec with Matchers with GitTestCase {

  import com.googlesource.gerrit.plugins.analytics.common.DateConversions._

  def newCommit(committer: String, when: String): RevCommit = {
    val date = new Date(isoStringToLongDate(when))
    val person = newPersonIdent(committer, s"$committer@company.com", date)
    add("afile.txt", date.toString, author = person, committer = author)
  }

  trait SomeSparseCommits {
    val c1 = newCommit("p0", "2017-08-01 15:00:00")
    val c2 = newCommit("p0", "2017-08-01 15:20:00")
    val c2a = newCommit("p0", "2017-08-01 16:20:00")
    val c3 = newCommit("p0", "2017-08-02 15:20:00")
    val c4 = newCommit("p0", "2017-09-02 15:20:00")
    val c5 = newCommit("p0", "2018-09-02 15:20:00")

    val cOther = newCommit("p1", "2017-08-01 15:00:01")

  }

  // should give 6 slots since c1,c2 are in the same hour and c2a is next hour
  // first slot with 2
  it should "do aggregation by hour" in new SomeSparseCommits {
    val filter = new AggregatedHistogramFilterByDates(
      aggregationStrategyMapping = AggregationStrategy.EMAIL_HOUR.aggregationStrategyMapping)
    new CommitFinder(testRepo).setFilter(filter).find

    val userActivity = filter.getHistogram.getAggregatedUserActivity
    userActivity should have size 6

    userActivity.foreach(activity => {
      if (activity.name == c1.getAuthorIdent.getName) {
        if (activity.getTimes.min == c1.getAuthorIdent.getWhen.getTime) {
          activity.getTimes should have size 2
        } else {
          activity.getTimes should have size 1
        }
      } else if (activity.name == cOther.getAuthorIdent.getName) {
        activity.getTimes should have size 1
      } else throw new Exception("Unknown commit")
    }
    )
  }

  // aggregating by day should give 5 slots:
  // p0 1/8/17 with 3 entries c1,c2,c2a
  // p0 2/8/17 with 1 entry c3
  // p0 2/9/17 with 1 entry c4
  // p0 2/9/18 with 1 entry c5
  // p1 1/8/17 with 1 entry c6
  "AggregatedHistogramFilter" should "do aggregation by day" in new SomeSparseCommits {

    val filter = new AggregatedHistogramFilterByDates(
      aggregationStrategyMapping = AggregationStrategy.EMAIL_DAY.aggregationStrategyMapping)
    new CommitFinder(testRepo).setFilter(filter).find

    val userActivity = filter.getHistogram.getAggregatedUserActivity
    userActivity should have size 5

    userActivity.foreach(activity => {
      if (activity.name == c1.getAuthorIdent.getName) {
        if (activity.getTimes.min == c1.getAuthorIdent.getWhen.getTime) {
          activity.getTimes should have size 3
        } else {
          activity.getTimes should have size 1
        }
      } else if (activity.name == cOther.getAuthorIdent.getName) {
        activity.getTimes should have size 1
      } else throw new Exception("Unknown commit")
    }
    )

  }



  // should give 4 slots
  // first slot of c1 with 4 and all the others with 1
  it should "do aggregation by month" in new SomeSparseCommits {
    val filter = new AggregatedHistogramFilterByDates(
      aggregationStrategyMapping = AggregationStrategy.EMAIL_MONTH.aggregationStrategyMapping)
    new CommitFinder(testRepo).setFilter(filter).find

    val userActivity = filter.getHistogram.getAggregatedUserActivity
    userActivity should have size 4

    userActivity.foreach(activity => {
      if (activity.name == c1.getAuthorIdent.getName) {
        if (activity.getTimes.min == c1.getAuthorIdent.getWhen.getTime) {
          activity.getTimes should have size 4
        } else {
          activity.getTimes should have size 1
        }
      } else if (activity.name == cOther.getAuthorIdent.getName) {
        activity.getTimes should have size 1
      } else throw new Exception("Unknown commit")
    }
    )

  }

  // should give 3 slots
  // first slot of c1 with 5 and all the others with 1
  it should "do aggregation by year" in new SomeSparseCommits {
    val filter = new AggregatedHistogramFilterByDates(
      aggregationStrategyMapping = AggregationStrategy.EMAIL_YEAR.aggregationStrategyMapping)
    new CommitFinder(testRepo).setFilter(filter).find

    val userActivity = filter.getHistogram.getAggregatedUserActivity
    userActivity should have size 3

    userActivity.foreach(activity => {
      if (activity.name == c1.getAuthorIdent.getName) {
        if (activity.getTimes.min == c1.getAuthorIdent.getWhen.getTime) {
          activity.getTimes should have size 5
        } else {
          activity.getTimes should have size 1
        }
      } else if (activity.name == cOther.getAuthorIdent.getName) {
        activity.getTimes should have size 1
      } else throw new Exception("Unknown commit")
    }
    )

  }

  // this should return only 2 slots, p0 with 6 entries, p1 with one entry
  it should "do aggregation by email" in new SomeSparseCommits {

    val filter = new AggregatedHistogramFilterByDates(
      aggregationStrategyMapping = AggregationStrategy.EMAIL.aggregationStrategyMapping)
    new CommitFinder(testRepo).setFilter(filter).find

    val userActivity = filter.getHistogram.getAggregatedUserActivity
    userActivity should have size 2

    userActivity.foreach(activity => {
      if (activity.name == c1.getAuthorIdent.getName) {
        activity.getTimes should have size 6
      }
      if (activity.email == cOther.getAuthorIdent.getName) {
        activity.getTimes should have size 1
      }

    })


  }

}
