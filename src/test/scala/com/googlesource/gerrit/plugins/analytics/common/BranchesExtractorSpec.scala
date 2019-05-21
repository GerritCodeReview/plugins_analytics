package com.googlesource.gerrit.plugins.analytics.common

import com.google.gerrit.acceptance.UseLocalDisk
import com.googlesource.gerrit.plugins.analytics.test.GitTestCase
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.junit.TestRepository
import org.scalatest.{FlatSpec, Matchers}

@UseLocalDisk
class BranchesExtractorSpec extends FlatSpec with Matchers with GitTestCase {
  def commitsBranches = new BranchesExtractor(testRepo.getRepository)

  behavior of "branchesOfCommit"

  it should "extract one branch for a commit existing only in one branch" in {
    val clonedRepo = clone(testRepo.asInstanceOf[TestRepository[FileRepository]])
    add("file", "content", repo = clonedRepo)
    branch(clonedRepo, "feature/branch")
    val commit = add("fileOnBranch", "content2", repo = clonedRepo)
    clonedRepo.git.push().call()

    commitsBranches.branchesOfCommit(commit) shouldBe Set("feature/branch")

  }

  it should "extract two branches for a commit existing in two different branches" in {
    val commit = add("file", "content")
    branch("feature/branch")
    add("fileOnBranch", "content2")

    commitsBranches.branchesOfCommit(commit) shouldBe Set("feature/branch", "master")

  }
}
