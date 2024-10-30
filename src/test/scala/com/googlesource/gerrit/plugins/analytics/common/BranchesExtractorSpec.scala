package com.googlesource.gerrit.plugins.analytics.common

import com.google.gerrit.acceptance.UseLocalDisk
import com.googlesource.gerrit.plugins.analytics.test.GerritTestDaemon
import org.eclipse.jgit.lib.Constants
import org.scalatest.{FlatSpec, Matchers}

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

@UseLocalDisk
class BranchesExtractorSpec extends FlatSpec with Matchers with GerritTestDaemon {

  val now = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS))
  val nowPlus1seconds = Date.from(now.toInstant.plusSeconds(1))
  val nowPlus2seconds = Date.from(now.toInstant.plusSeconds(2))
  val nowPlus3seconds = Date.from(now.toInstant.plusSeconds(3))

  behavior of "branchesOfCommit"

  it should "provide a commit in 'master' branch when the commit is same as 'from' date and 'to' date is not defined" in {
    val commit = testFileRepository.commitFile("file", "content", committer = newPersonIdent(ts = now))
    val branchExtractor = BranchesExtractor(
      testFileRepository.getRepository,
    FilterByDates(from = Some(now.getTime))
    )
    branchExtractor.branchesOfCommit should contain only (commit -> Set(Constants.MASTER))
  }

  it should "provide a commit in 'master' branch when the commit is before 'to' date and 'from' date is not defined" in {
    val commit = testFileRepository.commitFile("file", "content", committer = newPersonIdent(ts = now))
    val branchExtractor = BranchesExtractor(
      testFileRepository.getRepository,
      FilterByDates(to = Some(nowPlus1seconds.getTime))
    )
    branchExtractor.branchesOfCommit should contain only (commit -> Set(Constants.MASTER))
  }

  it should "provide a commit in 'master' branch when the commit is after 'from' date and before 'to' date" in {
    val commit = testFileRepository.commitFile("file", "content", committer = newPersonIdent(ts = nowPlus1seconds))
    val branchExtractor = BranchesExtractor(
      testFileRepository.getRepository,
      FilterByDates(from = Some(now.getTime), to = Some(nowPlus2seconds.getTime))
    )
    branchExtractor.branchesOfCommit should contain only (commit -> Set(Constants.MASTER))
  }

  it should "provide a commit in 'master' branch when neither 'from' date nor 'to' date are defined" in {
    val commit = testFileRepository.commitFile("file", "content", committer = newPersonIdent(ts = now))
    val branchExtractor = BranchesExtractor(testFileRepository.getRepository)
    branchExtractor.branchesOfCommit should contain only (commit -> Set(Constants.MASTER))
  }

  it should "provide commit1 in 'master' and 'feature/branch' and commit2 in 'feature/branch' branch" in {
    val commit1 = testFileRepository.commitFile("file", "content", committer = newPersonIdent(ts = nowPlus1seconds))
    testFileRepository.branch("feature/branch", "master")
    val commit2 = testFileRepository.commitFile("fileOnBranch", "content2", branch = "feature/branch", committer = newPersonIdent(ts = nowPlus2seconds))
    val branchExtractor = BranchesExtractor(
      testFileRepository.getRepository,
      FilterByDates(from = Some(now.getTime), to = Some(nowPlus3seconds.getTime))
    )
    branchExtractor.branchesOfCommit shouldBe Map(commit1 -> Set(Constants.MASTER, "feature/branch"), commit2 -> Set("feature/branch"))
  }

  it should "be empty when the commit is the same as 'to' date" in {
    testFileRepository.commitFile("file", "content", committer = newPersonIdent(ts = now))
    val branchExtractor = BranchesExtractor(
      testFileRepository.getRepository,
      FilterByDates(to = Some(now.getTime))
    )
    branchExtractor.branchesOfCommit shouldBe empty
  }

  it should "be empty when the commit is before 'from' date" in {
    testFileRepository.commitFile("file", "content", committer = newPersonIdent(ts = now))
    val branchExtractor = BranchesExtractor(
      testFileRepository.getRepository,
      FilterByDates(from = Some(nowPlus1seconds.getTime))
    )
    branchExtractor.branchesOfCommit shouldBe empty
  }
}
