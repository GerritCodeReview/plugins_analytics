package com.googlesource.gerrit.plugins.analytics.test

import com.googlesource.gerrit.plugins.analytics.CommitInfo
import com.googlesource.gerrit.plugins.analytics.common.GsonFormatter
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class CommitInfoSpec extends AnyFlatSpecLike with Matchers {

  "CommitInfo" should "be serialised as JSON correctly" in {
    val commitInfo = CommitInfo(sha1 = "sha", date = 1000L, merge = false, botLike = false, files = Set("file1", "file2"))

    val gsonBuilder = new GsonFormatter().gsonBuilder

    val actual = gsonBuilder.create().toJson(commitInfo)
    List(actual) should contain oneOf(
      "{\"sha1\":\"sha\",\"date\":1000,\"merge\":false,\"bot_like\":false,\"files\":[\"file1\",\"file2\"]}",
      "{\"sha1\":\"sha\",\"date\":1000,\"merge\":false,\"bot_like\":false,\"files\":[\"file2\",\"file1\"]}"
    )
  }

}
