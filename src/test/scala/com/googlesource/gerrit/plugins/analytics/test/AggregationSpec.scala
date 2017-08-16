// Copyright (C) 2016 The Android Open Source Project
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

import java.security.InvalidParameterException
import java.util.Date
import java.util.concurrent.TimeUnit

import com.googlesource.gerrit.plugins.analytics.common.{AggregatedHistogramFilterByDates, AggregationStrategy}
import org.eclipse.jgit.lib.PersonIdent
import org.gitective.core.CommitFinder
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

class AggregationSpec extends FlatSpec with Matchers with GitTestCase {

  def offset(unit: TimeUnit, count: Int): Date = {
    return new Date(System.currentTimeMillis() + unit.toMillis(count))
  }

  "AggregationStrategy" should
    "have valid constructors" in {
    val p = new PersonIdent("name", "email")
    val d = new Date
    AggregationStrategy("EMAIL").aggregationStrategyMapping(p, d)
      .split("/") should have size 1
    AggregationStrategy("YEAR").aggregationStrategyMapping(p, d)
      .split("/") should have size 2
    AggregationStrategy("MONTH").aggregationStrategyMapping(p, d)
      .split("/") should have size 3
    AggregationStrategy("DAY").aggregationStrategyMapping(p, d)
      .split("/") should have size 4
    AggregationStrategy("HOUR").aggregationStrategyMapping(p, d)
      .split("/") should have size 5
    AggregationStrategy("hour") == AggregationStrategy.HOUR
    intercept[InvalidParameterException] {
      AggregationStrategy("INVALID")
    }
  }
  // p0 has a commit today
  // p1 has a commit today and one tomorrow
  // aggregation by day gives 3 slots

  "AggregatedHistogramFilter" should "do aggregation by day" in {
    add("file.txt", "some content")

    val personToday = newPersonIdent("Second person", "second@company.com", offset(TimeUnit.DAYS, 0))
    val personTomorrow = newPersonIdent("Second person", "second@company.com", offset(TimeUnit.DAYS, 1))
    // put second commit same day
    add("file.txt", "other content", author = personToday, committer = personToday)
    // put another third commit 1 day in the future
    val thirdCommit = add("file2.txt", "other", author = personTomorrow, committer = personTomorrow)

    val filter = new AggregatedHistogramFilterByDates(
      aggregationStrategyMapping = AggregationStrategy.DAY.aggregationStrategyMapping)
    new CommitFinder(testRepo).setFilter(filter).find

    val userActivity = filter.getHistogram.getAggregatedUserActivity
    userActivity should have size 3
    val activity0 = userActivity(0)

    activity0.getTimes should have size 1
    activity0.getName should be(author.getName)
    activity0.getEmail should be(author.getEmailAddress)

    val activity1 = userActivity(1)

    activity1.getTimes should have size 1
    activity1.getName should be(personToday.getName)
    activity1.getEmail should be(personToday.getEmailAddress)

    val activity2 = userActivity(2)

    activity2.getTimes should have size 1
    activity2.getName should be(personToday.getName)
    activity2.getEmail should be(personToday.getEmailAddress)


    activity0.key.split("/") should have size 4
    activity1.key.split("/") should have size 4

  }


  it should "do aggregation by email" in {
    val firstCommitTs = add("file.txt", "some content")
      .getCommitterIdent.getWhen.getTime
    // second and third commit are set 1 day in the future
    val person = newPersonIdent("Second person", "second@company.com", offset(TimeUnit.DAYS, 1))
    val secondCommitTs = add("file.txt", "other content", author = person, committer = person)
      .getCommitterIdent.getWhen.getTime
    // put another third commit 1 day as well in the future
    val thirdCommit = add("file2.txt", "other", author = person, committer = person)

    val filter = new AggregatedHistogramFilterByDates(
      aggregationStrategyMapping = AggregationStrategy.EMAIL.aggregationStrategyMapping)
    new CommitFinder(testRepo).setFilter(filter).find

    val userActivity = filter.getHistogram.getAggregatedUserActivity
    userActivity should have size 2
    val activity0 = userActivity(0)

    activity0.getTimes should have size 1
    activity0.getName should be(author.getName)
    activity0.getEmail should be(author.getEmailAddress)

    val activity1 = userActivity(1)

    activity1.getTimes should have size 2
    activity1.getName should be(person.getName)
    activity1.getEmail should be(person.getEmailAddress)

  }


}
