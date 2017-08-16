package com.googlesource.gerrit.plugins.analytics.common

import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import org.gitective.core.filter.commit.CommitFilter
import org.gitective.core.stat.{CommitHistogram, CommitHistogramFilter, UserCommitActivity}

// A more generic version of CommitHistogram having a generic function
// mapping PersonIdent => String where to separate commits
class GenericCommitHistogram(get_key: PersonIdent => String = _.getEmailAddress)
  extends CommitHistogram {

  override def include(commit: RevCommit, user: PersonIdent): GenericCommitHistogram = {

    val key = get_key(user)
    val activity = Option(users.get(key)) match {
      case None =>
        val newActivity = new UserCommitActivity(
          user.getName, user.getEmailAddress)
        users.put(key, newActivity)
        newActivity
      case Some(activity) => activity
    }
    activity.include(commit, user)
    this
  }

}

abstract class GenericCommitHistogramFilter extends CommitHistogramFilter {
  val genericHistogram = new GenericCommitHistogram()

  override def getHistogram = genericHistogram
}
