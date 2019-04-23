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

import com.googlesource.gerrit.plugins.analytics.test.GitTestCase
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.treewalk.TreeWalk
import org.scalatest.{FlatSpec, Matchers}

class NonBinaryFileFilterSpec extends FlatSpec with Matchers with GitTestCase {

  case object NoCache extends CacheApi[String, Boolean] {
    override def getOrUpdate(el: String, f: => String => Boolean): Boolean = f(el)
  }

  behavior of "include"

  it should "include regular files" in {
    val aFile = "aFile"
    val commit = add(aFile, "some ascii content")

    val walk = TreeWalk.forPath(new FileRepository(testRepo), aFile, commit.getTree)

    NonBinaryFileFilter(NoCache).include(walk) shouldBe true
  }

  it should "not include binary files" in {
    val aFile = "aFile"
    val commit = add(aFile, Array.range(1, 512).map(_.toByte)) // Some binary content

    val walk = TreeWalk.forPath(new FileRepository(testRepo), aFile, commit.getTree)

    NonBinaryFileFilter(NoCache).include(walk) shouldBe false
  }
}