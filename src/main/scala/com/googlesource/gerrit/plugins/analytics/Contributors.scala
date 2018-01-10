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

package com.googlesource.gerrit.plugins.analytics

import com.google.gerrit.extensions.restapi.{BadRequestException, Response, RestReadView}
import com.google.gerrit.server.git.GitRepositoryManager
import com.google.gerrit.server.project.{ProjectResource, ProjectsCollection}
import com.google.gerrit.sshd.{CommandMetaData, SshCommand}
import com.google.inject.Inject
import com.googlesource.gerrit.plugins.analytics.common.DateConversions._
import com.googlesource.gerrit.plugins.analytics.common._
import org.kohsuke.args4j.{Option => ArgOption}


@CommandMetaData(name = "contributors", description = "Extracts the list of contributors to a project")
class ContributorsCommand @Inject()(val executor: ContributorsService,
                                    val projects: ProjectsCollection,
                                    val gsonFmt: GsonFormatter)
  extends SshCommand with ProjectResourceParser {

  private var beginDate: Option[Long] = None
  private var endDate: Option[Long] = None
  private var granularity: Option[AggregationStrategy] = None

  @ArgOption(name = "--extract-branches", aliases = Array("-r"),
    usage = "Do extra parsing to extract a list of all branches for each line")
  private var extractBranches: Boolean = false

  @ArgOption(name = "--since", aliases = Array("--after", "-b"),
    usage = "(included) begin timestamp. Must be in the format 2006-01-02[ 15:04:05[.890][ -0700]]")
  def setBeginDate(date: String) {
    try {
      beginDate = Some(date)
    } catch {
      case e: Exception => throw die(s"Invalid begin date ${e.getMessage}")
    }
  }

  @ArgOption(name = "--until", aliases = Array("--before", "-e"),
    usage = "(excluded) end timestamp. Must be in the format 2006-01-02[ 15:04:05[.890][ -0700]]")
  def setEndDate(date: String) {
    try {
      endDate = Some(date)
    } catch {
      case e: Exception => throw die(s"Invalid end date ${e.getMessage}")
    }
  }

  @ArgOption(name = "--aggregate", aliases = Array("-g"),
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
      granularity.getOrElse(AggregationStrategy.EMAIL), extractBranches), stdout)

}

class ContributorsResource @Inject()(val executor: ContributorsService,
                                     val gson: GsonFormatter)
  extends RestReadView[ProjectResource] {

  private var beginDate: Option[Long] = None
  private var endDate: Option[Long] = None
  private var granularity: Option[AggregationStrategy] = None

  @ArgOption(name = "--since", aliases = Array("--after", "-b"), metaVar = "QUERY",
    usage = "(included) begin timestamp. Must be in the format 2006-01-02[ 15:04:05[.890][ -0700]]")
  def setBeginDate(date: String) {
    try {
      beginDate = Some(date)
    } catch {
      case e: Exception => throw new BadRequestException(s"Invalid begin date ${e.getMessage}")
    }
  }

  @ArgOption(name = "--until", aliases = Array("--before", "-e"), metaVar = "QUERY",
    usage = "(excluded) end timestamp. Must be in the format 2006-01-02[ 15:04:05[.890][ -0700]]")
  def setEndDate(date: String) {
    try {
      endDate = Some(date)
    } catch {
      case e: Exception => throw new BadRequestException(s"Invalid end date ${e.getMessage}")
    }
  }

  @ArgOption(name = "--granularity", aliases = Array("--aggregate", "-g"), metaVar = "QUERY",
    usage = "can be one of EMAIL, EMAIL_HOUR, EMAIL_DAY, EMAIL_MONTH, EMAIL_YEAR, defaulting to EMAIL")
  def setGranularity(value: String) {
    try {
      granularity = Some(AggregationStrategy.apply(value))
    } catch {
      case e: Exception => throw new BadRequestException(s"Invalid granularity ${e.getMessage}")
    }
  }

  @ArgOption(name = "--extract-branches", aliases = Array("-r"),
    usage = "Do extra parsing to extract a list of all branches for each line")
  private var extractBranches: Boolean = false

  override def apply(projectRes: ProjectResource) =
    Response.ok(
      new GsonStreamedResult[UserActivitySummary](gson,
        executor.get(projectRes, beginDate, endDate,
          granularity.getOrElse(AggregationStrategy.EMAIL), extractBranches)))
}

class ContributorsService @Inject()(repoManager: GitRepositoryManager,
                                    histogram: UserActivityHistogram,
                                    gsonFmt: GsonFormatter) {
  def get(projectRes: ProjectResource, startDate: Option[Long], stopDate: Option[Long],
          aggregationStrategy: AggregationStrategy, extractBranches: Boolean)
  : TraversableOnce[UserActivitySummary] = {
    ManagedResource.use(repoManager.openRepository(projectRes.getNameKey)) { repo =>
      val stats = new Statistics(repo)
      import RichBoolean._
      val commitsBranchesOptionalEnricher = extractBranches.option(
        new CommitsBranches(repo, startDate, stopDate)
      )
      histogram.get(repo, new AggregatedHistogramFilterByDates(startDate, stopDate,
        aggregationStrategy))
        .par
        .flatMap(UserActivitySummary.apply(stats, commitsBranchesOptionalEnricher))
        .toStream
    }
  }
}

case class CommitInfo(sha1: String, date: Long, merge: Boolean)

case class UserActivitySummary(year: Integer,
                               month: Integer,
                               day: Integer,
                               hour: Integer,
                               name: String,
                               email: String,
                               numCommits: Integer,
                               numFiles: Integer,
                               addedLines: Integer,
                               deletedLines: Integer,
                               commits: Array[CommitInfo],
                               branches: Array[String],
                               lastCommitDate: Long,
                               isMerge: Boolean
                              )

object UserActivitySummary {
  def apply(statisticsHandler: Statistics,
            branchesLabeler: Option[CommitsBranches])
           (uca: AggregatedUserCommitActivity)
  : Iterable[UserActivitySummary] = {
    val INCLUDESEMPTY = -1

    implicit def stringToIntOrNull(x: String): Integer = if (x.isEmpty) null else new Integer(x)

    uca.key.split("/", INCLUDESEMPTY) match {
      case Array(email, year, month, day, hour) =>
        val branches = branchesLabeler.fold(Set.empty[String]) {
          labeler => labeler.forCommits(uca.getIds)
        }
        statisticsHandler.forCommits(uca.getIds: _*).map { stat =>
          UserActivitySummary(
            year, month, day, hour, uca.getName, uca.getEmail, uca.getCount,
            stat.numFiles, stat.addedLines, stat.deletedLines,
            stat.commits.toArray, branches.toArray, uca.getLatest, stat.isForMergeCommits
          )
        }
      case _ => throw new Exception(s"invalid key format found ${uca.key}")
    }
  }
}

