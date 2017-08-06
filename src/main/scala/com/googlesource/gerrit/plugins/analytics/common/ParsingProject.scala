package com.googlesource.gerrit.plugins.analytics.common

import java.io.IOException

import com.google.gerrit.extensions.restapi.UnprocessableEntityException
import com.google.gerrit.server.project.{ProjectResource, ProjectsCollection}
import org.kohsuke.args4j.Argument

trait ParsingProject {
  val projects: ProjectsCollection
  var projectRes: ProjectResource = null

  @Argument(usage = "project name", metaVar = "PROJECT", required = true)
  def setProject(project: String): Unit = {
    try {
      this.projectRes = projects.parse(project)
    } catch {
      case e: UnprocessableEntityException =>
        throw new IllegalArgumentException(e.getLocalizedMessage, e)
      case e: IOException =>
        throw new IllegalArgumentException("I/O Error while trying to access project " + project, e)
    }
  }

}
