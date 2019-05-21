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
import java.util.Date

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

  type GitRepository = TestRepository[_ <: Repository]

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

  /**
    * Test repository .git directory
    */
  protected var testRepo: GitRepository = null

  /**
    * Test project name.
    */
  protected var testRepoName: Project.NameKey = null

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

  /**
    * Set up method that initializes git repository
    *
    *
    */

  override def beforeEach = {
    testRepoName = createProject
    testRepo = GitUtil.newTestRepository(daemonTest.getRepository(testRepoName))
  }

  /**
    * Initialize a new repo in a new directory
    *
    * @return created .git folder
    * @throws GitAPIException
    */
  protected def createProject = daemonTest.newProject(threadSpecificDirectoryPrefix)

  private def threadSpecificDirectoryPrefix =
    "git-test-case-" + Thread.currentThread().getName.replaceAll("~[\\.0-9a-zA-Z]", "_") + System.nanoTime

  /**
    * Create branch with name and checkout
    *
    * @param name
    * @return branch ref
    *
    */
  protected def branch(name: String): Ref = branch(testRepo, name)

  /**
    * Delete branch with name
    *
    * @param name
    * @return branch ref
    *
    */
  protected def deleteBranch(name: String): String = deleteBranch(testRepo, name)

  /**
    * Create branch with name and checkout
    *
    * @param repo
    * @param name
    * @return branch ref
    *
    */
  protected def branch(repo: GitRepository, name: String): Ref = {
    repo.git.branchCreate.setName(name).call
    checkout(name, repo)
  }

  /**
    * Delete branch with name
    *
    * @param repo
    * @param name
    * @return branch ref
    *
    */
  protected def deleteBranch(repo: GitRepository, name: String): String = {
    repo.git.branchDelete().setBranchNames(name).setForce(true).call.get(0)
  }

  protected def clone(repo: TestRepository[FileRepository]): GitRepository = {
    val git = Git.cloneRepository
      .setDirectory(new File(threadSpecificDirectoryPrefix + "_clone"))
      .setURI(repo.getRepository.getDirectory.toURI.toString)
      .call()

    GitUtil.newTestRepository(git.getRepository)
  }

  /**
    * Checkout branch
    *
    * @param repo
    * @param name
    * @return branch ref
    *
    */
  @throws[Exception]
  protected def checkout(name: String, repo: GitRepository): Ref = {
    repo.git.checkout.setName(name).call
  } ensuring(_ != null, "Unable to checkout result")

  /**
    * Create tag with name
    *
    * @param name
    * @return tag ref
    *
    */
  protected def tag(name: String): Ref = tag(testRepo, name)

  /**
    * Create tag with name
    *
    * @param repo
    * @param name
    * @return tag ref
    *
    */
  protected def tag(repo: GitRepository, name: String): Ref = {
    repo.git.tag.setName(name).setMessage(name).call
    repo.getRepository.getTags.get(name)
  } ensuring(_ != null, s"Unable to tag file $name")

  /**
    * Add file to test repository
    *
    * @param path
    * @param content
    * @return commit
    *
    */
  protected def add(path: String, content: String, author: PersonIdent = author, committer: PersonIdent = committer, branch: String = "refs/heads/master", repo: GitRepository = testRepo): RevCommit =
    add(repo, path, content, author, committer, branch)

  /**
    * Add file to test repository
    *
    * @param repo
    * @param path
    * @param content
    * @return commit
    *
    */
  protected def add(repo: GitRepository, path: String, content: String, author: PersonIdent, committer: PersonIdent, branch: String): RevCommit = {
    val message = MessageFormat.format("Committing {0} at {1}", path, new Date)
    add(repo, path, content, message, author, committer, branch)
  }


  /**
    * Add file to test repository
    *
    * @param repo
    * @param path
    * @param content
    * @param message
    * @return commit
    *
    */
  protected def add(repo: GitRepository, path: String, content: String, message: String, author: PersonIdent, committer: PersonIdent, branch: String): RevCommit = {
    repo.branch(branch).commit()
      .add(path, content)
      .author(author)
      .committer(committer)
      .create
  } ensuring(_ != null, s"Unable to commit addition of path $path")

  /**
    * Add files to test repository
    *
    * @param contents iterable of file names and associated content
    * @return commit
    *
    */
  protected def add(contents: Iterable[(String, String)], branch: String): RevCommit =
    add(testRepo, contents, "Committing multiple files", branch)

  /**
    * Add files to test repository
    *
    * @param repo
    * @param contents iterable of file names and associated content
    * @param message
    * @return commit
    *
    */
  protected def add(repo: GitRepository, contents: Iterable[(String, String)], message: String, branch: String): RevCommit = {
    val commitBuilder = repo.branch(branch).commit()
    contents.foreach { case (path, content) =>
      commitBuilder.add(path, content)
    }
    commitBuilder.author(author).committer(committer).create()
  } ensuring(_ != null, "Unable to commit content addition")

  /**
    * Merge given branch into current branch
    *
    * @param branch
    * @return result
    *
    */
  protected def mergeBranch(branch: String, withCommit: Boolean, repo: GitRepository = testRepo): MergeResult = {
    repo.git.merge.setStrategy(MergeStrategy.RESOLVE)
      .include(CommitUtils.getRef(repo.getRepository, branch))
      .setCommit(withCommit)
      .setFastForward(FastForwardMode.NO_FF)
      .setMessage(s"merging branch $branch").call
  }

  /**
    * Merge ref into current branch
    *
    * @param ref
    * @return result
    *
    */
  protected def merge(ref: String): MergeResult = {
    testRepo.git.merge.setStrategy(MergeStrategy.RESOLVE).include(CommitUtils.getCommit(testRepo.git.getRepository, ref)).call
  }

  /**
    * commit specified content into a file, as committer
    *
    * @param committer - the author of this commit
    * @param fileName  - the name of the file
    * @param content   - the content of the file
    * @param when      - the date of the commit
    * @return RevCommit
    *
    */
  protected def commit(committer: String, fileName: String, content: String, when: Date = new Date(), message: Option[String] = None, repo: GitRepository = testRepo, branch: String = "refs/heads/master"): RevCommit = {
    val person = newPersonIdent(committer, committer, when)
    add(repo, fileName, content, author = person, committer = author, message = message.getOrElse("** no message **"), branch = branch)
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
  protected def mergeCommit(committer: String, fileName: String, content: String): MergeResult = {
    val currentBranch = testRepo.getRepository.getBranch
    val clonedRepo = clone(testRepo.asInstanceOf[TestRepository[FileRepository]])
    val tmpBranch = branch(clonedRepo, "tmp")
    try {
      commit(committer, fileName, content, repo = clonedRepo, branch = tmpBranch.getName)
      checkout(currentBranch, repo = clonedRepo)
      mergeBranch(tmpBranch.getName, withCommit = true, repo = clonedRepo)
    } finally {
      deleteBranch(clonedRepo, tmpBranch.getName)
      clonedRepo.git.push().call()
    }
  }
}

@UseLocalDisk
object GerritDaemonTest extends AbstractDaemonTest {
  baseConfig = new Config()

  def newProject(nameSuffix: String) = super.createProject(nameSuffix, allProjects, false)

  def getRepository(projectName: Project.NameKey) = repoManager.openRepository(projectName)

  def adminAuthor = admin.getIdent
}