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

import com.google.gerrit.reviewdb.client.Project
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.lib._
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.TreeFilter
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

case class NonBinaryFileFilter(projectKey: Project.NameKey, binaryFilesCache: BinaryFilesCache) extends TreeFilter {

  private val log = LoggerFactory.getLogger(classOf[NonBinaryFileFilter])

  private def open(reader: ObjectReader, mode: FileMode, id: ObjectId): Array[Byte] = {
      if(mode == FileMode.MISSING || id == ObjectId.zeroId() || mode.getObjectType != Constants.OBJ_BLOB) {
        Array.emptyByteArray
      }
      else {
        try {
          val ldr: ObjectLoader = reader.open(id.toObjectId)
          ldr.getCachedBytes()
        }
        catch {
          case NonFatal(e) =>
            log.error(s"Could not read blob: $id, defaulting to empty byte array [$mode, ${FileMode.MISSING}]", e)
            Array.emptyByteArray
        }
        finally {
          reader.close()
        }
      }
    }

  private def key(treeWalk: TreeWalk): String = s"${projectKey.get()}|${treeWalk.getPathString}"

  override def include(treeWalk: TreeWalk): Boolean = {
    if (treeWalk.isSubtree){
      true
    } else {
      log.debug(s"PATH:${key(treeWalk)}|HITs:${binaryFilesCache.hitCount}|MISS:${binaryFilesCache.missCount}|SIZE:${binaryFilesCache.size}")
      ! binaryFilesCache.get(key(treeWalk), _ => CacheBoolean(isBinary(treeWalk))).underlying
    }
  }

  def isBinary(treeWalk: TreeWalk): Boolean = {
    val objectId = if(treeWalk.getObjectId(0) != ObjectId.zeroId()) treeWalk.getObjectId(0) else treeWalk.getObjectId(1)
    val objectContent: Array[Byte] = open(treeWalk.getObjectReader, treeWalk.getFileMode(), objectId)
    val isBinary = RawText.isBinary(objectContent)
    binaryFilesCache.put(key(treeWalk), CacheBoolean(isBinary))
    isBinary
  }

  override def clone(): TreeFilter = this

  override def shouldBeRecursive(): Boolean = true
}