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

import com.google.gerrit.server.cache.CacheModule
import com.google.gerrit.server.cache.serialize.ObjectIdCacheSerializer
import org.eclipse.jgit.lib.ObjectId

class CommitsStatisticsCacheModule extends CacheModule() {

  override protected def configure(): Unit = {
    bind(classOf[CommitsStatisticsCache]).to(classOf[CommitsStatisticsCacheImpl])
    persist(CommitsStatisticsCache.COMMITS_STATISTICS_CACHE, classOf[ObjectId], classOf[CommitsStatistics])
      .version(1)
      .diskLimit(-1)
      .maximumWeight(100000)
      .keySerializer(ObjectIdCacheSerializer.INSTANCE)
      .valueSerializer(CommitsStatisticsCacheSerializer)
  }
}
