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

import com.google.gerrit.extensions.restapi.{BadRequestException, Response, RestReadView}
import com.google.gerrit.server.git.GitRepositoryManager
import com.google.gerrit.server.project.{ProjectResource, ProjectsCollection}
import com.google.gerrit.sshd.{CommandMetaData, SshCommand}
import com.google.inject.Inject
import com.googlesource.gerrit.plugins.analytics.common.DateConversions._
import com.googlesource.gerrit.plugins.analytics.common._
import org.eclipse.jgit.lib.ObjectId
import org.gitective.core.stat.UserCommitActivity
import org.kohsuke.args4j.{Option => ArgOption}


@CommandMetaData(name = "contributors", description = "Extracts the list of contributors to a project")
class ContributorsCommand @Inject()(val executor: ContributorsService,
                                    val projects: ProjectsCollection,
                                    val gsonFmt: GsonFormatter)
  extends SshCommand with ProjectResourceParser {

  private var beginDate: Option[Long] = None

  @ArgOption(name = "--since", aliases = Array("--after", "-b"),
    usage = "(included) begin timestamp. Must be in the format 2006-01-02[ 15:04:05[.890][ -0700]]")
  def setBeginDate(date: String) {
    try {
      beginDate = Some(date)
    } catch {
      case e: Exception => throw die(s"Invalid begin date ${e.getMessage}")
    }
  }

  private var endDate: Option[Long] = None

  @ArgOption(name = "--until", aliases = Array("--before", "-e"),
    usage = "(excluded) end timestamp. Must be in the format 2006-01-02[ 15:04:05[.890][ -0700]]")
  def setEndDate(date: String) {
    try {
      endDate = Some(date)
    } catch {
      case e: Exception => throw die(s"Invalid end date ${e.getMessage}")
    }
  }

  private var granularity: Option[AggregationStrategy] = None

  @ArgOption(name = "--granularity", aliases = Array("--aggregate", "-g"),
    usage = "Type of aggregation requested. ")
  def setGranularity(value: String) {
    try {
      granularity = Some(AggregationStrategy.apply(value))
    } catch {
      case e: Exception => throw die(s"Invalid granularity ${e.getMessage}")
    }
  }


  override protected def run =
    gsonFmt.format(executor.get(projectRes, beginDate, endDate,
      granularity.getOrElse(AggregationStrategy.EMAIL)), stdout)

}

class ContributorsResource @Inject()(val executor: ContributorsService,
                                     val gson: GsonFormatter)
  extends RestReadView[ProjectResource] {

  private var beginDate: Option[Long] = None

  @ArgOption(name = "--since", aliases = Array("--after", "-b"), metaVar = "QUERY",
    usage = "(included) begin timestamp. Must be in the format 2006-01-02[ 15:04:05[.890][ -0700]]")
  def setBeginDate(date: String) {
    try {
      beginDate = Some(date)
    } catch {
      case e: Exception => throw new BadRequestException(s"Invalid begin date ${e.getMessage}")
    }
  }

  private var endDate: Option[Long] = None

  @ArgOption(name = "--until", aliases = Array("--before", "-e"), metaVar = "QUERY",
    usage = "(excluded) end timestamp. Must be in the format 2006-01-02[ 15:04:05[.890][ -0700]]")
  def setEndDate(date: String) {
    try {
      endDate = Some(date)
    } catch {
      case e: Exception => throw new BadRequestException(s"Invalid end date ${e.getMessage}")
    }
  }

  private var granularity: Option[AggregationStrategy] = None

  @ArgOption(name = "--granularity", aliases = Array("--aggregate", "-g"), metaVar = "QUERY",
    usage = "(excluded) end timestamp. Must be in the format 2006-01-02[ 15:04:05[.890][ -0700]]")
  def setGreanularity(value: String) {
    try {
      granularity = Some(AggregationStrategy.apply(value))
    } catch {
      case e: Exception => throw new BadRequestException(s"Invalid granularity ${e.getMessage}")
    }
  }

  override def apply(projectRes: ProjectResource) =
    Response.ok(
      new GsonStreamedResult[UserActivitySummary](gson, executor.get(projectRes, beginDate, endDate, granularity.getOrElse(AggregationStrategy.EMAIL))))
}

class ContributorsService @Inject()(repoManager: GitRepositoryManager,
                                    histogram: UserActivityHistogram,
                                    gsonFmt: GsonFormatter) {

  def get(projectRes: ProjectResource, startDate: Option[Long], stopDate: Option[Long], aggregationStrategy: AggregationStrategy): TraversableOnce[UserActivitySummary] = {
    ManagedResource.use(repoManager.openRepository(projectRes.getNameKey)) {
      histogram.get(_, new AuthorHistogramFilterByDates(startDate, stopDate, aggregationStrategy.aggregationStrategyMapping))
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

