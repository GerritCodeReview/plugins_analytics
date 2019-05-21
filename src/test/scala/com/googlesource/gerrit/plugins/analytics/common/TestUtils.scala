package com.googlesource.gerrit.plugins.analytics.common

import com.googlesource.gerrit.plugins.analytics.test.GitTestCase
import org.gitective.core.CommitFinder

trait TestUtils {
  self: GitTestCase =>

  def aggregateBy(strategy: AggregationStrategy): Array[AggregatedUserCommitActivity] = {
    val filter = new AggregatedHistogramFilterByDates(aggregationStrategy = strategy)
    new CommitFinder(testRepo.getRepository).setFilter(filter).find
    filter.getHistogram.getAggregatedUserActivity
  }

}
