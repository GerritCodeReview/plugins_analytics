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
import com.google.gerrit.acceptance.UseLocalDisk
import com.googlesource.gerrit.plugins.analytics.common.AggregationStrategy._
import com.googlesource.gerrit.plugins.analytics.common.DateConversions.StringOps
import com.googlesource.gerrit.plugins.analytics.common.TestUtils
import org.eclipse.jgit.revwalk.RevCommit
import org.scalatest.{FlatSpec, Inspectors, Matchers}

@UseLocalDisk
class AggregationSpec extends FlatSpec with Matchers with GerritTestDaemon with TestUtils with Inspectors {

  def commitAtDate(committer: String, when: String, content: String): RevCommit = {
    val personIdent = newPersonIdent(committer, committer, new Date(when.isoStringToLongDate))
    testFileRepository.commitFile("somefile", content, committer = personIdent, author = personIdent)
  }

  "AggregatedHistogramFilter by email and year" should "aggregate two commits from the same author the same year" in {
    commitAtDate("john", "2017-08-01", "first commit")
    commitAtDate("john", "2017-10-05", "second commit")

    val userActivity = aggregateBy(EMAIL_YEAR)

    userActivity should have size 1
    userActivity.head.getCount should be(2)
    userActivity.head.email should be("john")
  }

  it should "keep as separate rows activity from the same author on two different year" in {
    commitAtDate("john", "2017-08-01", "first commit")
    commitAtDate("john", "2018-09-01", "second commit")

    val userActivity = aggregateBy(EMAIL_YEAR)

    userActivity should have size 2
    forAll(userActivity) {
      activity => {
        activity.email should be("john")
        activity.getCount should be(1)
        1
      }
    }
  }

  it should "keep as separate rows activity from two different authors on the same year" in {
    commitAtDate("john", "2017-08-01", "first commit")
    commitAtDate("bob", "2017-12-05", "second commit")

    val userActivity = aggregateBy(EMAIL_YEAR)

    userActivity should have size 2
    userActivity.map(_.email) should contain allOf("john", "bob")
    forAll(userActivity) {
      _.getCount should be(1)
    }
  }

  "AggregatedHistogramFilter by email and month" should "aggregate two commits from the same author the same month" in {
    commitAtDate("john", "2017-08-01", "first commit")
    commitAtDate("john", "2017-08-05", "second commit")

    val userActivity = aggregateBy(EMAIL_MONTH)

    userActivity should have size 1
    userActivity.head.getCount should be(2)
    userActivity.head.email should be("john")
  }

  it should "keep as separate rows activity from the same author on two different months" in {
    commitAtDate("john", "2017-08-01", "first commit")
    commitAtDate("john", "2017-09-01", "second commit")

    val userActivity = aggregateBy(EMAIL_MONTH)

    userActivity should have size 2
    forAll(userActivity) {
      activity => {
        activity.email should be("john")
        activity.getCount should be(1)
        1
      }
    }
  }

  it should "keep as separate rows activity from two different authors on the same month" in {
    commitAtDate("john", "2017-08-01", "first commit")
    commitAtDate("bob", "2017-08-05", "second commit")

    val userActivity = aggregateBy(EMAIL_MONTH)

    userActivity should have size 2
    userActivity.map(_.email) should contain allOf("john", "bob")
    forAll(userActivity) {
      _.getCount should be(1)
    }
  }

  "AggregatedHistogramFilter by email and day" should "aggregate two commits of the same author the same day" in {
    commitAtDate("john", "2017-08-01", "first commit")
    commitAtDate("john", "2017-08-01", "second commit")

    val userActivity = aggregateBy(EMAIL_DAY)

    userActivity should have size 1
    userActivity.head.getCount should be(2)
    userActivity.head.email should be("john")
  }

  it should "keep as separate rows activity from the same author on two different days" in {
    commitAtDate("john", "2017-08-01", "first commit")
    commitAtDate("john", "2017-08-02", "second commit")

    val userActivity = aggregateBy(EMAIL_DAY)

    userActivity should have size 2
    forAll(userActivity) {
      activity => {
        activity.email should be("john")
        activity.getCount should be
        1
      }
    }
  }

  it should "keep as separate rows activity from two different authors on the same day" in {
    commitAtDate("john", "2017-08-01", "first commit")
    commitAtDate("bob", "2017-08-01", "second commit")

    val userActivity = aggregateBy(EMAIL_DAY)

    userActivity should have size 2
    userActivity.map(_.email) should contain allOf("john", "bob")
    forAll(userActivity) {
      _.getCount should be(1)
    }
  }

  "AggregatedHistogramFilter by email and hour" should "aggregate two commits of the same author on the same hour" in {
    commitAtDate("john", "2017-08-01 10:15:03", "first commit")
    commitAtDate("john", "2017-08-01 10:45:01", "second commit")

    val userActivity = aggregateBy(EMAIL_HOUR)

    userActivity should have size 1
    userActivity.head.email should be("john")
    userActivity.head.getCount should be(2)
  }

  it should "keep separate commits from the same author on different hours" in {
    commitAtDate("john", "2017-08-01 10:15:03", "first commit")
    commitAtDate("john", "2017-08-01 11:30:01", "second commit")

    val userActivity = aggregateBy(EMAIL_HOUR)

    userActivity should have size 2
    forAll(userActivity) {
      activity => {
        activity.email should be("john")
        activity.getCount should be(1)
      }
    }
  }

  it should "keep separate commits from different authors on the same hour" in {
    commitAtDate("john", "2017-08-01 10:15:03", "first commit")
    commitAtDate("bob", "2017-08-01 10:20:00", "second commit")

    val userActivity = aggregateBy(EMAIL_HOUR)

    userActivity should have size 2
    forAll(userActivity) {
      _.getCount should be(1)
    }
    userActivity.map(_.email) should contain allOf("john", "bob")
  }
}
