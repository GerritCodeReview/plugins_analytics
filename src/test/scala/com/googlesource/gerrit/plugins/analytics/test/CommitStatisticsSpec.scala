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

import com.googlesource.gerrit.plugins.analytics.common.{AggregatedHistogramFilterByDates, AggregatedUserCommitActivity, UserActivityHistogram}
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.{PersonIdent, Repository}
import org.eclipse.jgit.revwalk.RevCommit
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.collection.JavaConverters._

class CommitStatisticsSpec extends FlatSpec with GitTestCase with Matchers with Inside {

  class TestEnvironment {
    val repo = new FileRepository(testRepo)
  }

  def commit(fname: String, content: String): RevCommit = {
    val date = new Date()
    add(testRepo, "afile.txt", content, author, committer = author)
  }

  def aggregate(repo: Repository): AggregatedUserCommitActivity =
    (new UserActivityHistogram).get(repo, new AggregatedHistogramFilterByDates()).head

  "AggregatedCommit" should "stats a single file added" in new TestEnvironment {
    val change = commit("file1.txt", "line1\nline2")

    inside (aggregate(repo)) {
      case s =>

        s.nfiles should be(1)
        s.added should be(2)
        s.deleted should be (0)
    }
  }

  it should "stats multiple files added" in new TestEnvironment {
    val initial = commit("file1.txt", "line1\nline2\n")
    val second = add(testRepo,
      List("file1.txt", "file2.txt").asJava,
      List("line1\n", "line1\nline2\n").asJava, "second commit")
    inside(aggregate(repo)) { case s  =>
      s.nfiles should be(3)
      s.added should be(5)
      s.deleted should be(0)
    }
  }

  it should "stats lines eliminated" in new TestEnvironment {
    val initial = commit("file1.txt", "line1\nline2\nline3")
    val second = commit("file1.txt", "line1\n")
    inside(aggregate(repo)) { case s  =>
      s.nfiles should be(2)
      s.added should be(3)
      s.deleted should be(2)
    }
  }

  it should "stats a Seq[RevCommit]" in new TestEnvironment {
    val initial = add(testRepo,
      List("file1.txt", "file3.txt").asJava,
      List("line1\n", "line1\nline2\n").asJava, "first commit")
    val second = add(testRepo,
      List("file1.txt", "file2.txt").asJava,
      List("line1a\n", "line1\nline2\n").asJava, "second commit")
    inside(aggregate(repo)) { case s =>
      s.nfiles should be(4)
      s.added should be(6)
      s.deleted should be(1)
    }
  }
}
