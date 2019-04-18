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

import java.util

/*
*
* This helper class allows to conversion of some collections from scala to java and vice-versa in such a way
* that the result is serializable.
* scala.collection.JavaConversions and java.collection.JavaConverters work by wrapping the underlying collection
* into a scala.collection.convert.Wrappers, which is not serializable.
* This is a known issue in scala 2.11: https://github.com/scala/bug/issues/8911
*
*/
object SerializableScalaConversions {

    def toScala[T](javaSet: util.Set[T]): Set[T] = {
      var scalaSet: Set[T] = Set.empty
      val itr = javaSet.iterator()

      while (itr.hasNext) {
        scalaSet = scalaSet + itr.next()
      }
      scalaSet
    }

    def toScala[T](javaList: util.List[T]): List[T] = {
      var scalaList: List[T] = List.empty

      val itr = javaList.iterator()

      while (itr.hasNext) {
        scalaList = scalaList :+ itr.next()
      }
      scalaList
    }

  def toJava[T](scalaSet: Set[T]): util.Set[T] = {
    val javaSet = new java.util.HashSet[T]()

    scalaSet.foreach(javaSet.add)
    javaSet
  }

}
