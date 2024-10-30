package com.googlesource.gerrit.plugins.analytics.common

import com.google.gerrit.acceptance.UseLocalDisk
import com.googlesource.gerrit.plugins.analytics.test.GerritTestDaemon
import org.eclipse.jgit.lib.Constants
import org.scalatest.{FlatSpec, Matchers}

import java.util.Date

@UseLocalDisk
class BranchesExtractorSpec extends FlatSpec with Matchers with GerritTestDaemon {

  val now = new Date()
  val nowPlus3seconds = Date.from(now.toInstant.plusSeconds(3))
  val nowPlus5seconds = Date.from(now.toInstant.plusSeconds(5))
  val nowPlus7seconds = Date.from(now.toInstant.plusSeconds(7))

  behavior of "branchesOfCommit"

  it should "extract master branch when the commit is after 'from' date and 'to' date is not defined" in {
    val commit1 = testFileRepository.commitFile("file1", "content1", committer = newPersonIdent("Test Committer", "committer@test.com", nowPlus5seconds))
    val branchExtractor = BranchesExtractor(
      testFileRepository.getRepository,
      FilterByDates(from = Some(now.getTime))
    )
    branchExtractor.branchesOfCommit(commit1) shouldBe Set(Constants.MASTER)
  }

  it should "extract master branch when the commit is before 'to' date and 'from' date is not defined" in {
    val commit1 = testFileRepository.commitFile("file1", "content1", committer = newPersonIdent("Test Committer", "committer@test.com", now))
    val branchExtractor = BranchesExtractor(
      testFileRepository.getRepository,
      FilterByDates(to = Some(nowPlus5seconds.getTime))
    )
    branchExtractor.branchesOfCommit(commit1) shouldBe Set(Constants.MASTER)
  }

  it should "extract master branch when the commit is after 'from' date and before 'to' date" in {
    val commit1 = testFileRepository.commitFile("file1", "content1", committer = newPersonIdent("Test Committer", "committer@test.com", nowPlus3seconds))
    val branchExtractor = BranchesExtractor(
      testFileRepository.getRepository,
      FilterByDates(from = Some(now.getTime), to = Some(nowPlus5seconds.getTime))
    )
    branchExtractor.branchesOfCommit(commit1) shouldBe Set(Constants.MASTER)
  }

  it should "extract master branch when neither 'from' date nor 'to' date are defined" in {
    val commit1 = testFileRepository.commitFile("file1", "content1", committer = newPersonIdent("Test Committer", "committer@test.com", nowPlus3seconds))
    val branchExtractor = BranchesExtractor(
      testFileRepository.getRepository,
      FilterByDates.filterAllRevs
    )
    branchExtractor.branchesOfCommit(commit1) shouldBe Set(Constants.MASTER)
  }

  it should "extract two branches for a commit existing in two different branches" in {
    val commit = testFileRepository.commitFile("file", "content", committer = newPersonIdent("Test Committer", "committer@test.com", nowPlus3seconds))
    testFileRepository.branch("feature/branch", "master")
    testFileRepository.commitFile("fileOnBranch", "content2", branch = "feature/branch", committer = newPersonIdent("Test Committer", "committer@test.com", nowPlus5seconds))
    val branchExtractor = BranchesExtractor(
      testFileRepository.getRepository,
      FilterByDates(from = Some(now.getTime), to = Some(nowPlus7seconds.getTime))
    )

    branchExtractor.branchesOfCommit(commit) shouldBe Set("feature/branch", "master")
  }

  it should "not extract any branch when the commit is after 'to' date" in {
    val commit1 = testFileRepository.commitFile("file1", "content1", committer = newPersonIdent("Test Committer", "committer@test.com", nowPlus5seconds))
    val branchExtractor = BranchesExtractor(
      testFileRepository.getRepository,
      FilterByDates(to = Some(now.getTime))
    )
    an [NoSuchElementException] should be thrownBy branchExtractor.branchesOfCommit(commit1)
  }

  it should "not extract any branch when the commit is before 'from' date" in {
    val commit1 = testFileRepository.commitFile("file1", "content1", committer = newPersonIdent("Test Committer", "committer@test.com", now))
    val branchExtractor = BranchesExtractor(
      testFileRepository.getRepository,
      FilterByDates(from = Some(nowPlus5seconds.getTime))
    )
    an [NoSuchElementException] should be thrownBy branchExtractor.branchesOfCommit(commit1)
  }
}
