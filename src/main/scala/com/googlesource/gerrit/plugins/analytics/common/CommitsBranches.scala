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

import scala.collection.JavaConversions._
import scala.collection.mutable

class CommitsBranches(repo: Repository) {
  private val labeledCommits = getAllCommitsLabeledWithBranches

  def find(objectIds: Seq[ObjectId]): Set[String] =
    objectIds.foldLeft(Set.empty[String]) {
      (acc, objectId) => {
        acc ++ labeledCommits(objectId)
      }
    }
  // this can be useful if we want to store an index in the labeled map
  // instead of the full name to save memory
  def getBranches: Seq[String] = {
    repo.getAllRefs.entrySet.toList
      .filter(_.getKey.startsWith(Constants.R_HEADS))
      .map(_.getKey)
  }

  def getAllCommitsLabeledWithBranches: mutable.Map[ObjectId, Set[String]] = {
    val branchesMap = getBranches
    val allCommits = mutable.HashMap[ObjectId, Set[String]]().withDefaultValue(Set.empty[String])
    branchesMap.foreach {
      branchName =>
        // this is one of the most efficient ways to scan over all commits
        // contained in a branch giving its name
        val commits = new Git(repo).log.add(repo.resolve(branchName)).call
        val strippedBranchName = branchName.split("/").last
        for (rev <- commits) {
          val obj = rev.getId
          allCommits(obj) = allCommits(obj) + strippedBranchName
        }
    }
    allCommits
  }
}
