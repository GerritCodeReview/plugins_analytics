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

package com.googlesource.gerrit.plugins.analytics.test

import com.googlesource.gerrit.plugins.analytics.common.{BinaryFilesCache, CacheBoolean}

object BinaryFilesNoCache extends BinaryFilesCache {
  override def get(key: String, getterF: String => CacheBoolean): CacheBoolean = getterF(key)

  override def put(key: String, value: CacheBoolean): Unit = ()

  override def hitCount: Long = 0

  override def evictionCount: Long = 0

  override def missCount: Long = 0

  override def size: Long = 0
}
