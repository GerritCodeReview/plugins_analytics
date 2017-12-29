package com.googlesource.gerrit.plugins.analytics.common

import com.googlesource.gerrit.plugins.analytics.common.AggregationStrategy.EMAIL_YEAR
import com.googlesource.gerrit.plugins.analytics.test.GitTestCase
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.scalatest.{FlatSpec, Matchers}

class UserActivityHistogramTest extends FlatSpec with Matchers with GitTestCase {

  "UserActivityHistogram" should "aggregate the correct number of activities - repo without commits" in {
    val repo = new FileRepository(testRepo)
    val filter = new AggregatedHistogramFilterByDates(aggregationStrategy = EMAIL_YEAR)
    new UserActivityHistogram().get(repo, filter) should have size 0
  }

  it should "aggregate the correct number of activities - repo with commits" in {
    val repo = new FileRepository(testRepo)
    add("test.txt", "content")
    val filter = new AggregatedHistogramFilterByDates(aggregationStrategy = EMAIL_YEAR)
    new UserActivityHistogram().get(repo, filter) should have size 1
  }

}
