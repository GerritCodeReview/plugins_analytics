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

import java.io.File
import java.util.{Date, UUID}

import com.google.gerrit.acceptance.{AbstractDaemonTest, GitUtil}
import com.google.gerrit.entities.Project
import com.google.gerrit.extensions.annotations.PluginName
import com.google.gerrit.extensions.client.SubmitType
import com.google.gerrit.acceptance._
import com.google.gerrit.extensions.restapi.RestApiModule
import com.google.gerrit.server.project.ProjectResource.PROJECT_KIND
import com.google.inject.AbstractModule
import com.googlesource.gerrit.plugins.analytics.{AnalyticsConfig, ContributorsResource}
import com.googlesource.gerrit.plugins.analytics.common.CommitsStatisticsCache
import org.eclipse.jgit.api.MergeCommand.FastForwardMode
import org.eclipse.jgit.api.{Git, MergeResult}
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.junit.TestRepository
import org.eclipse.jgit.lib._
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.revwalk.RevCommit
import org.gitective.core.CommitUtils
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.scalatest._

trait GerritTestDaemon extends BeforeAndAfterEach {
  self: Suite =>

  type TestFileRepository = TestRepository[FileRepository]

  val daemonTest = GerritTestDaemon

  protected abstract override def runTest(testName: String, args: Args): Status = {
    var status: Status = FailedStatus
    val runLambda = () => super.runTest(testName, args)

    daemonTest.testRunner.apply(new Statement() {
      override def evaluate(): Unit = {
        status = runLambda.apply()
      }
    }, Description.createTestDescription(getClass.getName, testName)).evaluate()

    status
  }

  var testFileRepository: TestFileRepository = _

  implicit var fileRepository: FileRepository = _

  implicit var fileRepositoryName: Project.NameKey = null

  protected lazy val author = newPersonIdent("Test Author", "author@test.com")

  protected lazy val committer = newPersonIdent("Test Committer", "committer@test.com")

  def newPersonIdent(name: String = "Test Person", email: String = "person@test.com", ts: Date = new Date()) =
    new PersonIdent(new PersonIdent(name, email), ts)

  override def beforeEach {
    daemonTest.setUpTestPlugin()
    fileRepositoryName = daemonTest.newProject(testSpecificRepositoryName)
    fileRepository = daemonTest.getRepository(fileRepositoryName)
    testFileRepository = GitUtil.newTestRepository(fileRepository)
  }

  private def testSpecificRepositoryName = "git-test-case-" + UUID.randomUUID().toString

  implicit class PimpedGitRepository(repo: TestFileRepository) {

    def branch(name: String, startPoint: String): Ref =
      repo.git.branchCreate.setName(name).setStartPoint(startPoint).call

    def gitClone: TestFileRepository = {
      val TmpDir = System.getProperty("java.io.tmpdir")
      val clonedRepository = Git.cloneRepository
        .setDirectory(new File(s"$TmpDir/${testSpecificRepositoryName}_clone"))
        .setURI(repo.getRepository.getDirectory.toURI.toString)
        .call()
        .getRepository.asInstanceOf[FileRepository]

      GitUtil.newTestRepository(clonedRepository)
    }

    def commitFile(path: String, content: String, author: PersonIdent = author, committer: PersonIdent = committer, branch: String = Constants.MASTER, message: String = ""): RevCommit =
      repo.branch(branch).commit()
        .add(path, content)
        .author(author)
        .committer(committer)
        .message(message)
        .create

    def commitFiles(files: Iterable[(String, String)], author: PersonIdent = author, committer: PersonIdent = committer, branch: String = Constants.MASTER, message: String = ""): RevCommit = {
      val commit = repo.branch(branch).commit()
        .author(author)
        .committer(committer)
        .message(message)

      files.foldLeft(commit) {
        (commit, fileAndContent) => commit.add(fileAndContent._1, fileAndContent._2)
      }.create()
    }

    def mergeCommitFile(fileName: String, content: String, author: PersonIdent = author, committer: PersonIdent = committer): MergeResult = {
      val currentBranch = repo.getRepository.getFullBranch
      val tmpBranch = repo.branch("tmp", currentBranch)
      try {
        repo.commitFile(fileName, content, branch = tmpBranch.getName, author = author, committer = committer)
        repo.mergeBranch(tmpBranch.getName, withCommit = true)
      } finally {
        repo.git.branchDelete().setBranchNames(tmpBranch.getName).setForce(true).call.get(0)
      }
    }

    def mergeBranch(branch: String, withCommit: Boolean, message: String = "merging branch"): MergeResult = {
      repo.git.merge.setStrategy(MergeStrategy.RESOLVE)
        .include(CommitUtils.getRef(repo.getRepository, branch))
        .setCommit(withCommit)
        .setFastForward(FastForwardMode.NO_FF)
        .setMessage(message)
        .call
    }

    def push =
      repo.git.push.setPushAll.call
  }
}

@TestPlugin(
  name = "analytics",
  sysModule = "com.googlesource.gerrit.plugins.analytics.test.GerritTestDaemon$TestModule"
)
object GerritTestDaemon extends LightweightPluginDaemonTest {
  baseConfig = new Config()
  AbstractDaemonTest.temporaryFolder.create()
  tempDataDir.create()

  def newProject(nameSuffix: String) = {
    resourcePrefix = ""
    super.createProjectOverAPI(nameSuffix, allProjects, false, SubmitType.MERGE_IF_NECESSARY)
  }

  def getRepository(projectName: Project.NameKey): FileRepository =
    repoManager.openRepository(projectName).asInstanceOf[FileRepository]

  def adminAuthor = admin.newIdent

  def getInstance[T](clazz: Class[T]): T =
    plugin.getSysInjector.getInstance(clazz)

  def getCanonicalWebUrl: String = canonicalWebUrl.get()

  def restSession: RestSession = adminRestSession

  class TestModule extends AbstractModule {
    override def configure(): Unit = {
      bind(classOf[CommitsStatisticsCache]).to(classOf[CommitsStatisticsNoCache])
      bind(classOf[AnalyticsConfig]).toInstance(TestAnalyticsConfig)
      install(new RestApiModule() {
        override protected def configure() = {
          get(PROJECT_KIND, "contributors").to(classOf[ContributorsResource])
        }
      })
    }
  }
}