// Copyright (C) 2018 GerritForge Ltd
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

import com.googlesource.gerrit.plugins.analytics.test.GitTestCase
import org.scalatest.{FlatSpec, Matchers}

class BotLikeExtractorImplSpec extends FlatSpec with Matchers with GitTestCase {

  behavior of "isBotLike"

  it should "return true when all files match bot-like identifiers" in {
    val extractor = new BotLikeExtractorImpl(List(""".+\.xml""", """.+\.bzl"""))

    extractor.isBotLike(Set(
      "tools/maven/gerrit-acceptance-framework_pom.xml",
      "tools/maven/gerrit-extension-api_pom.xml",
      "tools/maven/gerrit-plugin-api_pom.xml",
      "tools/maven/gerrit-plugin-gwtui_pom.xml",
      "tools/maven/gerrit-war_pom.xml",
      "version.bzl"
    )).shouldBe(true)

  }

  it should "return false when some of the files match bot-like identifiers" in {
    val extractor = new BotLikeExtractorImpl(List(""".+\.xml""", """.+\.bzl"""))

    extractor.isBotLike(Set(
      "tools/maven/gerrit-extension-api_pom.xml",
      "tools/maven/gerrit-plugin-api_pom.xml",
      "tools/maven/gerrit-plugin-gwtui_pom.xml",
      "tools/maven/gerrit-war_pom.xml",
      "javatests/com/google/gerrit/acceptance/api/change/PrivateChangeIT.java",
      "version.bzl"
    )).shouldBe(false)

  }

}
