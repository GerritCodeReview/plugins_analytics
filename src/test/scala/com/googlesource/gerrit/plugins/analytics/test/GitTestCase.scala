/*
 * Copyright (c) 2011 Kevin Sawicki <kevinsawicki@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.googlesource.gerrit.plugins.analytics.test

import java.io.File
import java.io.PrintWriter
import java.text.MessageFormat
import java.util.Date
import java.util

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.notes.Note
import org.eclipse.jgit.revwalk.RevCommit
import org.gitective.core.CommitUtils
import org.scalatest.{BeforeAndAfterEach, Suite}


/**
  * Base test case with utilities for common Git operations performed during
  * testing
  */
trait GitTestCase extends BeforeAndAfterEach {
  self: Suite =>
  /**
    * Test repository .git directory
    */
  protected var testRepo: File = null

  /**
    * Author used for commits
    */
  protected val author = new PersonIdent("Test Author", "author@test.com")

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
    testRepo = initRepo
  }

  /**
    * Initialize a new repo in a new directory
    *
    * @return created .git folder
    * @throws GitAPIException
    */
  protected def initRepo: File = {
    val tmpDir = System.getProperty("java.io.tmpdir")
    assert(tmpDir != null, "java.io.tmpdir was null")
    val dir = new File(tmpDir, "git-test-case-" + System.nanoTime)
    assert(dir.mkdir)
    Git.init.setDirectory(dir).setBare(false).call
    val repo = new File(dir, Constants.DOT_GIT)
    assert(repo.exists)
    repo.deleteOnExit()
    repo
  }

  /**
    * Create branch with name and checkout
    *
    * @param name
    * @return branch ref
    *
    */
  protected def branch(name: String): Ref = branch(testRepo, name)

  /**
    * Create branch with name and checkout
    *
    * @param repo
    * @param name
    * @return branch ref
    *
    */
  protected def branch(repo: File, name: String): Ref = {
    val git = Git.open(repo)
    git.branchCreate.setName(name).call
    checkout(repo, name)
  }

  /**
    * Checkout branch
    *
    * @param name
    * @return branch ref
    *
    */
  protected def checkout(name: String): Ref = checkout(testRepo, name)

  /**
    * Checkout branch
    *
    * @param repo
    * @param name
    * @return branch ref
    *
    */
  @throws[Exception]
  protected def checkout(repo: File, name: String): Ref = {
    val git = Git.open(repo)
    val ref = git.checkout.setName(name).call
    assert(ref != null)
    ref
  }

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
  protected def tag(repo: File, name: String): Ref = {
    val git = Git.open(repo)
    git.tag.setName(name).setMessage(name).call
    val tagRef = git.getRepository.getTags.get(name)
    assert(tagRef != null)
    tagRef
  }

  /**
    * Add file to test repository
    *
    * @param path
    * @param content
    * @return commit
    *
    */
  protected def add(path: String, content: String, author: PersonIdent = author, committer: PersonIdent = committer): RevCommit = add(testRepo, path, content, author, committer)

  /**
    * Add file to test repository
    *
    * @param repo
    * @param path
    * @param content
    * @return commit
    *
    */
  protected def add(repo: File, path: String, content: String, author: PersonIdent, committer: PersonIdent): RevCommit = {
    val message = MessageFormat.format("Committing {0} at {1}", path, new Date)
    add(repo, path, content, message, author, committer)
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
  protected def add(repo: File, path: String, content: String, message: String, author: PersonIdent, committer: PersonIdent): RevCommit = {
    val file = new File(repo.getParentFile, path)
    if (!file.getParentFile.exists) assert(file.getParentFile.mkdirs)
    if (!file.exists) assert(file.createNewFile)
    val writer = new PrintWriter(file)
    try
      writer.print(Option(content).fold("")(identity))
    finally writer.close()
    val git = Git.open(repo)
    git.add.addFilepattern(path).call
    val commit = git.commit.setOnly(path).setMessage(message).setAuthor(author).setCommitter(committer).call
    assert(null != commit)
    commit
  }

  /**
    * Move file in test repository
    *
    * @param from
    * @param to
    * @return commit
    *
    */
  protected def mv(from: String, to: String): RevCommit = mv(testRepo, from, to, MessageFormat.format("Moving {0} to {1} at {2}", from, to, new Date))

  /**
    * Move file in test repository
    *
    * @param from
    * @param to
    * @param message
    * @return commit
    *
    */
  protected def mv(from: String, to: String, message: String): RevCommit = mv(testRepo, from, to, message)

  /**
    * Move file in test repository
    *
    * @param repo
    * @param from
    * @param to
    * @param message
    * @return commit
    *
    */
  protected def mv(repo: File, from: String, to: String, message: String): RevCommit = {
    val file = new File(repo.getParentFile, from)
    file.renameTo(new File(repo.getParentFile, to))
    val git = Git.open(repo)
    git.rm.addFilepattern(from)
    git.add.addFilepattern(to).call
    val commit = git.commit.setAll(true).setMessage(message).setAuthor(author).setCommitter(committer).call
    assert(null != commit)
    commit
  }

  /**
    * Add files to test repository
    *
    * @param paths
    * @param contents
    * @return commit
    *
    */
  protected def add(paths: util.List[String], contents: util.List[String]): RevCommit = add(testRepo, paths, contents, "Committing multiple files")

  /**
    * Add files to test repository
    *
    * @param repo
    * @param paths
    * @param contents
    * @param message
    * @return commit
    *
    */
  protected def add(repo: File, paths: util.List[String], contents: util.List[String], message: String): RevCommit = {
    val git = Git.open(repo)
    var i = 0
    while ( {
      i < paths.size
    }) {
      val path = paths.get(i)
      var content = contents.get(i)
      val file = new File(repo.getParentFile, path)
      if (!file.getParentFile.exists) assert(file.getParentFile.mkdirs)
      if (!file.exists) assert(file.createNewFile)
      val writer = new PrintWriter(file)
      if (content == null) content = ""
      try
        writer.print(content)
      finally writer.close()
      git.add.addFilepattern(path).call

      {
        i += 1;
        i - 1
      }
    }
    val commit = git.commit.setMessage(message).setAuthor(author).setCommitter(committer).call
    assert(null != commit)
    commit
  }

  /**
    * Merge ref into current branch
    *
    * @param ref
    * @return result
    *
    */
  protected def merge(ref: String): MergeResult = {
    val git = Git.open(testRepo)
    git.merge.setStrategy(MergeStrategy.RESOLVE).include(CommitUtils.getCommit(git.getRepository, ref)).call
  }

  /**
    * Add note to latest commit with given content
    *
    * @param content
    * @return note
    *
    */
  protected def note(content: String): Note = note(content, "commits")

  /**
    * Add note to latest commit with given content
    *
    * @param content
    * @param ref
    * @return note
    *
    */
  protected def note(content: String, ref: String): Note = {
    val git = Git.open(testRepo)
    val note = git.notesAdd.setMessage(content).setNotesRef(Constants.R_NOTES + ref).setObjectId(CommitUtils.getHead(git.getRepository)).call
    assert(null != note)
    note
  }

  /**
    * Delete and commit file at path
    *
    * @param path
    * @return commit
    *
    */
  protected def delete(path: String): RevCommit = {
    val message = MessageFormat.format("Committing {0} at {1}", path, new Date)
    val git = Git.open(testRepo)
    git.rm.addFilepattern(path).call
    val commit = git.commit.setOnly(path).setMessage(message).setAuthor(author).setCommitter(committer).call
    assert(null != commit)
    commit
  }
}