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

import com.google.gerrit.acceptance.UseLocalDisk
import com.googlesource.gerrit.plugins.analytics.AnalyticsConfig
import com.googlesource.gerrit.plugins.analytics.test.GerritTestDaemon
import org.eclipse.jgit.treewalk.TreeWalk
import org.scalatest.{FlatSpec, Matchers}

@UseLocalDisk
class IgnoreFileSuffixFilterSpec extends FlatSpec with Matchers with GerritTestDaemon {

  behavior of "IgnoreFileSuffixFilter"

  it should "include a file with suffix not listed in configuration" in {
    val ignoreSuffix = ".dmg"
    val fileSuffix = ".txt"
    val aFile = s"aFile$fileSuffix"
    val commit = testFileRepository.commitFile(aFile, "some content")

    val walk = TreeWalk.forPath(testFileRepository.getRepository, aFile, commit.getTree)

    newIgnoreFileSuffix(ignoreSuffix).include(walk) shouldBe true
  }

  it should "not include a file with suffix listed in configuration" in {
    val ignoreSuffix = ".dmg"
    val aFile = s"aFile$ignoreSuffix"
    val commit = testFileRepository.commitFile(aFile, "some content")

    val walk = TreeWalk.forPath(testFileRepository.getRepository, aFile, commit.getTree)

    newIgnoreFileSuffix(ignoreSuffix).include(walk) shouldBe false
  }

  private def newIgnoreFileSuffix(suffixes: String*) = IgnoreFileSuffixFilter(new AnalyticsConfig {
    override lazy val botlikeFilenameRegexps = List.empty
    override lazy val isExtractIssues: Boolean = false
    override def ignoreFileSuffixes: List[String] = suffixes.toList
  })
}
