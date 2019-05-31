// Copyright (C) 2019 GerritForge Ltd
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

import com.googlesource.gerrit.plugins.analytics.AnalyticsConfig
import com.googlesource.gerrit.plugins.analytics.test.GerritTestDaemon
import org.scalatest.{FlatSpec, Matchers}

class BotLikeExtractorImplSpec extends FlatSpec with Matchers with GerritTestDaemon {

  behavior of "isBotLike"

  it should "return true when all files match bot-like identifiers" in {
    val extractor = newBotLikeExtractorImpl(List(""".+\.xml"""))

    extractor.isBotLike(Set(
      "some/path/AFile.xml",
      "some/path/AnotherFile.xml"
    )).shouldBe(true)
  }

  it should "return false when at least one file does not match bot-like identifiers" in {
    val extractor = newBotLikeExtractorImpl(List(""".+\.xml"""))

    extractor.isBotLike(Set(
      "some/path/AFile.xml",
      "some/path/AnotherFile.someExtension"
    )).shouldBe(false)
  }

  it should "return false when no bot-like identifiers have been provided" in {
    val extractor = newBotLikeExtractorImpl(List.empty)

    extractor.isBotLike(Set("some/path/anyFile")).shouldBe(false)
  }

  private def newBotLikeExtractorImpl(botLikeRegexps: List[String]) = new BotLikeExtractorImpl(new AnalyticsConfig(null, null) {
    override lazy val botlikeFilenameRegexps = botLikeRegexps
  })
}
