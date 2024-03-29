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

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.{Constants, ObjectId, Repository}
import org.eclipse.jgit.revwalk.RevWalk

import scala.jdk.CollectionConverters._
import scala.util.Using

case class BranchesExtractor(repo: Repository) {
  lazy val branchesOfCommit: Map[ObjectId, Set[String]] = {

    Using.resources(new Git(repo), new RevWalk(repo)) { (git, rw) =>
      git.branchList.call.asScala.foldLeft(Map.empty[ObjectId, Set[String]]) { (branchesAcc, ref) =>
        val branchName = ref.getName.drop(Constants.R_HEADS.length)

        rw.reset()
        rw.markStart(rw.parseCommit(ref.getObjectId))
        rw.asScala.foldLeft(branchesAcc) { (thisBranchAcc, rev) =>
          val sha1 = rev.getId
          thisBranchAcc.get(sha1) match {
            case Some(set) => thisBranchAcc + (sha1 -> (set + branchName))
            case None      => thisBranchAcc + (sha1 -> Set(branchName))
          }
        }
      }
    }
  }
}
