package com.googlesource.gerrit.plugins.analytics.common

import com.google.gerrit.acceptance.UseLocalDisk
import com.googlesource.gerrit.plugins.analytics.test.GerritDaemonTest
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.junit.TestRepository
import org.scalatest.{FlatSpec, Matchers}

@UseLocalDisk
class BranchesExtractorSpec extends FlatSpec with Matchers with GerritDaemonTest {
  def commitsBranches = new BranchesExtractor(testRepo.getRepository)

  behavior of "branchesOfCommit"

  it should "extract one branch for a commit existing only in one branch" in {
    val clonedRepo = testRepo.gitClone
    clonedRepo.commitFile("file", "content")
    clonedRepo.branch("feature/branch", "master")
    val commit = clonedRepo.commitFile("fileOnBranch", "content2", branch = "feature/branch")
    clonedRepo.push

    commitsBranches.branchesOfCommit(commit) shouldBe Set("feature/branch")
  }

  it should "extract two branches for a commit existing in two different branches" in {
    val clonedRepo = testRepo.gitClone
    val commit = clonedRepo.commitFile("file", "content")
    clonedRepo.branch("feature/branch", "master")
    clonedRepo.commitFile("fileOnBranch", "content2", branch = "feature/branch")
    clonedRepo.push

    commitsBranches.branchesOfCommit(commit) shouldBe Set("feature/branch", "master")

  }
}
