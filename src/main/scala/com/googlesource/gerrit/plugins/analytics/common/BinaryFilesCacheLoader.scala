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

import com.google.common.cache.CacheLoader
import com.google.gerrit.reviewdb.client.Project
import com.google.gerrit.server.git.GitRepositoryManager
import com.google.gerrit.server.project.ProjectCache
import com.google.inject.Inject
import com.googlesource.gerrit.plugins.analytics.AnalyticsConfig
import com.googlesource.gerrit.plugins.analytics.common.ManagedResource.use
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.lib._
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

class BinaryFilesCacheLoader @Inject()(
  gitRepositoryManager: GitRepositoryManager,
  projectCache: ProjectCache,
  botLikeExtractor: BotLikeExtractor,
  config: AnalyticsConfig
) extends CacheLoader[BinaryFilesCacheKey, CacheBoolean] {

  private val log = LoggerFactory.getLogger(classOf[BinaryFilesCacheLoader])

  override def load(cacheKey: BinaryFilesCacheKey): CacheBoolean = {
    val nameKey = new Project.NameKey(cacheKey.projectName)

    log.warn(s"TONYCACHE|${cacheKey.filePath}|${cacheKey.objectId}")

    use(gitRepositoryManager.openRepository(nameKey)) { repo => isBinary(cacheKey.objectId, cacheKey.filePath, repo.newObjectReader()) }
  }

  private def open(reader: ObjectReader, filePath: String, id: ObjectId): Array[Byte] = {
    try {
      val ldr: ObjectLoader = reader.open(id.toObjectId)
      ldr.getCachedBytes()
    }
    catch {
      case NonFatal(e) =>
        log.warn(s"Could not read blob: $id [file: $filePath], defaulting to empty byte array")
        Array.emptyByteArray
    }
    finally {
      reader.close()
    }
  }

  private def isBinary(objectId: ObjectId, filePath: String, objectReader: ObjectReader): CacheBoolean = {
    val objectContent: Array[Byte] = open(objectReader, filePath, objectId)
    val isBinary = RawText.isBinary(objectContent)
    CacheBoolean(isBinary)
  }
}