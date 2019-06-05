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
import java.nio.charset.StandardCharsets

import com.google.gerrit.server.cache.serialize.{CacheSerializer, ObjectIdCacheSerializer}
import org.eclipse.jgit.lib.{Constants, ObjectId}
import org.slf4j.LoggerFactory

object BinaryFilesCacheBooleanSerializer extends CacheSerializer[CacheBoolean] {

  private val log = LoggerFactory.getLogger("BinaryFilesCacheSerializer")

  override def serialize(`object`: CacheBoolean): Array[Byte] = {
    val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(stream)
    oos.writeObject(`object`)
    oos.close()
    stream.toByteArray
  }

  override def deserialize(in: Array[Byte]): CacheBoolean = {
    val ois = new ObjectInputStream(new ByteArrayInputStream(in))
    val value = ois.readObject.asInstanceOf[CacheBoolean]
    ois.close()
    value
  }
}

object BinaryFilesCacheKeySerializer extends CacheSerializer[BinaryFilesCacheKey] {
  private val separator = '|'

  override def serialize(obj: BinaryFilesCacheKey): Array[Byte] = {
    val objectIdBin = ObjectIdCacheSerializer.INSTANCE.serialize(obj.objectId)
    val projectAndFilePathBin = Seq(obj.projectName, obj.filePath).mkString(separator.toString).getBytes(StandardCharsets.UTF_8)
    objectIdBin ++ projectAndFilePathBin
  }

  override def deserialize(in: Array[Byte]): BinaryFilesCacheKey = {
    val objectIdBin = in.take(Constants.OBJECT_ID_LENGTH)
    val projectAndFilePathBin = in.drop(Constants.OBJECT_ID_LENGTH)
    val projectAndFilePath = new String(projectAndFilePathBin, StandardCharsets.UTF_8)
    val Array(project, filePath) = projectAndFilePath.split(separator).take(2)
    val objectId = ObjectId.fromRaw(objectIdBin)

    BinaryFilesCacheKey(project, objectId, filePath)
  }
}
