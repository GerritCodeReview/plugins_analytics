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

package com.googlesource.gerrit.plugins.analytics.test

import org.scalatest.{FlatSpec, Inside, Matchers}
import Timer.time

class TempSpec extends FlatSpec with Matchers with
  Inside {

  case class Commit(commit: String,
                    parents: Set[String])

  def getBranchesFromCommit(repository: Map[String, Commit],
                            branchHeads: Map[String, String],
                            commit: String): Set[String] = {
    def checkRecursiveCommit(branch: String, branchChainElement: Commit)
    : Set[String]
    = {
      if (branchChainElement.commit == commit) {
        println(s"found element $commit")
        Set(branch)
      } else {
        branchChainElement.parents.flatMap {
          parent => {
            println(s"checking parent $parent")
            val ret = checkRecursiveCommit(branch, repository(parent))
            println(s"returning $ret")
            ret
          }
        }
      }
    }

    branchHeads.flatMap { case (name, head) => checkRecursiveCommit(name,
      repository(head))
    }.toSet

  }


  "aaaa" should "find branches for a commit" in {
    val repo: Map[String, Commit] = Map(
      "c1" -> Commit("c1", Set.empty),
      "c2" -> Commit("c2", Set("c1", "c3")),
      "c3" -> Commit("c3", Set.empty)
    )
    val branchHeads = Map("master" -> "c2")
    // "branch" -> "c3")
//    val branches = getBranchesFromCommit(repo, branchHeads, "c1")
//    branches should have size(1)
//    branches should contain ("master")
    val branchHeads2 = branchHeads + ( "branch" -> "c3")
    val branches2 = getBranchesFromCommit(repo, branchHeads2, "c1")
    branches2 should have size(2)
    branches2 should contain allOf( "master", "branch")

  }
  //  it should "find branches for a commit with deep history" in {
  //    val repo: Map[String, Commit] = Map(
  //      "c1" -> Commit(None, "c1", Set("c2", "c3")),
  //      "c2" -> Commit(None, "c2", Set("c4")),
  //      "c3" -> Commit(Some("label1"), "c3", Set.empty),
  //      "c4" -> Commit(None, "c4", Set("c5")),
  //      "c5" -> Commit(Some("label2"), "c5", Set.empty),
  //      "c6" -> Commit(None, "c6", Set.empty)
  //    )
  //    getLabelsForCommit(repo, "c1") should contain allOf(
  //      "label1", "label2"
  //    )
  //    getLabelsForCommit(repo, "c6") should have size (0)
  //
  //  }
}
