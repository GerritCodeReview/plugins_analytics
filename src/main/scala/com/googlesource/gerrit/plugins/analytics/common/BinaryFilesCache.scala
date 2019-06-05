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

import com.google.common.cache.LoadingCache
import com.google.inject.Inject
import com.google.inject.name.Named
import com.googlesource.gerrit.plugins.analytics.common.BinaryFilesCache.BINARY_FILES_CACHE
import org.eclipse.jgit.lib.ObjectId

trait BinaryFilesCache {
  def get(project: String, objectId: ObjectId, filePath: String): CacheBoolean
}

case class BinaryFilesCacheKey(projectName: String, objectId: ObjectId, filePath: String)

object BinaryFilesCache {
  final val BINARY_FILES_CACHE = "binary_files_cache"
}

class BinaryFilesCacheImpl @Inject() (@Named(BINARY_FILES_CACHE) binaryFilesCache: LoadingCache[BinaryFilesCacheKey, CacheBoolean]
) extends BinaryFilesCache {

  override def get(project: String, objectId: ObjectId, filePath: String): CacheBoolean =
    binaryFilesCache.get(BinaryFilesCacheKey(project, objectId, filePath))
}

case class CacheBoolean(underlying: Boolean) extends AnyVal