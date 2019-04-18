package com.googlesource.gerrit.plugins.analytics.test

import com.googlesource.gerrit.plugins.analytics.UserActivitySummary
import com.googlesource.gerrit.plugins.analytics.common.AggregationStrategy.EMAIL
import com.googlesource.gerrit.plugins.analytics.common.{Statistics, TestUtils}
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.scalatest.{FlatSpec, Matchers}

class UserActivitySummarySpec extends FlatSpec with GitTestCase with TestUtils with Matchers {

  "numCommits" should "count only comments filtered by their merge status" in {
    val personEmail = "aCommitter@aCompany.com"

    // we want merge and non-merge commits to be authored by same person, so that they can be aggregated together
    val repo = getRepoOwnedByPerson(personEmail)

    commit(personEmail, fileName="aFile.txt", content="some content")
    mergeCommit(personEmail, fileName = "anotherFile.txt", content="some other content")
    val aggregatedCommits = aggregateBy(EMAIL)

    val List(nonMergeSummary, mergeSummary) = UserActivitySummary.apply(new Statistics(repo, TestBotLikeExtractor)(CommitsStatisticsNoCache))(aggregatedCommits.head)

    nonMergeSummary.numCommits should be(2)
    mergeSummary.numCommits should be(1)
  }

  def getRepoOwnedByPerson(email: String = author.getEmailAddress): FileRepository = {
    val repo = new FileRepository(testRepo)

    val conf = repo.getConfig
    conf.load()
    conf.setString("user", null, "name", email)
    conf.setString("user", null, "email", email)
    conf.save()

    repo
  }

}
