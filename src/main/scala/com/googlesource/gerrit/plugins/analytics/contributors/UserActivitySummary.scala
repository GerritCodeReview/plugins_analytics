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

package com.googlesource.gerrit.plugins.analytics.contributors

import org.eclipse.jgit.lib.ObjectId
import org.gitective.core.stat.UserCommitActivity

object UserActivitySummary {
  def fromUserActivity(uca: UserCommitActivity) =
    UserActivitySummary(uca.getName, uca.getEmail, uca.getCount,
      getCommits(uca.getIds, uca.getTimes, uca.getMerges), uca.getLatest)

  private def getCommits(ids: Array[ObjectId], times: Array[Long], merges: Array[Boolean]):
  Array[CommitInfo] = {
    (ids, times, merges).zipped.map((id, time, merge) => CommitInfo(id.name, time, merge))
  }
}

case class UserActivitySummary(name: String, email: String, numCommits: Int,
                               commits: Array[CommitInfo], lastCommitDate: Long)