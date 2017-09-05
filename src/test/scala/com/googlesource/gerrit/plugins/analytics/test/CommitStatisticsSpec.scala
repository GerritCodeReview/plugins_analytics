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
}
