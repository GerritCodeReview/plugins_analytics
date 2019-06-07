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

import com.google.gerrit.acceptance.UseLocalDisk
import com.googlesource.gerrit.plugins.analytics.CommitInfo
import com.googlesource.gerrit.plugins.analytics.common.{CommitsStatistics, Statistics}
import org.scalatest.{FlatSpec, Inside, Matchers}

@UseLocalDisk
class CommitStatisticsSpec extends FlatSpec with GerritTestDaemon with TestCommitStatisticsNoCache with Matchers with Inside {
  class TestEnvironment {
    val repo = fileRepository
    val stats = new Statistics(fileRepositoryName, commitsStatisticsNoCache)
  }

  "CommitStatistics" should "stats a single file added" in new TestEnvironment {
    val change = testFileRepository.commitFile("file1.txt", "line1\nline2")

    inside(stats.forCommits(change)) { case List(s: CommitsStatistics) =>
      s.numFiles should be(1)
      s.addedLines should be(2)
      s.deletedLines should be(0)
    }
  }

  it should "sum to another compatible CommitStatistics generating an aggregated stat" in {
    val commit1 = CommitInfo("sha_1", 1000l, false, botLike = false, Set("file1"))
    val commit2 = CommitInfo("sha_2", 2000l, false, botLike = false, Set("file1"))
    val commit3 = CommitInfo("sha_3", 3000l, false, botLike = false, Set("file2"))
    val commit4 = CommitInfo("sha_4", 1000l, false, botLike = false, Set("file1"))

    val stat1 = CommitsStatistics(3, 4, false, false, List(commit1, commit2))
    val stat2 = CommitsStatistics(5, 7, false, false, List(commit3, commit4))

    (stat1 + stat2) shouldBe CommitsStatistics(8, 11, false, false, List(commit1, commit2, commit3, commit4))
  }

  it should "fail if trying to be added to a CommitStatistics object for a different isMerge value" in {
    an [IllegalArgumentException] should be thrownBy  (CommitsStatistics.EmptyMerge + CommitsStatistics.EmptyNonMerge)
  }

  it should "stats multiple files added" in new TestEnvironment {
    val initial = testFileRepository.commitFile("file1.txt", "line1\nline2\n")
    val second = testFileRepository.commitFiles(
      List(
        "file1.txt" -> "line1\n",
        "file2.txt" -> "line1\nline2\n"
      ), message = "second commit")

    inside(stats.forCommits(second)) { case List(s: CommitsStatistics) =>
      s.numFiles should be(2)
      s.addedLines should be(2)
      s.deletedLines should be(1)
    }
  }

  it should "stats lines eliminated" in new TestEnvironment {
    val initial = testFileRepository.commitFile( "file1.txt", "line1\nline2\nline3")
    val second = testFileRepository.commitFile( "file1.txt", "line1\n")
    inside(stats.forCommits(second)) { case List(s: CommitsStatistics) =>
      s.numFiles should be(1)
      s.addedLines should be(0)
      s.deletedLines should be(2)
    }
  }

  it should "stats a Seq[RevCommit]" in new TestEnvironment {
    val initial = testFileRepository.commitFiles(
      List(
        "file1.txt" -> "line1\n",
        "file3.txt" -> "line1\nline2\n"),
      message = "first commit")

    val second = testFileRepository.commitFiles(
      List(
        "file1.txt" -> "line1a\n",
        "file2.txt" -> "line1\nline2\n"),
      message = "second commit")

    inside(stats.forCommits(initial, second)) { case List(nonMergeStats: CommitsStatistics) =>
      nonMergeStats.numFiles should be(4)
      nonMergeStats.numDistinctFiles should be(3)
      nonMergeStats.addedLines should be(6)
      nonMergeStats.deletedLines should be(1)
    }
  }

  it should "return zero value stats if the commit does not include any file" in new TestEnvironment {
    val emptyCommit = testFileRepository.commitFiles(List.empty, message = "Empty commit")
    inside(stats.forCommits(emptyCommit)) { case List(stats) =>
      stats.numFiles should be(0)
      stats.addedLines should be(0)
      stats.deletedLines should be(0)
    }
  }

  it should "split merge commits and non-merge commits" in new TestEnvironment {
    val clonedRepo = testFileRepository.gitClone
    val firstNonMerge = clonedRepo.commitFile("file1.txt", "line1\nline2\n")
    val merge = clonedRepo.mergeCommitFile("file1.txt", "line1\nline2\nline3")
    val nonMerge = clonedRepo.commitFiles(
      List(
        "file1.txt" -> "line1\n",
        "file2.txt" -> "line1\nline2\n"),
      message = "second commit")
    clonedRepo.push

    inside(stats.forCommits(firstNonMerge, merge.getNewHead, nonMerge)) {
      case List(nonMergeStats, mergeStats) =>
        mergeStats.numFiles should be(1)
        mergeStats.addedLines should be(1)
        mergeStats.deletedLines should be(0)

        nonMergeStats.numFiles should be(3)
        nonMergeStats.addedLines should be(4)
        nonMergeStats.deletedLines should be(2)

      case wrongContent => fail(s"Expected two results instead got $wrongContent")
    }
  }
}
