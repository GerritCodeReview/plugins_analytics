package com.googlesource.gerrit.plugins.analytics.common

import org.eclipse.jgit.revwalk.RevCommit

object RevisionBrowsingSupport {

  implicit class PimpedRevCommit(val self: RevCommit) extends AnyVal {
    def isMerge: Boolean = self.getParentCount > 1
  }

}
