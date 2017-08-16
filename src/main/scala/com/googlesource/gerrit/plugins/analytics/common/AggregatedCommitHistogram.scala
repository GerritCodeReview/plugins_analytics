package com.googlesource.gerrit.plugins.analytics.common

import java.util.Date

import com.googlesource.gerrit.plugins.analytics.common.AggregatedCommitHistogram.AggregationStrategyMapping
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import org.gitective.core.stat.{CommitHistogram, CommitHistogramFilter, UserCommitActivity}

// A more generic version of CommitHistogram having a generic function
// mapping (PersonIdent, Date) => String
class AggregatedCommitHistogram(val aggregationStrategyForUser: AggregationStrategyMapping)
  extends CommitHistogram {
  override def include(commit: RevCommit, user: PersonIdent): AggregatedCommitHistogram = {
    val key = aggregationStrategyForUser(user,commit.getAuthorIdent.getWhen)
    val activity = Option(users.get(key)) match {
      case None =>
        val newActivity = new UserCommitActivity(
          user.getName, user.getEmailAddress)
        users.put(key, newActivity)
        newActivity
      case Some(foundActivity) => foundActivity
    }
    activity.include(commit, user)
    this
  }
}

object AggregatedCommitHistogram {
  type AggregationStrategyMapping = (PersonIdent, Date) => String

  def apply(aggregationStrategy: AggregationStrategyMapping) {
    new AggregatedCommitHistogram(aggregationStrategy)
  }
}

abstract class GenericCommitHistogramFilter(aggregationStrategyMapping: AggregationStrategyMapping) extends CommitHistogramFilter {
  val genericHistogram = new AggregatedCommitHistogram(aggregationStrategyMapping)

  override def getHistogram = genericHistogram
}
