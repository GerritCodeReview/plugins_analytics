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

package com.googlesource.gerrit.plugins.analytics

import com.google.gerrit.extensions.restapi.{Response, RestReadView}
import com.google.gerrit.server.git.GitRepositoryManager
import com.google.gerrit.server.project.{ProjectResource, ProjectsCollection}
import com.google.gerrit.sshd.{CommandMetaData, SshCommand}
import com.google.inject.Inject
import com.googlesource.gerrit.plugins.analytics.common._
import org.eclipse.jgit.lib.ObjectId
import org.gitective.core.stat.UserCommitActivity
import org.kohsuke.args4j.Option
import org.slf4j.LoggerFactory


@CommandMetaData(name = "contributors", description = "Extracts the list of contributors to a project")
class ContributorsCommand @Inject()(val executor: ContributorsService,
                                    val projects: ProjectsCollection,
                                    val gsonFmt: GsonFormatter)
  extends SshCommand with ProjectResourceParser with Dates {
  @Option(name = "--begin", usage = "(included) begin date in YYYYMMDD format")
  private var beginDate = MIN_DATE
  @Option(name = "--end", usage = "(excluded) end date in YYYYMMDD format")
  private var endDate = MAX_DATE
  @Option(name = "--all", usage = "consider all dates")
  def setAll(b: Boolean) = {
    beginDate = MIN_DATE
    endDate = MAX_DATE
  }
  override protected def run =
    gsonFmt.format(executor.get(projectRes, beginDate, endDate), stdout)

}

class ContributorsResource @Inject()(val executor: ContributorsService,
                                     val gson: GsonFormatter)
  extends RestReadView[ProjectResource] with Dates {
  @Option(name = "--begin", aliases = Array("-b"), metaVar = "QUERY", usage = "Begin date YYYYMMDD")
  var beginDate = MIN_DATE
  @Option(name = "--end", aliases = Array("-e"), metaVar = "QUERY", usage = "(Excluded) end date YYYYMMDD")
  var endDate = MAX_DATE
  @Option(name = "--all", aliases = Array("-a"), metaVar = "QUERY", usage = "consider all dates")
  def setAll(b: Boolean) = {
    beginDate = MIN_DATE
    endDate = MAX_DATE
  }


  override def apply(projectRes: ProjectResource) =
    Response.ok(
      new GsonStreamedResult[UserActivitySummary](gson, executor.get(projectRes, beginDate, endDate)))
}

class ContributorsService @Inject()(repoManager: GitRepositoryManager,
                                    histogram: UserActivityHistogram,
                                    gsonFmt: GsonFormatter) extends Dates {
  private val log = LoggerFactory.getLogger(classOf[ContributorsCommand])

  def get(projectRes: ProjectResource, startDate: String, stopDate: String): TraversableOnce[UserActivitySummary] = {
    log.info(s"Running with startDate: $startDate, stopDate: $stopDate")

    ManagedResource.use(repoManager.openRepository(projectRes.getNameKey)) {
      histogram.get(_, new AuthorHistogramByDates(parse_date(startDate), parse_date(stopDate)))
        .par
        .map(UserActivitySummary.apply).toStream
    }
  }
}

case class CommitInfo(sha1: String, date: Long, merge: Boolean)

case class UserActivitySummary(name: String, email: String, numCommits: Int,
                               commits: Array[CommitInfo], lastCommitDate: Long)

object UserActivitySummary {
  def apply(uca: UserCommitActivity): UserActivitySummary =
    UserActivitySummary(uca.getName, uca.getEmail, uca.getCount,
      getCommits(uca.getIds, uca.getTimes, uca.getMerges), uca.getLatest)

  private def getCommits(ids: Array[ObjectId], times: Array[Long], merges: Array[Boolean]):
  Array[CommitInfo] = {
    (ids, times, merges).zipped.map((id, time, merge) => CommitInfo(id.name, time, merge))
  }
}

