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

import com.googlesource.gerrit.plugins.analytics.common.AggregatedCommitHistogram.AggregationStrategyMapping
import com.googlesource.gerrit.plugins.analytics.common.AggregationStrategy.DYNAMIC_AGGREGATION
import org.eclipse.jgit.revwalk.{RevCommit, RevWalk}
import org.slf4j.LoggerFactory

/**
  * Commit filter that includes commits only on the specified interval
  * starting from and to excluded
  */
class AggregatedHistogramFilterByDates(
              val from: Option[Long] = None,
              val to: Option[Long] = None,
              val branchesExtractor: Option[BranchesExtractor] = None,
              val hashTagsExtractor: Option[HashTagsExtractor] = None,
              val aggregationStrategy: AggregationStrategy = AggregationStrategy.EMAIL)
    extends AbstractCommitHistogramFilter(aggregationStrategy) {

  private final val log = LoggerFactory.getLogger(classOf[AggregatedHistogramFilterByDates])

  type MaybeBranch = Option[String]
  type MaybeHashTag = Option[String]

  private def getAggregationStrategies(
      commit: RevCommit): Seq[AggregationStrategy] = {

    val (branches, hashTags): (List[String], List[String]) = getAggregationValues(commit)

    val aggregationTuples: List[(MaybeBranch, MaybeHashTag)] = getAggregationTuples((branches, hashTags))

    val dynamicAggregationStrategies = getDynamicAggregationStrategies(aggregationTuples)

    if(dynamicAggregationStrategies.isEmpty) {
      Seq(aggregationStrategy)
    }
    else {
      dynamicAggregationStrategies
    }
  }

  private def getDynamicAggregationStrategies(aggregationTuples: List[(MaybeBranch, MaybeHashTag)]) = {
    aggregationTuples.map {
      case (maybeBranch: MaybeBranch, maybeHashTag: MaybeHashTag) =>
        val newMapping: AggregationStrategyMapping = (p, d) => {
          aggregationStrategy.mapping(p, d).copy(branch = maybeBranch, hashtag = maybeHashTag)
        }
        DYNAMIC_AGGREGATION(aggregationStrategy, newMapping)
    }
  }

  private def getAggregationTuples(aggregationValues: (List[String], List[String])): List[(MaybeBranch, MaybeHashTag)] = {

    val max = Math.max(aggregationValues._1.length, aggregationValues._2.length)
    val aggregationValuesTuple: (List[MaybeBranch],List[MaybeHashTag]) =
      (aggregationValues._1.map(Option(_)).padTo(max, None) , aggregationValues._2.map(Option(_)).padTo(max,None))

    def keepTuple: (MaybeBranch, MaybeHashTag) => Boolean = { (b, h) =>
      // If both the aggregations are defined, both the values of the tuple
      // need to be defined
      if (aggregationValues._1.nonEmpty && aggregationValues._2.nonEmpty) {
        b.isDefined && h.isDefined
      } else {
        true
      }
    }

    (for {
      maybeHashTag <- aggregationValuesTuple._2
      maybeBranch <- aggregationValuesTuple._1
      if keepTuple(maybeBranch, maybeHashTag)
    } yield (maybeBranch, maybeHashTag)).distinct
  }

  private def getAggregationValues(commit: RevCommit) = {
    val branches: List[String] = branchesExtractor
      .map { be =>
        be.branchesOfCommit(commit.getId).toList
      }
      .getOrElse(List.empty[String])
    val hashtags: List[String] = hashTagsExtractor
      .map {
        e =>
          e.tagsOfCommit(commit).toList
      }.getOrElse(List.empty[String])
    log.debug(s"Aggregable branches: $branches, aggregable hashtags: $hashtags")
    (branches, hashtags)
  }

  override def include(walker: RevWalk, commit: RevCommit) = {
    val commitDate = commit.getCommitterIdent.getWhen.getTime
    val author = commit.getAuthorIdent

    if (from.fold(true)(commitDate >=) && to.fold(true)(commitDate <)) {
      val aggregationsStrategies = getAggregationStrategies(commit)
      getHistogram.includeWithStrategies(commit, author, aggregationsStrategies)
      true
    } else {
      false
    }
  }

  override def clone =
    new AggregatedHistogramFilterByDates(from,
                                         to,
                                         branchesExtractor,
                                         hashTagsExtractor,
                                         aggregationStrategy)
}
