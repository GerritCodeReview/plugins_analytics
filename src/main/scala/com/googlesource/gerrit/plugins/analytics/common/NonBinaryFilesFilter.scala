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
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.TreeFilter

case class NonBinaryFilesFilter(projectKey: Project.NameKey, binaryFilesCache: BinaryFilesCache) extends TreeFilter {

  override def include(treeWalk: TreeWalk): Boolean = {
    if (treeWalk.isSubtree){
      true
    } else {
      val objectId = if(treeWalk.getObjectId(0) != ObjectId.zeroId()) treeWalk.getObjectId(0) else treeWalk.getObjectId(1)
      !binaryFilesCache.get(projectKey.get(), objectId, treeWalk.getPathString).underlying
    }
  }

  override def clone(): TreeFilter = this

  override def shouldBeRecursive(): Boolean = true
}