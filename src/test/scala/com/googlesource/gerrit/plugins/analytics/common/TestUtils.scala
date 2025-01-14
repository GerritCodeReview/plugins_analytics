package com.googlesource.gerrit.plugins.analytics.common

import org.eclipse.jgit.internal.storage.file.FileRepository
import org.gitective.core.CommitFinder

trait TestUtils {

  def aggregateBy(strategy: AggregationStrategy, branchName: Option[String] = None)(implicit testRepository: FileRepository): Array[AggregatedUserCommitActivity] = {
    val filter = new AggregatedHistogramFilterByDates(aggregationStrategy = strategy, branchName = branchName)
    new CommitFinder(testRepository).setFilter(filter).find
    filter.getHistogram.getAggregatedUserActivity
  }

}
