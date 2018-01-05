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

class TempSpec extends FlatSpec with GeneratedRepository with Matchers with
  Inside {
  private def getLabelsForCommit(repository: Map[String, Commit], commitName: String)
  : Set[String] = {
    repository.get(commitName).map(commit =>
      commit.label.fold(
        // in case label is empty we need to recursively find parents
        commit.parents.flatMap(
          parentName => getLabelsForCommit(repository, parentName))
      )(
        // if label is provided we just use this one
        label => Set[String](label))).
      getOrElse(Set.empty)
  }

  "enrich" should "take not too much time" in {
    time("enriching table") {
      // par allows to use all 8 core processors so for 1 M we pass from 82
      // seconds to around 15 seconds (!)
      repository.values.par.map {
        c => {
          getLabelsForCommit(repository, c.commit)
        }
      }
    }
  }

  it should "find branches for a commit" in {
    val repo: Map[String, Commit] = Map(
      "c1" -> Commit(None, "c1", Set("c2", "c3")),
      "c2" -> Commit(Some("label"), "c2", Set.empty),
      "c3" -> Commit(Some("label2"), "c3", Set.empty)
    )
    getLabelsForCommit(repo, "c1") should contain allOf(
      "label", "label2"
    )

  }
  it should "find branches for a commit with deep history" in {
    val repo: Map[String, Commit] = Map(
      "c1" -> Commit(None, "c1", Set("c2", "c3")),
      "c2" -> Commit(None, "c2", Set("c4")),
      "c3" -> Commit(Some("label1"), "c3", Set.empty),
      "c4" -> Commit(None, "c4", Set("c5")),
      "c5" -> Commit(Some("label2"), "c5", Set.empty),
      "c6" -> Commit(None, "c6", Set.empty)
    )
    getLabelsForCommit(repo, "c1") should contain allOf(
      "label1", "label2"
    )
    getLabelsForCommit(repo, "c6") should have size (0)

  }
}
