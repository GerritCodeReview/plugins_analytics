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
import com.google.gerrit.reviewdb.client.{AccountGroup, Project}
import com.google.gerrit.server.git.{GitRepositoryManager, LocalDiskRepositoryManager}
import com.google.gerrit.server.project.{ProjectCache, ProjectState}
import com.googlesource.gerrit.plugins.analytics.IssueInfo
import com.googlesource.gerrit.plugins.analytics.common.{CommitsStatistics, Statistics}
import org.eclipse.jgit.lib.Repository
import org.scalatest.{FlatSpec, Inside, Matchers}

@UseLocalDisk
class CommitStatisticsCommentLinkSpec extends FlatSpec with GerritTestDaemon with Matchers with Inside {

  def createCommentLinkInfo(pattern: String, link: Option[String] = None, html: Option[String] = None) = {
    val info = new CommentLinkInfo
    info.`match` = pattern
    info.link = link.getOrElse(null)
    info.html = html.getOrElse(null)
    info
  }

  object TestRepositoryManager extends GitRepositoryManager {
    override def openRepository(name: Project.NameKey): Repository = new FileRepository(testRepo)

    override def createRepository(name: Project.NameKey): Repository = ???

    override def list(): util.SortedSet[Project.NameKey] = ???
  }

  object TestProjectCache extends ProjectCache {
    override def getAllProjects: ProjectState = ???

    override def getAllUsers: ProjectState = ???

    override def get(projectName: Project.NameKey): ProjectState = ???

    override def checkedGet(projectName: Project.NameKey): ProjectState = ???

    override def checkedGet(projectName: Project.NameKey, strict: Boolean): ProjectState = ???

    override def evict(p: Project): Unit = ???

    override def evict(p: Project.NameKey): Unit = ???

    override def remove(p: Project): Unit = ???

    override def remove(name: Project.NameKey): Unit = ???

    override def all(): ImmutableSortedSet[Project.NameKey] = ???

    override def guessRelevantGroupUUIDs(): util.Set[AccountGroup.UUID] = ???

    override def byName(prefix: String): ImmutableSortedSet[Project.NameKey] = ???

    override def onCreateProject(newProjectName: Project.NameKey): Unit = ???
  }

  class TestEnvironment(val repo: Repository = fileRepository,
                        val commentLinks: List[CommentLinkInfo] = List(
                          createCommentLinkInfo(pattern = "(bug\\s+#?)(\\d+)",
                            link = Some("http://bugs.example.com/show_bug.cgi?id=$2")),
                          createCommentLinkInfo(pattern = "([Bb]ug:\\s+)(\\d+)",
                            html = Some("$1<a href=\"http://trak.example.com/$2\">$2</a>")))) {
    lazy val stats = new Statistics(new Project.NameKey("testRepo"), CommitsStatisticsNoCache(new CommitsStatisticsLoader(TestRepositoryManager, TestProjectCache, new BotLikeExtractor() {
      override def isBotLike(files: Set[String]): Boolean = false
    })))
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
        s.issues should contain(IssueInfo("Bug: 1234", "Bug: <a href=\"http://trak.example.com/1234\">1234</a>"))
    }
  }

  it should "collect multiple links" in new TestEnvironment {
    val multipleComments = testFileRepository.commitFile("file1.txt", "content4", message =
      "this solves bug 12 and Bug: 23")

    inside(stats.forCommits(multipleComments)) {
      case List(s: CommitsStatistics) =>
        s.issues should contain allOf(
          IssueInfo("bug 12", "http://bugs.example.com/show_bug.cgi?id=12"),
          IssueInfo("Bug: 23", "Bug: <a href=\"http://trak.example.com/23\">23</a>")
        )
    }
  }
}
