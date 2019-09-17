// Copyright (C) 2019 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.analytics.test

import java.lang.reflect.Type

import com.google.gerrit.acceptance.UseLocalDisk
import com.google.gson._
import com.googlesource.gerrit.plugins.analytics.UserActivitySummary
import com.googlesource.gerrit.plugins.analytics.common.AggregationStrategy.EMAIL_HOUR
import com.googlesource.gerrit.plugins.analytics.common.GsonFormatter
import com.googlesource.gerrit.plugins.analytics.test.TestAnalyticsConfig.IGNORED_FILE_SUFFIX
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.collection.JavaConverters._

@UseLocalDisk
class ContributorsServiceSpec extends FlatSpec with Matchers with GerritTestDaemon with Inside {

  "ContributorsService" should "get commit statistics" in {
    val aContributorName = "Contributor Name"
    val aContributorEmail = "contributor@test.com"
    val aFileName = "file.txt"
    val anIgnoredFileName = s"file$IGNORED_FILE_SUFFIX"

    val commit = testFileRepository.commitFiles(
      List(anIgnoredFileName -> "1\n2\n", aFileName -> "1\n2\n"),
      newPersonIdent(aContributorName, aContributorEmail)
    )

    val statsJson = daemonTest.restSession.get(s"/projects/${fileRepositoryName.get()}/analytics~contributors?aggregate=${EMAIL_HOUR.name}")

    statsJson.assertOK()

    val stats = TestGson().fromJson(statsJson.getEntityContent, classOf[UserActivitySummary])

    inside(stats) {
      case UserActivitySummary(_, _, _, _, theAuthorName, theAuthorEmail, numCommits, numFiles, numDistinctFiles, addedLines, deletedLines, commits, _, _, _, _, _, _) =>
        theAuthorName shouldBe aContributorName
        theAuthorEmail shouldBe aContributorEmail
        numCommits shouldBe 1
        numFiles shouldBe 1
        numDistinctFiles shouldBe 1
        addedLines shouldBe 2
        deletedLines shouldBe 0
        commits.head.files should contain only aFileName
        commits.head.sha1 shouldBe commit.name
    }
  }
}

object TestGson {

  class SetStringDeserializer extends JsonDeserializer[Set[String]] {
    override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Set[String] =
      json.getAsJsonArray.asScala.map(_.getAsString).toSet
  }

  class OptionDeserializer extends JsonDeserializer[Option[Any]] {
    override def deserialize(jsonElement: JsonElement, `type`: Type, jsonDeserializationContext: JsonDeserializationContext): Option[Any] = {
      Some(jsonElement)
    }
  }

  def apply(): Gson =
    new GsonFormatter()
      .gsonBuilder
      .registerTypeHierarchyAdapter(classOf[Iterable[String]], new SetStringDeserializer)
      .registerTypeHierarchyAdapter(classOf[Option[Any]], new OptionDeserializer())
      .create()
}