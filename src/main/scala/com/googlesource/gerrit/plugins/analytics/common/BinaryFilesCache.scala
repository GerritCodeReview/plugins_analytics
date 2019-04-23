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

case class CacheBoolean(underlying: Boolean) extends AnyVal

trait BinaryFilesCache {
  def get(key: String, getterF: String => CacheBoolean): CacheBoolean
  def put(key: String, value: CacheBoolean): Unit
  def hitCount: Long
  def evictionCount: Long
  def missCount: Long
  def size: Long
}

object BinaryFilesCache {
  final val BINARY_FILES_CACHE = "binary_files_cache"
}

class BinaryFilesCacheImpl @Inject() (
  @Named(BinaryFilesCache.BINARY_FILES_CACHE) binaryStatsCache: Cache[String, CacheBoolean]
) extends BinaryFilesCache {
  override def get(filePath: String, funBool: String => CacheBoolean): CacheBoolean = binaryStatsCache.get(filePath, new Callable[CacheBoolean] {
    override def call(): CacheBoolean = funBool(filePath)
  })

  override def put(filePath: String, isBinary: CacheBoolean): Unit = binaryStatsCache.put(filePath, isBinary)

  override def hitCount: Long = binaryStatsCache.stats().hitCount()

  override def evictionCount: Long = binaryStatsCache.stats().evictionCount()

  override def missCount: Long = binaryStatsCache.stats().missCount()

  override def size: Long = binaryStatsCache.size()
}