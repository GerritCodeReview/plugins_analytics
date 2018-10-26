package com.googlesource.gerrit.plugins.analytics.common

import com.googlesource.gerrit.plugins.analytics.test.GitTestCase
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.scalatest.{FlatSpec, Matchers}

class BranchesExtractorSpec extends FlatSpec with Matchers with GitTestCase {
  def commitsBranches = new BranchesExtractor(new FileRepository(testRepo))

  behavior of "branchesOfCommit"

  it should "find correct branch across two different branches" in {
    val c1 = add("file", "content")
    branch("feature/branch")
    val c2 = add("fileOnBranch", "content2")

    commitsBranches.branchesOfCommit(c1.getId) shouldBe Set("feature/branch", "master")
    commitsBranches.branchesOfCommit(c2.getId) shouldBe Set("feature/branch")
  }
}
