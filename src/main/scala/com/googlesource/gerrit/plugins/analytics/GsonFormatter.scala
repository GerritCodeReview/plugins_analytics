// Copyright (C) 2016 The Android Open Source Project
// Copyright (C) 2016 The Android Open Source Project
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
package com.googlesource.gerrit.plugins.analytics

import java.io.PrintWriter

import com.google.gerrit.server.OutputFormat
import com.google.gson.Gson
import com.google.inject.Singleton

@Singleton class GsonFormatter {
  val gsonBuilder = OutputFormat.JSON_COMPACT.newGsonBuilder

  def format[T](values: List[T], out: PrintWriter): Unit = {
    val gson: Gson = gsonBuilder.create
    values.foreach((value: T) => {
      def foo(value: T) = {
        gson.toJson(value, out)
        out.println()
      }
      foo(value)
    })
  }
}
