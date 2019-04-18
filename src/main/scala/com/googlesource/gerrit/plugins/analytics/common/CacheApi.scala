// Copyright (C) 2018 GerritForge Ltd
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

import org.eclipse.jgit.lib.ObjectId

trait CacheApi[A, B] {
  def getOrUpdate(el: A, f: => A => B): B
}

final class InMemoryCommitStatisticsCache extends CacheApi[ObjectId, CommitsStatistics] {

  private val cache = collection.mutable.Map.empty[ObjectId, CommitsStatistics]

  override def getOrUpdate(el: ObjectId, f: => ObjectId => CommitsStatistics): CommitsStatistics = cache.getOrElse(el, {
    cache.update(el, f(el))
    cache(el)
  })
}
