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

package com.googlesource.gerrit.plugins.analytics.common

import java.io.IOException

import com.google.gerrit.extensions.restapi.UnprocessableEntityException
import com.google.gerrit.server.project.{ProjectResource, ProjectsCollection}
import org.kohsuke.args4j.Argument

trait ProjectResourceParser {
  def projects: ProjectsCollection

  var projectRes: ProjectResource = null

  @Argument(usage = "project name", metaVar = "PROJECT", required = true)
  def setProject(project: String): Unit = {
    try {
      this.projectRes = projects.parse(project)
    } catch {
      case e: Exception =>
        throw new IllegalArgumentException("Error while trying to access project " + project, e)
    }
  }
}
