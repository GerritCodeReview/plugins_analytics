// Copyright (C) 2017 The Android Open Source Project
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

import com.google.gerrit.acceptance.UseLocalDisk
import com.googlesource.gerrit.plugins.analytics.common.AggregationStrategy.EMAIL_YEAR
import com.googlesource.gerrit.plugins.analytics.test.GerritTestDaemon
import org.scalatest.{FlatSpec, Matchers}

@UseLocalDisk
class UserActivityHistogramSpec extends FlatSpec with Matchers with GerritTestDaemon {

  "UserActivityHistogram" should "return no activities" in {
    val filter = new AggregatedHistogramFilterByDates(aggregationStrategy = EMAIL_YEAR)
    new UserActivityHistogram().get(testFileRepository.getRepository, filter) should have size 0
  }

  it should "aggregate to one activity" in {
    testFileRepository.commitFile("test.txt", "content")
    val filter = new AggregatedHistogramFilterByDates(aggregationStrategy = EMAIL_YEAR)
    new UserActivityHistogram().get(testFileRepository.getRepository, filter) should have size 1
  }

  it should "filter by branch" in {
    val branch = "testBranch"
    testFileRepository.commitFile("test.txt", "content", branch=branch)

    val filter = new AggregatedHistogramFilterByDates(aggregationStrategy = EMAIL_YEAR)

    new UserActivityHistogram().get(testFileRepository.getRepository, filter) should have size 0
    new UserActivityHistogram().get(testFileRepository.getRepository, filter, branch) should have size 1
  }

}
