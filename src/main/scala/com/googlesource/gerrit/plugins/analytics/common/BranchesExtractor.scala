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

import com.googlesource.gerrit.plugins.analytics.common.ManagedResource.use
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand.ListMode
import org.eclipse.jgit.lib.{Constants, ObjectId, Repository}
import org.eclipse.jgit.revwalk.RevWalk

import scala.collection.JavaConverters._

class BranchesExtractor(repo: Repository) {

  def findBranches(targetCommit: ObjectId): Set[String] = {
    use(new Git(repo)) { git =>
      use(new RevWalk(repo)) { revWalk =>
        val targetRevCommit = revWalk.parseCommit(targetCommit)
        git.branchList()
          .setListMode(ListMode.ALL)
          .call()
          .asScala
          .flatMap { branch =>
            val branchCommit = revWalk.parseCommit(branch.getObjectId)
            if (revWalk.isMergedInto(targetRevCommit, branchCommit))
              Some(branch.getName.drop(Constants.R_HEADS.length))
            else None
          }
          .toSet
      }
    }
  }
}

