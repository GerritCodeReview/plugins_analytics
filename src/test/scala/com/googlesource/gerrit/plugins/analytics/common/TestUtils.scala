package com.googlesource.gerrit.plugins.analytics.common

import com.googlesource.gerrit.plugins.analytics.test.GitTestCase
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.gitective.core.CommitFinder

trait TestUtils {
  self: GitTestCase =>

  def aggregateBy(strategy: AggregationStrategy): Array[AggregatedUserCommitActivity] = {
    val repo = new FileRepository(testRepo)
    val filter = new AggregatedHistogramFilterByDates(repo, aggregationStrategy = strategy)
    new CommitFinder(testRepo).setFilter(filter).find
    filter.getHistogram.getAggregatedUserActivity
  }

}
