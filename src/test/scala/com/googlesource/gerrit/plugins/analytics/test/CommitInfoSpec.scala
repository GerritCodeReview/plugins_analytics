package com.googlesource.gerrit.plugins.analytics.test

import com.googlesource.gerrit.plugins.analytics.CommitInfo
import com.googlesource.gerrit.plugins.analytics.common.GsonFormatter
import org.scalatest.{FlatSpec, Matchers}

class CommitInfoSpec extends FlatSpec with Matchers {

  "CommitInfo" should "be serialised as JSON correctly" in {
    val commitInfo = CommitInfo(sha1 = "sha", date = 1000l, merge = false, botLike = false, files = Set("file1", "file2"))

    val gsonBuilder = new GsonFormatter().gsonBuilder

    val actual = gsonBuilder.create().toJson(commitInfo)
    List(actual) should contain oneOf(
      "{\"sha1\":\"sha\",\"date\":1000,\"merge\":false,\"bot_like\":false,\"files\":[\"file1\",\"file2\"],\"hash_tags\":[]}",
      "{\"sha1\":\"sha\",\"date\":1000,\"merge\":false,\"bot_like\":false,\"files\":[\"file2\",\"file1\"],\"hash_tags\":[]}"
    )
  }

}
