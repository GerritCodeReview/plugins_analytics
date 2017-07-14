// Copyright (C) 2016 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.analytics

object AutoManageResources {
  implicit class ImplicitManagedResource[A <: AutoCloseable](resource: => A)
    extends ManagedResource(resource)
}

class ManagedResource[A <: AutoCloseable](r: => A) {
  def map[B](f: (A) => B): B = {
    val resource = r
    var currentException: Throwable = null
    try {
      f(resource)
    } catch {
      case e: Throwable =>
        currentException = e
        throw e
    } finally {
      if (resource != null) {
        if (currentException != null) {
          try {
            resource.close()
          } catch {
            case e: Throwable =>
              currentException.addSuppressed(e)
          }
        } else {
          resource.close()
        }
      }
    }
  }
  def flatMap[B](f: (A) => B): B = map(f)
  def foreach[U](f: (A) => U): Unit = map(f)
}
