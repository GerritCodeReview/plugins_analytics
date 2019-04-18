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

import com.google.common.cache.{Cache, LoadingCache}
import com.google.gerrit.extensions.api.projects.CommentLinkInfo
import com.google.gerrit.server.cache.serialize.{CacheSerializer, ObjectIdCacheSerializer}
import com.googlesource.gerrit.plugins.analytics.{CommitInfo, IssueInfo}
import com.googlesource.gerrit.plugins.analytics.common.ManagedResource.use
import org.eclipse.jgit.diff.{DiffFormatter, RawTextComparator}
import org.eclipse.jgit.lib.{ObjectId, ObjectIdSerializer, Repository}
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.{CanonicalTreeParser, EmptyTreeIterator}
import org.eclipse.jgit.util.io.DisabledOutputStream

import scala.util.matching.Regex
import scala.collection.JavaConverters._

trait CommitsStatisticsCache {
  def get(project: String, objectId: ObjectId): CommitsStatistics
}

case class CommitsStatisticsCacheKey(projectName: String, commitId: ObjectId)

object CommitsStatisticsCache {
  final val COMMITS_STATISTICS_CACHE = "commits_statistics_cache"
}

case class CommitsStatisticsCacheImpl (
  commitStatsCache: LoadingCache[CommitsStatisticsCacheKey, CommitsStatistics]
) extends CommitsStatisticsCache {

  override def get(project: String, objectId: ObjectId): CommitsStatistics =
    commitStatsCache.get(CommitsStatisticsCacheKey(project, objectId))
}