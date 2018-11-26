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
import com.googlesource.gerrit.plugins.analytics.common.AggregationStrategy.GENERIC_AGGREGATION
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

  val log = LoggerFactory.getLogger(classOf[AggregatedHistogramFilterByDates])

  private def getExtendedAggregationStrategies(
      commit: RevCommit): Seq[AggregationStrategy] = {

    val branches: List[String] = branchesExtractor
      .map{ be =>
        be.branchesOfCommit(commit.getId).toList
      }
      .getOrElse(List.empty[String])
    val hashtags: List[String] = hashTagsExtractor
      .map {
        e =>
          e.tagsOfCommit(commit).toList
      }.getOrElse(List.empty[String])

    log.debug(s"Aggregable branches: $branches, aggregable hashtags: $hashtags")

    val max = Math.max(branches.length, hashtags.length)

    val aggregationPropertiesTuple: (List[Option[String]],List[Option[String]]) =
      if (branches.length > hashtags.length) {
        val paddedProperty: List[Option[String]] = hashtags.map(Option(_)) ++ List.fill(max - hashtags.length)(None)
          (branches.map(Option(_)) , paddedProperty)
      } else {
        val paddedProperty: List[Option[String]] = branches.map(Option(_)) ++ List.fill(max - branches.length)(None)
          (hashtags.map(Option(_)), paddedProperty)
      }

    val aggregationTuple =
      if (branches.nonEmpty && hashtags.nonEmpty) {
        for {
          firstAggregationProperty <- aggregationPropertiesTuple._1
          secondAggregationProperty <- aggregationPropertiesTuple._2
          if secondAggregationProperty.isDefined && firstAggregationProperty.isDefined
        } yield (firstAggregationProperty, secondAggregationProperty)
      } else {
        aggregationPropertiesTuple._1.map((_,None))
      }

    val extendedAggregationStrategies = aggregationTuple.map {
      case (firstProperty, secondProperty) =>
        val newMapping: AggregationStrategyMapping = (p, d) => {
          val propertyTuple: (Option[String], Option[String]) = if (branches.length > hashtags.length) {
            (firstProperty, secondProperty)
          } else {
            (secondProperty, firstProperty)
          }
          aggregationStrategy.mapping(p, d).copy(branch = propertyTuple._1, hashtag = propertyTuple._2)
        }
        GENERIC_AGGREGATION(aggregationStrategy, newMapping)
    }

    if(extendedAggregationStrategies.isEmpty) {
      Seq(aggregationStrategy)
    }
    else {
      extendedAggregationStrategies
    }
  }

  override def include(walker: RevWalk, commit: RevCommit) = {
    val commitDate = commit.getCommitterIdent.getWhen.getTime
    val author = commit.getAuthorIdent

    if (from.fold(true)(commitDate >=) && to.fold(true)(commitDate <)) {
      val extendedAggregations = getExtendedAggregationStrategies(commit)
      getHistogram.includeWithStrategies(commit, author, extendedAggregations)
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
