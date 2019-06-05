// Copyright (C) 2019 The Android Open Source Project
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

import com.googlesource.gerrit.plugins.analytics.test.{GerritTestDaemon, TestBinaryFilesNoCache}
import org.eclipse.jgit.treewalk.TreeWalk
import org.scalatest.{FlatSpec, Matchers}

class NonBinaryFilesFilterSpec extends FlatSpec with Matchers with GerritTestDaemon with TestBinaryFilesNoCache {

  behavior of "include"

  it should "include regular files" in {
    val aFile = "aFile"
    val commit = testFileRepository.commitFile(aFile, "some ascii content")

    val walk = TreeWalk.forPath(testFileRepository.getRepository, aFile, commit.getTree)

    NonBinaryFilesFilter(fileRepositoryName, binaryFilesNoCache).include(walk) shouldBe true
  }

  it should "not include binary files" in {
    val aFile = "aFile"
    val commit = testFileRepository.commitBinaryFile(aFile, Array.range(1, 512).map(_.toByte)) // Some binary content

    val walk = TreeWalk.forPath(testFileRepository.getRepository, aFile, commit.getTree)

    NonBinaryFilesFilter(fileRepositoryName, binaryFilesNoCache).include(walk) shouldBe false
  }
}