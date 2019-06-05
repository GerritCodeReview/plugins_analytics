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

import org.eclipse.jgit.lib.ObjectId
import org.scalatest.{FlatSpec, Matchers}

class BinaryFilesCacheKeySerializerSpec extends FlatSpec with Matchers {

  "BinaryFilesCacheKeySerializer" should "serialize and deserialize a BinaryFilesCacheKey" in {
    val originalCacheKey = BinaryFilesCacheKey(
      projectName="aProject",
      objectId=ObjectId.fromString("2c6a8d095a23e061249858fc5c907c2e377cd59f"),
      filePath="a/file/path"
    )

    val deSerializedCacheKey = BinaryFilesCacheKeySerializer.deserialize(
      BinaryFilesCacheKeySerializer.serialize(originalCacheKey)
    )

    deSerializedCacheKey shouldBe originalCacheKey
  }
}