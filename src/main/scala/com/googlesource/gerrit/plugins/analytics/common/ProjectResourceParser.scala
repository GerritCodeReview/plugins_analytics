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
