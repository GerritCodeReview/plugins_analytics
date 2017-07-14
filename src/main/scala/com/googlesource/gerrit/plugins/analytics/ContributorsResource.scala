// Copyright (C) 2013 The Android Open Source Project
// Copyright (C) 2013 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
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

import java.io.{OutputStream, PrintWriter}

import com.google.gerrit.extensions.restapi.{BinaryResult, Response, RestReadView}
import com.google.gerrit.server.git.GitRepositoryManager
import com.google.gerrit.server.project.ProjectResource
import com.google.inject.Inject
import org.eclipse.jgit.lib.Repository

class ContributorsResource @Inject()
(
  val repoManager: GitRepositoryManager,
  val userSummary: UserSummaryExport,
  val gsonFmt: GsonFormatter) extends RestReadView[ProjectResource] {

  private[analytics] class JsonStreamedResult[T](val committers: List[T]) extends BinaryResult {

    override def writeTo(os: OutputStream) = {
      val sout = new PrintWriter(os)
      try gsonFmt.format(committers, sout)
      finally if (sout != null) sout.close()
    }
  }

  def apply(projectRes: ProjectResource): Response[BinaryResult] = {
    return Response.ok(new JsonStreamedResult[UserActivitySummary]
    (get(projectRes)))
  }


  def get(projectRes: ProjectResource): List[UserActivitySummary] = {
    val repo: Repository = repoManager.openRepository(projectRes.getNameKey)
    try {
      return userSummary.getCommitters(repo)
    } finally {
      if (repo != null) repo.close()
    }
  }

}