package com.googlesource.gerrit.plugins.analytics.test

import com.google.gerrit.acceptance.{GitUtil, UseLocalDisk}
import com.google.gerrit.entities.Project
import com.googlesource.gerrit.plugins.analytics.UserActivitySummary
import com.googlesource.gerrit.plugins.analytics.common.AggregationStrategy.EMAIL
import com.googlesource.gerrit.plugins.analytics.common.{Statistics, TestUtils}
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.Repository
import org.scalatest.{FlatSpec, Matchers}
import org.eclipse.jgit.lib.Constants.HEAD

@UseLocalDisk
class UserActivitySummarySpec extends FlatSpec with GerritTestDaemon with TestCommitStatisticsNoCache with TestUtils with Matchers {

  "numCommits" should "count only comments filtered by their merge status" in {
    val personEmail = "aCommitter@aCompany.com"

    // we want merge and non-merge commits to be authored by same person, so that they can be aggregated together
    val newProjectKey: Project.NameKey = daemonTest.newProject(testSpecificRepositoryName, true)
    val repository = daemonTest.getRepository(newProjectKey)
    val cloneRepo = GitUtil.newTestRepository(repository).gitClone
    getRepoOwnedByPerson(personEmail, repo = cloneRepo)

    val personIdent = newPersonIdent("aPerson", personEmail)
    cloneRepo.commitFile("aFile.txt", "some content", committer = personIdent, author = personIdent)
    cloneRepo.mergeCommitFile("anotherFile.txt", "some other content", author = personIdent, committer = personIdent)
    cloneRepo.push

    val aggregatedCommits = aggregateBy(EMAIL)(repository)
    val summary = UserActivitySummary.apply(new Statistics(newProjectKey, commitsStatisticsNoCache), HEAD)(aggregatedCommits.head)

    val nonMergeSummary = summary.head
    val mergeSummary = summary.drop(1).head

    nonMergeSummary.numCommits should be(2)
    mergeSummary.numCommits should be(1)
  }

  "UserActivitySummary" should "correctly set starting ref" in {
    val (newProjectKey: Project.NameKey, repository: FileRepository) = createRepositoryWithCommit

    val aggregatedCommits = aggregateBy(EMAIL)(repository)
    val summaryHead = UserActivitySummary.apply(new Statistics(newProjectKey, commitsStatisticsNoCache), HEAD)(aggregatedCommits.head)
    summaryHead.head.startingRevision shouldBe(HEAD)

    val nonHead = "nonHeadRef"
    val summaryNonHead = UserActivitySummary.apply(new Statistics(newProjectKey, commitsStatisticsNoCache), nonHead)(aggregatedCommits.head)
    summaryNonHead.head.startingRevision shouldBe(nonHead)
  }

  private def createRepositoryWithCommit = {
    val personEmail = "aCommitter@aCompany.com"

    val newProjectKey: Project.NameKey = daemonTest.newProject(testSpecificRepositoryName, true)
    val repository = daemonTest.getRepository(newProjectKey)
    val cloneRepo = GitUtil.newTestRepository(repository).gitClone
    getRepoOwnedByPerson(personEmail, repo = cloneRepo)

    val personIdent = newPersonIdent("aPerson", personEmail)
    cloneRepo.commitFile("aFile.txt", "some content", committer = personIdent, author = personIdent)
    cloneRepo.push
    (newProjectKey, repository)
  }

  def getRepoOwnedByPerson(email: String = author.getEmailAddress, repo: TestFileRepository = testFileRepository): Repository = {
    val conf = repo.getRepository.getConfig
    conf.load()
    conf.setString("user", null, "name", email)
    conf.setString("user", null, "email", email)
    conf.save()

    repo.getRepository
  }

}
