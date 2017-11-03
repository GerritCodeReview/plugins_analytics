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

import com.googlesource.gerrit.plugins.analytics.common.{CommitsStatistics, Statistics}
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.revwalk.RevCommit
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.collection.JavaConverters._

class CommitStatisticsSpec extends FlatSpec with GitTestCase with Matchers with Inside {


  class TestEnvironment {
    val repo = new FileRepository(testRepo)
    val stats = new Statistics(repo)
  }

  def commit(committer: String, fname: String, content: String): RevCommit = {
    val date = new Date()
    val person = newPersonIdent(committer, committer, date)
    add(testRepo, "afile.txt", content, author = person, committer = author)
  }

  "CommitStatistics" should "stats a single file added" in new TestEnvironment {
    val change = commit("user", "file1.txt", "line1\nline2")

    inside(stats.find(change)) { case s: CommitsStatistics =>
      s.numFiles should be(1)
      s.addedLines should be(2)
      s.deletedLines should be(0)
    }
  }

  it should "stats multiple files added" in new TestEnvironment {
    val initial = commit("user", "file1.txt", "line1\nline2\n")
    val second = add(testRepo,
      List("file1.txt", "file2.txt").asJava,
      List("line1\n", "line1\nline2\n").asJava, "second commit")
    inside(stats.find(second)) { case s: CommitsStatistics =>
      s.numFiles should be(2)
      s.addedLines should be(3)
      s.deletedLines should be(0)
    }
  }

  it should "stats lines eliminated" in new TestEnvironment {
    val initial = commit("user", "file1.txt", "line1\nline2\nline3")
    val second = commit("user", "file1.txt", "line1\n")
    inside(stats.find(second)) { case s: CommitsStatistics =>
      s.numFiles should be(1)
      s.addedLines should be(0)
      s.deletedLines should be(2)
    }
  }

  it should "stats a Seq[RevCommit]" in new TestEnvironment {
    val initial = add(testRepo,
      List("file1.txt", "file3.txt").asJava,
      List("line1\n", "line1\nline2\n").asJava, "first commit")
    val second = add(testRepo,
      List("file1.txt", "file2.txt").asJava,
      List("line1a\n", "line1\nline2\n").asJava, "second commit")
    inside(stats.find(List(initial, second))) { case s: CommitsStatistics =>
      s.numFiles should be(4)
      s.addedLines should be(6)
      s.deletedLines should be(1)
    }
  }

  it should "not return any stats if the commit does not include any file" in new TestEnvironment {
    val emptyCommit = add(testRepo,
      List.empty[String].asJava,
      List.empty[String].asJava, "Empty commit")
    stats.find(emptyCommit) shouldBe CommitsStatistics.Empty
  }
}
