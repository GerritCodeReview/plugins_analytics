// Copyright (C) 2016 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.analytics.common

import java.util.Date

import org.eclipse.jgit.revwalk.{RevCommit, RevWalk}
import org.gitective.core.stat.CommitHistogramFilter

/**
  * Commit filter that includes commits only on the specified interval
  * starting from and to excluded
  */
class AuthorHistogramByDates(val from: Date, val to: Date)
  extends CommitHistogramFilter {

  override def include(walker: RevWalk, commit: RevCommit) = {
    def isInsideInterval(commit: RevCommit): Boolean = {
      val author = commit.getAuthorIdent
      if (author != null) {
        val commitDate = author.getWhen
        if (commitDate.after(from) && commitDate.before(to)) {
          histogram.include(commit, author)
          true
        } else false
      } else false
    }

    isInsideInterval(commit)
  }

  override def clone = new AuthorHistogramByDates(from, to)
}