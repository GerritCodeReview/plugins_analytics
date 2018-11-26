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

package com.googlesource.gerrit.plugins.analytics.test

import java.util.Date

import com.googlesource.gerrit.plugins.analytics.common.AggregationStrategy.EMAIL
import com.googlesource.gerrit.plugins.analytics.common.{AggregatedHistogramFilterByDates, BranchesExtractor, HashTagsExtractor}
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import org.gitective.core.CommitFinder
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

import scala.collection.mutable.{Map => MMap}

class AggregatedHistogramFilterByDatesSpec extends FlatSpec with GitTestCase with BeforeAndAfterEach with Matchers {


  "Author history filter" should
    "select one commit without intervals restriction" in {

    add("file.txt", "some content")
    val filter = new AggregatedHistogramFilterByDates()
    new CommitFinder(testRepo).setFilter(filter).find

    val userActivity = filter.getHistogram.getUserActivity
    filter.getHistogram.getUserActivity should have size 1
    val activity = userActivity.head
    activity.getCount should be(1)
    activity.getName should be(author.getName)
    activity.getEmail should be(author.getEmailAddress)
  }

  it should "select only the second of two commits based on the start timestamp" in {
    val firstCommitTs = add("file.txt", "some content")
      .getCommitterIdent.getWhen.getTime
    val person = newPersonIdent("Second person", "second@company.com", new Date(firstCommitTs + 1000L))
    val secondCommitTs = add("file.txt", "other content", author = person, committer = person)
      .getCommitterIdent.getWhen.getTime

    secondCommitTs should be > firstCommitTs

    val filter = new AggregatedHistogramFilterByDates(from = Some(secondCommitTs))
    new CommitFinder(testRepo).setFilter(filter).find

    val userActivity = filter.getHistogram.getUserActivity
    userActivity should have size 1
    val activity = userActivity.head

    activity.getTimes should have size 1
    activity.getName should be(person.getName)
    activity.getEmail should be(person.getEmailAddress)
  }

  it should "select only the first of two commits based on the end timestamp" in {
    val person = newPersonIdent("First person", "first@company.com")
    val firstCommitTs = add("file.txt", "some content", author = person, committer = person)
      .getCommitterIdent.getWhen.getTime
    val secondCommitTs = add("file.txt", "other content", committer = new PersonIdent(committer, new Date(firstCommitTs + 1000L)))
      .getCommitterIdent.getWhen.getTime

    secondCommitTs should be > firstCommitTs

    val filter = new AggregatedHistogramFilterByDates(to = Some(secondCommitTs))
    new CommitFinder(testRepo).setFilter(filter).find

    val userActivity = filter.getHistogram.getUserActivity
    userActivity should have size 1
    val activity = userActivity.head

    activity.getTimes should have size 1
    activity.getName should be(person.getName)
    activity.getEmail should be(person.getEmailAddress)
  }

  it should "select only one middle commit out of three based on interval from/to timestamp" in {
    val firstCommitTs = add("file.txt", "some content")
      .getCommitterIdent.getWhen.getTime
    val person = newPersonIdent("Middle person", "middle@company.com", new Date(firstCommitTs + 1000L))
    val middleCommitTs = add("file.txt", "other content", author = person, committer = person)
      .getCommitterIdent.getWhen.getTime
    val lastCommitTs = add("file.text", "yet other content", committer = new PersonIdent(committer, new Date(middleCommitTs + 1000L)))
      .getCommitterIdent.getWhen.getTime

    middleCommitTs should be > firstCommitTs
    lastCommitTs should be > middleCommitTs

    val filter = new AggregatedHistogramFilterByDates(from = Some(middleCommitTs), to = Some(lastCommitTs))
    new CommitFinder(testRepo).setFilter(filter).find

    val userActivity = filter.getHistogram.getUserActivity
    userActivity should have size 1
    val activity = userActivity.head

    activity.getTimes should have size 1
    activity.getName should be(person.getName)
    activity.getEmail should be(person.getEmailAddress)
  }

  it should "aggregate commits of the same user separately when they are in different branches and branchesExtractor is set" in {
    val repo = new FileRepository(testRepo)
    add("file1.txt", "add file1.txt to master branch")
    branch("another/branch")
    add("file2.txt", "add file2.txt to another/branch")
    val filter = new AggregatedHistogramFilterByDates(
      aggregationStrategy=EMAIL,
      branchesExtractor = Some(new BranchesExtractor(repo))
    )

    new CommitFinder(testRepo).setFilter(filter).find
    val userActivity = filter.getHistogram.getUserActivity

    userActivity should have size 2
  }

  it should "aggregate commits of the same user separately when they have different hashtags and hashtagsExtractor is set" in {

    case object FakeHashTagsExtractorImpl extends HashTagsExtractor {
      var commitToHashTags: MMap[String, Seq[String]] = MMap.empty
      override def findByCommit(commit: RevCommit): Seq[String] = commitToHashTags.getOrElse(commit.getName, Seq.empty[String])
    }

    def addHashTagsToCommit(commit: RevCommit, hashtags: Seq[String]): Unit = {
      FakeHashTagsExtractorImpl.commitToHashTags.put(commit.getName, hashtags.distinct)
    }

    val commit = add("file1.txt", "add file1.txt")
    addHashTagsToCommit(commit, Seq("hashtag1", "hashtag2"))

    val filter = new AggregatedHistogramFilterByDates(
      aggregationStrategy=EMAIL,
      hashTagsExtractor= Some(FakeHashTagsExtractorImpl)
    )

    new CommitFinder(testRepo).setFilter(filter).find
    val userActivity = filter.getHistogram.getUserActivity

    userActivity should have size 2
  }
}
