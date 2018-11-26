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

import com.google.gerrit.extensions.api.projects.CommentLinkInfo
import com.google.gerrit.extensions.restapi.{BadRequestException, Response, RestReadView}
import com.google.gerrit.server.git.GitRepositoryManager
import com.google.gerrit.server.notedb.{AbstractChangeNotes, ChangeNoteJson, LegacyChangeNoteRead, NoteDbMetrics}
import com.google.gerrit.server.project.{ProjectCache, ProjectResource}
import com.google.gerrit.server.restapi.project.ProjectsCollection
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

  @ArgOption(name = "--extract-hashtags", aliases = Array("-t"),
    usage = "Do extra parsing to extract hashtags associated to a change")
  private var extractHashTags: Boolean = false

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

  @ArgOption(name = "--extract-issues", aliases = Array("-i"),
    usage = "Extract a list of issues and links using the Gerrit's commentLink configuration")
  private var extractIssues: Boolean = false

  override protected def run =
    gsonFmt.format(executor.get(projectRes, beginDate, endDate,
      granularity.getOrElse(AggregationStrategy.EMAIL), extractBranches, extractIssues, extractHashTags), stdout)

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

  @ArgOption(name = "--extract-hashtags", aliases = Array("-t"),
    usage = "Do extra parsing to extract hashtags associated to a change")
  private var extractHashTags: Boolean = false

  @ArgOption(name = "--extract-issues", aliases = Array("-i"),
    usage = "Extract a list of issues and links using the Gerrit's commentLink configuration")
  private var extractIssues: Boolean = false

  override def apply(projectRes: ProjectResource) =
    Response.ok(
      new GsonStreamedResult[UserActivitySummary](gson,
        executor.get(projectRes, beginDate, endDate,
          granularity.getOrElse(AggregationStrategy.EMAIL), extractBranches, extractIssues, extractHashTags)))
}

class ContributorsService @Inject()(repoManager: GitRepositoryManager,
                                    projectCache:ProjectCache,
                                    changeNotesArgs: AbstractChangeNotes.Args,
                                    changeNoteJson: ChangeNoteJson,
                                    noteDbMetrics: NoteDbMetrics,
                                    histogram: UserActivityHistogram,
                                    legacyChangeNoteRead: LegacyChangeNoteRead,
                                    gsonFmt: GsonFormatter) {
  import RichBoolean._

  import scala.collection.JavaConverters._

  def get(projectRes: ProjectResource, startDate: Option[Long], stopDate: Option[Long],
          aggregationStrategy: AggregationStrategy, extractBranches: Boolean, extractIssues: Boolean, extractHashTags: Boolean)
  : TraversableOnce[UserActivitySummary] = {
    val nameKey = projectRes.getNameKey
    val commentLinks: List[CommentLinkInfo] = extractIssues.option {
      projectCache.get(nameKey).getCommentLinks.asScala
    }.toList.flatten

    ManagedResource.use(repoManager.openRepository(projectRes.getNameKey)) { repo =>
      val stats = new Statistics(repo, commentLinks.asJava)
      val branchesExtractor = extractBranches.option(new BranchesExtractor(repo))
      val maybeHashtagExtractor = extractHashTags.option(new HashTagsExtractorImpl(changeNotesArgs, changeNoteJson, legacyChangeNoteRead, noteDbMetrics, repo, projectRes.getNameKey))

      histogram.get(repo, new AggregatedHistogramFilterByDates(startDate, stopDate, branchesExtractor, maybeHashtagExtractor, aggregationStrategy))
        .par
        .flatMap(aggregatedCommitActivity => UserActivitySummary.apply(stats)(aggregatedCommitActivity))
        .toStream
    }
  }
}

case class CommitInfo(sha1: String, date: Long, merge: Boolean, files: java.util.Set[String])

case class IssueInfo(code: String, link: String)

case class UserActivitySummary(year: Option[Int],
                               month: Option[Int],
                               day: Option[Int],
                               hour: Option[Int],
                               name: String,
                               email: String,
                               numCommits: Integer,
                               numFiles: Integer,
                               numDistinctFiles: Integer,
                               addedLines: Integer,
                               deletedLines: Integer,
                               commits: Array[CommitInfo],
                               branches: Array[String],
                               hashtag: Option[String],
                               issuesCodes: Array[String],
                               issuesLinks: Array[String],
                               lastCommitDate: Long,
                               isMerge: Boolean
                              )

object UserActivitySummary {
  def apply(statisticsHandler: Statistics)(uca: AggregatedUserCommitActivity)
  : Iterable[UserActivitySummary] = {

    implicit def stringToIntOrNull(x: String): Integer = if (x.isEmpty) null else new Integer(x)

    statisticsHandler.forCommits(uca.getIds: _*).map { stat =>
      val maybeBranches =
        uca.key.branch.filter(_.nonEmpty).map(b => Array(b)).getOrElse(Array.empty)

          UserActivitySummary(
            uca.key.year, uca.key.month, uca.key.day, uca.key.hour, uca.getName, uca.key.email, stat.commits.size,
            stat.numFiles, stat.numDistinctFiles, stat.addedLines, stat.deletedLines,
            stat.commits.toArray, maybeBranches, uca.key.hashtag ,stat.issues.map(_.code)
              .toArray, stat.issues.map(_.link).toArray, uca.getLatest, stat
              .isForMergeCommits
          )
        }

  }
}

