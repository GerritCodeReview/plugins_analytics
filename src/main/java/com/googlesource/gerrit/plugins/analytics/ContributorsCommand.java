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

package com.googlesource.gerrit.plugins.analytics;

import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.server.project.ProjectResource;
import com.google.gerrit.server.project.ProjectsCollection;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;

import org.kohsuke.args4j.Argument;

import java.io.IOException;

@CommandMetaData(name = "contributors", description = "Extracts the list of contributors to a project")
public class ContributorsCommand extends SshCommand {
  private final ProjectsCollection projects;
  private final ContributorsResource contributors;
  private final GsonFormatter gsonFmt;

  @Inject
  public ContributorsCommand(ProjectsCollection projects,
      ContributorsResource contributors, GsonFormatter gsonFmt) {
    this.projects = projects;
    this.contributors = contributors;
    this.gsonFmt = gsonFmt;
  }

  @Argument(usage = "project name", metaVar = "PROJECT", required = true)
  void setProject(String project) throws IllegalArgumentException {
    try {
      this.projectRes = projects.parse(project);
    } catch (UnprocessableEntityException e) {
      throw new IllegalArgumentException(e.getLocalizedMessage(), e);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "I/O Error while trying to access project " + project, e);
    }
  }

  private ProjectResource projectRes;

  @Override
  protected void run() throws UnloggedFailure, Failure, Exception {
    gsonFmt.format(contributors.getStream(projectRes), stdout);
  }
}
