package com.googlesource.gerrit.plugins.analytics.common

import com.googlesource.gerrit.plugins.analytics.test.GitTestCase
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.scalatest.{FlatSpec, Matchers}

class CommitsBranchesTest extends FlatSpec with Matchers with GitTestCase {
  def commitsBranches = new CommitsBranches(new FileRepository(testRepo))
  "getBranches" should "return all branches" in {
    val c1 = add("file", "content")
    branch("branch")
    val c2 = add("fileOnBranch", "content2")
    commitsBranches.getBranches should contain allOf(
      "refs/heads/master", "refs/heads/branch"
    )
  }
  "getAllCommitsLabeledWithBranches" should "label correctly a set of " +
    "commits" in {
    val c1 = add("file", "content")
    val c2 = add("file2", "content")
    val c3 = add("file3", "content")
    val c4 = add("file4", "content")
    branch("branch")
    val c5 = add("fileOnBranch", "content2")
    val c6 = add("fileOnBranch2", "content2")
    val c7 = add("fileOnBranch3", "content2")
    val c8 = add("fileOnBranch4", "content2")

    val labeled = commitsBranches.getAllCommitsLabeledWithBranches
    labeled should have size 8
    Seq(c1, c2, c3, c4).foreach(c => labeled(c) should contain allOf
      ("master", "branch"))

    Seq(c5, c6, c7, c8).foreach(c => labeled(c) should be(Set
    ("branch")))
  }
  "commitsBranches" should "enrich commits with their branches" in {
    val c1 = add("file", "content")
    val c2 = add("file2", "content")
    val c3 = add("file3", "content")
    val c4 = add("file4", "content")
    branch("branch")
    val c5 = add("fileOnBranch", "content2")
    val c6 = add("fileOnBranch2", "content2")
    val c7 = add("fileOnBranch3", "content2")
    val c8 = add("fileOnBranch4", "content2")

    commitsBranches.find(Seq(c1,c6)) should be(Set("master","branch"))

  }

}
