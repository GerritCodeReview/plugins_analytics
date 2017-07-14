/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlesource.gerrit.plugins.analytics

import java.io.IOException

import com.google.gerrit.extensions.restapi.UnprocessableEntityException
import com.google.gerrit.server.project.{ProjectResource, ProjectsCollection}
import com.google.gerrit.sshd.{CommandMetaData, SshCommand}
import com.google.inject.Inject
import org.kohsuke.args4j.Argument

@CommandMetaData(name = "contributors", description = "Extracts the list of contributors to a project")
class ContributorsCommand @Inject() (
  val projects: ProjectsCollection,
  val contributors: ContributorsResource,
  val gsonFmt: GsonFormatter) extends SshCommand {

  @Argument(usage = "project name", metaVar = "PROJECT", required = true)
  private[analytics] def setProject(project: String): Unit = {
    try {
      this.projectRes = projects.parse(project)
    } catch {
      case e: UnprocessableEntityException =>
        throw new IllegalArgumentException(e.getLocalizedMessage, e)
      case e: IOException =>
        throw new IllegalArgumentException("I/O Error while trying to access project " + project, e)
    }
  }

  private var projectRes: ProjectResource = _
  override protected def run(): Unit = {
    gsonFmt.format(contributors.getUsersActivities(projectRes), stdout)
  }
}