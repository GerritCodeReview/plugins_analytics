package com.googlesource.gerrit.plugins.analytics.common

import com.google.gerrit.acceptance.UseLocalDisk
import com.googlesource.gerrit.plugins.analytics.test.GerritTestDaemon
import org.scalatest.{FlatSpec, Matchers}

@UseLocalDisk
class BranchesExtractorSpec extends FlatSpec with Matchers with GerritTestDaemon {
  def commitsBranches = new BranchesExtractor(testFileRepository.getRepository, None)

  behavior of "branchesOfCommit"

  it should "extract one branch for a commit existing only in one branch" in {
    testFileRepository.commitFile("file", "content")
    testFileRepository.branch("feature/branch", "master")
    val commit = testFileRepository.commitFile("fileOnBranch", "content2", branch = "feature/branch")

    commitsBranches.branchesOfCommit(commit) shouldBe Set("feature/branch")
  }

  it should "extract two branches for a commit existing in two different branches" in {
    val commit = testFileRepository.commitFile("file", "content")
    testFileRepository.branch("feature/branch", "master")
    testFileRepository.commitFile("fileOnBranch", "content2", branch = "feature/branch")

    commitsBranches.branchesOfCommit(commit) shouldBe Set("feature/branch", "master")
  }
}
