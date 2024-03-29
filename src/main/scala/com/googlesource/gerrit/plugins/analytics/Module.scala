// Copyright (C) 2017 The Android Open Source Project
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

import com.google.gerrit.extensions.restapi.RestApiModule
import com.google.gerrit.server.project.ProjectResource.PROJECT_KIND
import com.google.inject.AbstractModule
import com.googlesource.gerrit.plugins.analytics.common.{BotLikeExtractor, BotLikeExtractorImpl}
import com.googlesource.gerrit.plugins.analytics.common.CommitsStatisticsCacheModule

class Module extends AbstractModule {

  override protected def configure() = {
    bind(classOf[BotLikeExtractor]).to(classOf[BotLikeExtractorImpl])

    install(new CommitsStatisticsCacheModule())
    install(new RestApiModule() {
      override protected def configure() = {
        get(PROJECT_KIND, "contributors").to(classOf[ContributorsResource])
      }
    })
  }
}
