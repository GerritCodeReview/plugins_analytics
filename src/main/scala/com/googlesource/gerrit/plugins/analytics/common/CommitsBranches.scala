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
import org.eclipse.jgit.lib.{ObjectId, Repository}
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter

import scala.collection.JavaConversions._
import scala.collection.mutable

class CommitsBranches(repo: Repository, from: Option[Long] = None,
                      to: Option[Long] = None) {
  private val refs = new Git(repo).branchList.call
  private val allCommits = mutable.HashMap[String, Set[String]]()
    .withDefaultValue(Set.empty[String])
  for (ref <- refs) {
    def stripRefsHeads(refName: String) = refName.split("/").drop(2).mkString("/")

    val branchRealName = stripRefsHeads(ref.getName)
    use(new RevWalk(repo)) { rw: RevWalk =>
      // if we have date filtering apply it to reduce memory impact
      from.foreach(d1 => rw.setRevFilter(CommitTimeRevFilter.after(d1)))
      to.foreach(d2 => rw.setRevFilter(CommitTimeRevFilter.after(d2)))
      rw.markStart(rw.parseCommit(ref.getObjectId))
      rw.foreach { rev =>
        val sha1 = rev.getName
        allCommits(sha1) = allCommits(sha1) + branchRealName
      }
    }
  }

  def find(objectIds: Seq[ObjectId]): Set[String] =
    objectIds.foldLeft(Set.empty[String]) {
      (acc, objectId) => {
        acc ++ allCommits(objectId.getName)
      }
    }
}