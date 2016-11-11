// Copyright (C) 2013 The Android Open Source Project
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

import com.google.gerrit.extensions.restapi.BinaryResult;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.stream.Stream;

class ContributorsResource implements RestReadView<ProjectResource> {
  private final GitRepositoryManager repoManager;
  private final UserSummaryExport userSummary;
  private final GsonFormatter gsonFmt;

  class JsonStreamedResult<T> extends BinaryResult {
    private final Stream<T> committers;

    public JsonStreamedResult(Stream<T> committers) {
      this.committers = committers;
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
      try (PrintWriter sout = new PrintWriter(os)) {
        gsonFmt.format(committers, sout);
      }
    }
  }

  @Inject
  public ContributorsResource(GitRepositoryManager repoManager,
      UserSummaryExport userSummary, GsonFormatter gsonFmt) {
    this.repoManager = repoManager;
    this.userSummary = userSummary;
    this.gsonFmt = gsonFmt;
  }

  @Override
  public Response<BinaryResult> apply(ProjectResource projectRes)
      throws RepositoryNotFoundException, IOException {
    return Response.ok(new JsonStreamedResult<>(getStream(projectRes)));
  }

  public Stream<UserActivitySummary> getStream(ProjectResource projectRes)
      throws RepositoryNotFoundException, IOException {
    try (Repository repo = repoManager.openRepository(projectRes.getNameKey())) {
      return userSummary.getCommittersStream(repo);
    }
  }
}
