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
import com.googlesource.gerrit.plugins.analytics.common.AggregationStrategy.{BY_BRANCH, BY_HASHTAG, GENERIC_AGG}
import org.eclipse.jgit.revwalk.{RevCommit, RevWalk}

/**
  * Commit filter that includes commits only on the specified interval
  * starting from and to excluded
  */
class AggregatedHistogramFilterByDates(val from: Option[Long] = None, val to: Option[Long] = None,
                                       val branchesExtractor: Option[BranchesExtractor] = None,
                                       val aggregationStrategy: AggregationStrategy = AggregationStrategy.EMAIL)
  extends AbstractCommitHistogramFilter(aggregationStrategy) {

  implicit class Cartesian[X](xs: Traversable[X]) {
    def cart[Y](ys: Traversable[Y]) = for { x <- xs; y <- ys } yield (x, y)
  }

//  case class HashTagExtractor()
//  val hashTagsExtractor: Option[HashTagExtractor] = ???

  def getExtendedAggregationStrategies(commit: RevCommit): Seq[AggregationStrategy] = {

    val extractBranches = branchesExtractor.isDefined
    val extractHashtags = false

    val branches: Seq[Option[String]] = branchesExtractor.map(
      _.branchesOfCommit(commit.getId).map(Some(_)).toSeq)
      .getOrElse(Seq.empty[Option[String]])
//    val hashtags: Seq[Option[String]] = Seq("hashtag1", "hashtag2").map(Some(_))
    val hashtags: Seq[Option[String]] = Seq.empty

    val max = Math.max(branches.length, hashtags.length)

    val aggregationTuple = (for {
      b <- List.tabulate(max)(i => branches.lift(i).getOrElse(None))
      h <- List.tabulate(max)(i => hashtags.lift(i).getOrElse(None))
      if !extractBranches || b.isDefined
      if !extractHashtags || h.isDefined
    } yield (b, h)).distinct

    aggregationTuple.map{ case (b,h) =>
      val newMapping: AggregationStrategyMapping = (p, d) =>
      aggregationStrategy.mapping(p, d).copy(hashtag = h, branch = b)

      GENERIC_AGG(aggregationStrategy, newMapping)
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

  override def clone = new AggregatedHistogramFilterByDates(from, to, branchesExtractor, aggregationStrategy)
}