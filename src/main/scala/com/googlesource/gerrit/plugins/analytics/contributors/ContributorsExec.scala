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

import com.google.gerrit.server.git.GitRepositoryManager
import com.google.gerrit.server.project.ProjectResource
import com.google.inject.Inject
import com.googlesource.gerrit.plugins.analytics.common.{FindHistogram, JsonFormatter, ManagedResource}
import org.eclipse.jgit.lib.ObjectId
import org.gitective.core.stat.{AuthorHistogramFilter, UserCommitActivity}

class ContributorsExec @Inject()(
                                  repoManager: GitRepositoryManager,
                                  histogram: FindHistogram,
                                  gsonFmt: JsonFormatter) {

  def get(projectRes: ProjectResource): TraversableOnce[UserActivitySummary] = {
    ManagedResource.use(repoManager.openRepository(projectRes.getNameKey))(
      histogram.getUserActivity(_, new AuthorHistogramFilter)
        .par.map(UserActivitySummary.fromUserActivity).toStream
    )
  }
}

case class CommitInfo(val sha1: String, val date: Long, val merge: Boolean)

case class UserActivitySummary(name: String, email: String, numCommits: Int,
                               commits: Array[CommitInfo], lastCommitDate: Long)

object UserActivitySummary {
  def fromUserActivity(uca: UserCommitActivity) =
    UserActivitySummary(uca.getName, uca.getEmail, uca.getCount,
      getCommits(uca.getIds, uca.getTimes, uca.getMerges), uca.getLatest)

  private def getCommits(ids: Array[ObjectId], times: Array[Long], merges: Array[Boolean]):
  Array[CommitInfo] = {
    (ids, times, merges).zipped.map((id, time, merge) => CommitInfo(id.name, time, merge))
  }

}

