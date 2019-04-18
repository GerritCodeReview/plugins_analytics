// Copyright (C) 2019 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.analytics.common

import java.util.concurrent.Callable

import com.google.common.cache.Cache
import com.google.inject.Inject
import com.google.inject.name.Named
import org.eclipse.jgit.lib.ObjectId

trait CommitsStatisticsCache {
  def get(objectId: ObjectId, funStats: ObjectId => CommitsStatistics): CommitsStatistics
  def put(objectId: ObjectId, commitsStatistics: CommitsStatistics): Unit
  def hitCount: Long
  def evictionCount: Long
  def missCount: Long
  def size: Long
}

object CommitsStatisticsCache {
  final val COMMITS_STATISTICS_CACHE = "commits_statistics_cache"
}

class CommitsStatisticsCacheImpl @Inject() (
  @Named(CommitsStatisticsCache.COMMITS_STATISTICS_CACHE) commitStatsCache: Cache[ObjectId, CommitsStatistics]
) extends CommitsStatisticsCache {

  override def get(objectId: ObjectId, funStats: ObjectId => CommitsStatistics): CommitsStatistics =
    commitStatsCache.get(objectId, new Callable[CommitsStatistics] {
        override def call(): CommitsStatistics = funStats(objectId)
      })

  override def put(objectId: ObjectId, commitsStatistics: CommitsStatistics): Unit = commitStatsCache.put(objectId, commitsStatistics)

  override def hitCount: Long = commitStatsCache.stats().hitCount()

  override def evictionCount: Long = commitStatsCache.stats().evictionCount()

  override def missCount: Long = commitStatsCache.stats().missCount()

  override def size: Long = commitStatsCache.size()
}