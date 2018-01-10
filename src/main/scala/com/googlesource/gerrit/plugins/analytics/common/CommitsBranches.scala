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
import org.eclipse.jgit.lib.{Constants, ObjectId, Repository}
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter

import scala.collection.JavaConversions._
import scala.collection.mutable

class CommitsBranches(repo: Repository, from: Option[Long] = None,
                      to: Option[Long] = None) {
  def find(objectIds: Seq[ObjectId]): Set[String] = {
    val commitToBranchesMap = mutable.HashMap[String, Set[String]]()
      .withDefaultValue(Set.empty[String])

    def loadTable() {
      val refs = new Git(repo).branchList.call
      for (ref <- refs) {
        def stripRefsHeads(refName: String) = refName.drop(Constants.R_HEADS.length)

        val branchName = stripRefsHeads(ref.getName)
        use(new RevWalk(repo)) { rw: RevWalk =>
          // if we have date filtering apply it to reduce memory impact
          from.foreach(d1 => rw.setRevFilter(CommitTimeRevFilter.after(d1)))
          to.foreach(d2 => rw.setRevFilter(CommitTimeRevFilter.before(d2)))
          rw.markStart(rw.parseCommit(ref.getObjectId))
          rw.foreach { rev =>
            val sha1 = rev.getName
            commitToBranchesMap(sha1) = commitToBranchesMap(sha1) + branchName
          }
        }
      }
    }

    loadTable()
    objectIds.foldLeft(Set.empty[String]) {
      (acc, objectId) => {
        acc ++ commitToBranchesMap(objectId.getName)
      }
    }
  }
}
