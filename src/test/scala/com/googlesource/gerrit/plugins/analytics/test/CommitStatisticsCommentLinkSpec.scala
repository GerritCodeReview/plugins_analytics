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

import com.google.gerrit.acceptance.UseLocalDisk
import com.google.gerrit.extensions.api.projects.CommentLinkInfo
import com.google.gerrit.server.git.GitRepositoryManager
import com.googlesource.gerrit.plugins.analytics.IssueInfo
import com.googlesource.gerrit.plugins.analytics.common.{CommitsStatistics, Statistics}
import org.eclipse.jgit.lib.Repository
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._

@UseLocalDisk
class CommitStatisticsCommentLinkSpec extends AnyFlatSpecLike with GerritTestDaemon with TestCommitStatisticsNoCache with Matchers with Inside {

  def createCommentLinkInfo(pattern: String, link: Option[String] = None, html: Option[String] = None) = {
    val info = new CommentLinkInfo
    info.`match` = pattern
    info.link = link.getOrElse(null)
    info.html = html.getOrElse(null)
    info
  }

  class TestEnvironment(val repo: Repository = fileRepository) {
    lazy val stats = new Statistics(fileRepositoryName, commitsStatisticsNoCache)
    testFileRepository.commitFile("project.config",
      """
        |[access]
        |       inheritFrom = All-Projects
        |[submit]
        |       action = inherit
        |[commentlink "link1"]
        |       match = "(bug\\s+#?)(\\d+)"
        |       link = "http://bugs.example.com/show_bug.cgi?id=$2"
        |[commentlink "link2"]
        |       match = "([Bb]ug:\\s+)(\\d+)"
        |       link = "http://trak.example.com/$2" """.stripMargin, branch = "refs/meta/config")
    daemonTest.reloadProject(fileRepositoryName)
  }

  it should "collect no commentslink if no matching" in new TestEnvironment {
    val nocomments = testFileRepository.commitFile("file1.txt", "content1")

    inside(stats.forCommits(nocomments)) {
      case List(s: CommitsStatistics) =>
        s.issues should have size 0
    }
  }
  it should "collect simple bugzilla comments" in new TestEnvironment {
    val simpleComment = testFileRepository.commitFile("file1.txt", "content2", message =
      "this solves bug #23")

    inside(stats.forCommits(simpleComment)) {
      case List(s: CommitsStatistics) =>
        s.issues should have size 1
        s.issues should contain(IssueInfo("bug #23", "http://bugs.example.com/show_bug.cgi?id=23"))
    }
  }
  it should "collect simple track link" in new TestEnvironment {
    val simpleTrackComment = testFileRepository.commitFile("file1.txt", "content3", message
      = "this solves Bug: 1234")

    inside(stats.forCommits(simpleTrackComment)) {
      case List(s: CommitsStatistics) =>
        s.issues should have size 1
        s.issues should contain(IssueInfo("Bug: 1234", "http://trak.example.com/1234"))
    }
  }

  it should "collect multiple links" in new TestEnvironment {
    val multipleComments = testFileRepository.commitFile("file1.txt", "content4", message =
      "this solves bug 12 and Bug: 23")

    inside(stats.forCommits(multipleComments)) {
      case List(s: CommitsStatistics) =>
        s.issues should contain allOf(
          IssueInfo("bug 12", "http://bugs.example.com/show_bug.cgi?id=12"),
          IssueInfo("Bug: 23", "http://trak.example.com/23")
        )
    }
  }
}
