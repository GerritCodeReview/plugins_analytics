package com.googlesource.gerrit.plugins.analytics.test

import com.google.common.collect.Sets.newHashSet
import com.google.common.collect.{Lists, Sets}
import com.google.gerrit.server.OutputFormat
import com.googlesource.gerrit.plugins.analytics.CommitInfo
import org.scalatest.{FlatSpec, Matchers}

class CommitInfoSpec extends FlatSpec with Matchers {

  "CommitInfo" should "be serialised as JSON correctly" in {
    val commitInfo = CommitInfo(sha1 = "sha", date = 1000l, merge = false, files = newHashSet("file1", "file2"))

    val gsonBuilder = OutputFormat.JSON_COMPACT.newGsonBuilder

    val actual = gsonBuilder.create().toJson(commitInfo)
    List(actual) should contain oneOf(
      "{\"sha1\":\"sha\",\"date\":1000,\"merge\":false,\"files\":[\"file1\",\"file2\"]}",
      "{\"sha1\":\"sha\",\"date\":1000,\"merge\":false,\"files\":[\"file2\",\"file1\"]}"
    )
  }

}
