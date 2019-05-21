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
import java.text.MessageFormat
import java.util.{Date, UUID}

import com.google.gerrit.acceptance.{AbstractDaemonTest, GitUtil, UseLocalDisk}
import com.google.gerrit.reviewdb.client.Project
import org.eclipse.jgit.api.MergeCommand.FastForwardMode
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.{Git, MergeResult}
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.junit.TestRepository
import org.eclipse.jgit.lib._
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.revwalk.RevCommit
import org.gitective.core.CommitUtils
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.scalatest.{Args, BeforeAndAfterEach, Status, Suite}


/**
  * Base test case with utilities for common Git operations performed during
  * testing
  */
trait GitTestCase extends BeforeAndAfterEach {
  self: Suite =>

  val TmpDir = System.getProperty("java.io.tmpdir")

  type GitRepository = TestRepository[FileRepository]

  val daemonTest = GerritDaemonTest

  protected abstract override def runTest(testName: String, args: Args): Status = {
    var status: Option[Status] = None
    val runLamba = () => super.runTest(testName, args)

    val result = daemonTest.testRunner.apply(new Statement() {
      override def evaluate(): Unit = {
        status = Some(runLamba.apply())
      }
    }, Description.createTestDescription(getClass.getName, testName)).evaluate()

    status.get
  }

  protected var testRepo: GitRepository = null

  /**
    * Author used for commits
    */
  protected lazy val author = daemonTest.adminAuthor

  def newPersonIdent(name: String = "Test Person", email: String = "person@test.com", ts: Date = new Date()) =
    new PersonIdent(new PersonIdent(name, email), ts)

  /**
    * Committer used for commits
    */
  protected val committer = new PersonIdent("Test Committer", "committer@test.com")

  override def beforeEach {
    testRepo = GitUtil.newTestRepository(
      daemonTest.getRepository(
        daemonTest.newProject(testSpecificRepositoryName)))
  }

  private def testSpecificRepositoryName = "git-test-case-" + UUID.randomUUID().toString

  implicit class PimpedGitRepository(repo: GitRepository) {

    def branch(name: String, startPoint: String): Ref =
      repo.git.branchCreate.setName(name).setStartPoint(startPoint).call

    def gitClone: GitRepository = {
      val clonedRepository = Git.cloneRepository
        .setDirectory(new File(s"$TmpDir/${testSpecificRepositoryName}_clone"))
        .setURI(repo.getRepository.getDirectory.toURI.toString)
        .call()
        .getRepository.asInstanceOf[FileRepository]

      GitUtil.newTestRepository(clonedRepository)
    }

    def commitFile(path: String, content: String, author: PersonIdent = author, committer: PersonIdent = committer, branch: String = "refs/heads/master", message: String = ""): RevCommit =
      repo.branch(branch).commit()
        .add(path, content)
        .author(author)
        .committer(committer)
        .message(message)
        .create

    def commitFiles(files: Iterable[(String, String)], author: PersonIdent = author, committer: PersonIdent = committer, branch: String = "refs/heads/master", message: String = ""): RevCommit = {
      val commit = repo.branch(branch).commit()
        .author(author)
        .committer(committer)
        .message(message)

      files.foldLeft(commit) {
        (commit, fileAndContent) => commit.add(fileAndContent._1, fileAndContent._2)
      }.create()
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


  /**
    * commit specified content into a file, as committer and merge into current branch
    *
    * @param committer - the author of this commit
    * @param fileName  - the name of the file
    * @param content   - the content of the file
    * @return MergeResult
    *
    */
  protected def mergeCommit(committer: String, fileName: String, content: String, repo: GitRepository = testRepo): MergeResult = {
    val currentBranch = repo.getRepository.getFullBranch
    val personIdent = newPersonIdent(committer, committer)
    val tmpBranch = repo.branch("tmp", currentBranch)
    try {
      repo.commitFile(fileName, content, branch = tmpBranch.getName, author = personIdent, committer = personIdent)
      repo.mergeBranch(tmpBranch.getName, withCommit = true)
    } finally {
      repo.git.branchDelete().setBranchNames(tmpBranch.getName).setForce(true).call.get(0)
    }
  }
}

@UseLocalDisk
object GerritDaemonTest extends AbstractDaemonTest {
  baseConfig = new Config()

  def newProject(nameSuffix: String) = {
    resourcePrefix = ""
    super.createProject(nameSuffix, allProjects, false)
  }

  def getRepository(projectName: Project.NameKey): FileRepository = repoManager.openRepository(projectName).asInstanceOf[FileRepository]

  def adminAuthor = admin.getIdent
}