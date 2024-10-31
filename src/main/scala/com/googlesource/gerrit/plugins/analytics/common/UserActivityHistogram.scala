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

import com.google.gerrit.extensions.restapi.PreconditionFailedException
import com.google.inject.Singleton
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.Constants.HEAD
import org.gitective.core.CommitFinder

@Singleton
class UserActivityHistogram {
  def get(repo: Repository, filter: AbstractCommitHistogramFilter, branchFilter: Option[String]) = {
    val finder = new CommitFinder(repo)

    try {
      finder.setFilter(filter).findFrom(branchFilter.getOrElse(HEAD))
      val histogram = filter.getHistogram
      histogram.getAggregatedUserActivity
    } catch {
      // 'find' throws an IllegalArgumentException when the conditions to walk through the commits tree are not met,
      // i.e: an empty repository doesn't have the starting commit.
      case _: IllegalArgumentException => Array.empty[AggregatedUserCommitActivity]
      case e: Exception => throw new PreconditionFailedException(s"Cannot find commits: ${e.getMessage}").initCause(e)
    }
  }
}