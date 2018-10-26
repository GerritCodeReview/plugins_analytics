// Copyright (C) 2017 The Android Open Source Project
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

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.{RevCommit, RevWalk}

/**
  * Commit filter that includes commits only on the specified interval
  * starting from and to excluded
  */
class AggregatedHistogramFilterByDates(repo: Repository, val from: Option[Long] = None, val to: Option[Long] = None,
                                       val extractBranches: Boolean = false,
                                       val aggregationStrategy: AggregationStrategy = AggregationStrategy.EMAIL)
  extends AbstractCommitHistogramFilter(aggregationStrategy) {

  override def include(walker: RevWalk, commit: RevCommit) = {
    val commitDate = commit.getCommitterIdent.getWhen.getTime
    val author = commit.getAuthorIdent

    if (from.fold(true)(commitDate >=) && to.fold(true)(commitDate <)) {
      if(extractBranches) {
        val branches = new CommitsBranches(repo, from, to).forCommits(Set(commit.getId))
        getHistogram.includeWithBranches(commit, author, branches)
      } else {
        getHistogram.include(commit, author)
      }
      true
    } else {
      false
    }
  }

  override def clone = new AggregatedHistogramFilterByDates(repo, from, to, extractBranches, aggregationStrategy)
}