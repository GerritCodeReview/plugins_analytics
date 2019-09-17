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

import java.io.PrintWriter
import java.lang.reflect.Type

import com.google.gerrit.json.OutputFormat
import com.google.gson.{Gson, GsonBuilder, JsonSerializer, _}
import com.google.inject.Singleton

@Singleton
class GsonFormatter {
  val gsonBuilder: GsonBuilder =
    OutputFormat.JSON_COMPACT.newGsonBuilder
      .registerTypeHierarchyAdapter(classOf[Iterable[Any]], new IterableSerializer)
      .registerTypeHierarchyAdapter(classOf[Option[Any]], new OptionSerializer())

  def format[T](values: TraversableOnce[T], out: PrintWriter) {
    val gson: Gson = gsonBuilder.create

    for (value <- values) {
      gson.toJson(value, out)
      out.println()
    }
  }

  class IterableSerializer extends JsonSerializer[Iterable[Any]] {
    override def serialize(src: Iterable[Any], typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
      import scala.collection.JavaConverters._
      context.serialize(src.asJava)
    }
  }

  class OptionSerializer extends JsonSerializer[Option[Any]] {
    def serialize(src: Option[Any], typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
      src match {
        case None => JsonNull.INSTANCE
        case Some(v) => context.serialize(v)
      }
    }
  }
}
