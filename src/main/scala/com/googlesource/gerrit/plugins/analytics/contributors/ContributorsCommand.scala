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

package com.googlesource.gerrit.plugins.analytics.contributors

import com.google.gerrit.server.project.{ProjectResource, ProjectsCollection}
import com.google.gerrit.sshd.{CommandMetaData, SshCommand}
import com.google.inject.Inject
import com.googlesource.gerrit.plugins.analytics.common.{JsonFormatter, ParsingProject}

@CommandMetaData(name = "contributors", description = "Extracts the list of contributors to a project")
class ContributorsCommand @Inject() (
  val projects: ProjectsCollection,
  val executor: ContributorsExec,
  val gsonFmt: JsonFormatter) extends SshCommand with ParsingProject {

  override protected def run() {
    gsonFmt.format(executor.get(projectRes), stdout)
  }
}