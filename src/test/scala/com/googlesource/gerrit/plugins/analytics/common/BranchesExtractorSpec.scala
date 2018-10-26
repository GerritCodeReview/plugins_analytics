package com.googlesource.gerrit.plugins.analytics.common

import com.googlesource.gerrit.plugins.analytics.test.GitTestCase
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.scalatest.{FlatSpec, Matchers}

class BranchesExtractorSpec extends FlatSpec with Matchers with GitTestCase {
  def commitsBranches = new BranchesExtractor(new FileRepository(testRepo))

  behavior of "branchesOfCommit"

  it should "extract one branch for a commit existing only in one branch" in {
    add("file", "content")
    branch("feature/branch")
    val commit = add("fileOnBranch", "content2")

    commitsBranches.branchesOfCommit(commit.getId) shouldBe Set("feature/branch")

  }

  it should "extract two branches for a commit existing in two different branches" in {
    val commit = add("file", "content")
    branch("feature/branch")
    add("fileOnBranch", "content2")

    commitsBranches.branchesOfCommit(commit.getId) shouldBe Set("feature/branch", "master")

  }
}
