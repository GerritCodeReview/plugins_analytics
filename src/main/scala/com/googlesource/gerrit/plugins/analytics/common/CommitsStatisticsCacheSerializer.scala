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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import com.google.gerrit.server.cache.serialize.CacheSerializer

object CommitsStatisticsCacheSerializer extends CacheSerializer[CommitsStatistics] {
  override def serialize(`object`: CommitsStatistics): Array[Byte] = {
    val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(stream)
    oos.writeObject(`object`)
    oos.close()
    stream.toByteArray
  }

  override def deserialize(in: Array[Byte]): CommitsStatistics = {
    val ois = new ObjectInputStream(new ByteArrayInputStream(in))
    val value = ois.readObject.asInstanceOf[CommitsStatistics]
    ois.close()
    value
  }
}
