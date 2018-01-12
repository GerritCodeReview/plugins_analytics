package com.googlesource.gerrit.plugins.analytics.test

import com.google.gerrit.server.OutputFormat
import com.googlesource.gerrit.plugins.analytics.CommitInfo
import org.scalatest.{FlatSpec, Matchers}

class CommitInfoSpec extends FlatSpec with Matchers {

  "CommitInfo" should "be serialised as JSON correctly" in {
    val commitInfo = CommitInfo(sha1 = "sha", date = 1000l, merge = false, files = Set("file1", "file2"))

    val gsonBuilder = OutputFormat.JSON_COMPACT.newGsonBuilder
    gsonBuilder.registerTypeAdapter(classOf[CommitInfo], CommitInfo.JsonTypeAdapter)

    val actual = gsonBuilder.create().toJson(commitInfo)
    actual shouldBe "{\"sha1\":\"sha\",\"date\":1000,\"merge\":false,\"files\":[\"file1\",\"file2\"]}"
  }

}
