package com.googlesource.gerrit.plugins.analytics.common

import com.googlesource.gerrit.plugins.analytics.test.GitTestCase
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.scalatest.{FlatSpec, Matchers}

class CommitsBranchesTest extends FlatSpec with Matchers with GitTestCase {
  def commitsBranches = new CommitsBranches(new FileRepository(testRepo))

  "getAllCommitsLabeledWithBranches" should "label correctly a set of " +
    "commits" in {
    val c1 = add("file", "content")
    val c2 = add("file2", "content")
    val c3 = add("file3", "content")
    val c4 = add("file4", "content")
    branch("feature/branch")
    val c5 = add("fileOnBranch", "content2")
    val c6 = add("fileOnBranch2", "content2")
    val c7 = add("fileOnBranch3", "content2")
    val c8 = add("fileOnBranch4", "content2")

    commitsBranches.forCommits(Seq(c1, c2, c3, c4)) should be(
      Set("master", "feature/branch"))

    commitsBranches.forCommits(Seq(c7, c8)) should be(Set("feature/branch"))
  }
}
