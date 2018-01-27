// Copyright (C) 2018 The Android Open Source Project
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

import java.util.{Arrays, Date}

import com.google.gerrit.extensions.api.projects.CommentLinkInfo
import com.googlesource.gerrit.plugins.analytics.IssueInfo
import com.googlesource.gerrit.plugins.analytics.common.{CommitsStatistics, Statistics}
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.revwalk.RevCommit
import org.scalatest.{FlatSpec, Inside, Matchers}
import scala.collection.JavaConverters._

class CommitStatisticsCommentLinkSpec extends FlatSpec with GitTestCase with Matchers with Inside {

  def createCommentLinkInfo(pattern: String, link: Option[String] = None, html: Option[String] = None) = {
    val info = new CommentLinkInfo
    info.`match` = pattern
    info.link = link.getOrElse(null)
    info.html = html.getOrElse(null)
    info
  }

  def commit(committer: String, fileName: String, content: String, message: Option[String] = None): RevCommit = {
    val date = new Date()
    val person = newPersonIdent(committer, committer, date)
    add(testRepo, fileName, content, author = person, committer = author, message = message.getOrElse("** no message **"))
  }

  class TestEnvironment(val repo: FileRepository = new FileRepository(testRepo),
                        val commentLinks: java.util.List[CommentLinkInfo] = Seq(
                          createCommentLinkInfo(pattern = "(bug\\s+#?)(\\d+)",
                            link = Some("http://bugs.example.com/show_bug.cgi?id=$2")),
                          createCommentLinkInfo(pattern = "([Bb]ug:\\s+)(\\d+)",
                            html = Some("$1<a href=\"http://trak.example.com/$2\">$2</a>"))).asJava) {

    lazy val stats = new Statistics(repo, commentLinks)
  }

  it should "collect no commentslink if no matching" in new TestEnvironment {
    val nocomments = commit("user", "file1.txt", "content1")

    inside(stats.forCommits(nocomments)) {
      case List(s: CommitsStatistics) =>
        s.issues should have size 0
    }

  }
  it should "collect simple bugzilla comments" in new TestEnvironment {
    val simpleComment = commit("user", "file1.txt", "content2", message =
      Some("this solves bug #23"))

    inside(stats.forCommits(simpleComment)) {
      case List(s: CommitsStatistics) =>
        s.issues should have size 1
        s.issues should contain(IssueInfo("bug #23", "http://bugs.example.com/show_bug.cgi?id=23"))
    }

  }
  it should "collect simple track link" in new TestEnvironment {
    val simpleTrackComment = commit("user", "file1.txt", "content3", message
      = Some("this solves Bug: 1234"))

    inside(stats.forCommits(simpleTrackComment)) {
      case List(s: CommitsStatistics) =>
        s.issues should have size 1
        s.issues should contain(IssueInfo("Bug: 1234", "Bug: <a href=\"http://trak.example.com/1234\">1234</a>"))
    }

  }
  it should "collect multiple links" in new TestEnvironment {
    val multipleComments = commit("user", "file1.txt", "content4", message =
      Some("this solves bug 12 and Bug: 23"))

    inside(stats.forCommits(multipleComments)) {
      case List(s: CommitsStatistics) =>
        s.issues should contain allOf(
          IssueInfo("bug 12", "http://bugs.example.com/show_bug.cgi?id=12"),
          IssueInfo("Bug: 23", "Bug: <a href=\"http://trak.example.com/23\">23</a>")
        )
    }

  }

}
