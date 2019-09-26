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

package google.gerrit.server.notedb

import com.google.common.collect.ImmutableSet
import com.google.gerrit.acceptance.UseLocalDisk
import com.google.gerrit.extensions.api.GerritApi
import com.google.gerrit.extensions.api.changes.{ChangeApi, HashtagsInput}
import com.google.gerrit.extensions.common.ChangeInput
import com.google.gerrit.server.notedb.{ChangeNotesCache, ChangeNotesCommit, HashTagsExtractorImpl}
import com.googlesource.gerrit.plugins.analytics.test.GerritTestDaemon
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.scalatest.{FlatSpec, Matchers}

@UseLocalDisk
class HashTagsExtractorSpec extends FlatSpec with Matchers with GerritTestDaemon {

  def hashTagExtractor = HashTagsExtractorImpl(fileRepositoryName, testFileRepository.getRepository, GerritTestDaemon.getInstance(classOf[ChangeNotesCache]))

  behavior of "hashTagsOfCommit"

  it should "extract hashTags when they are defined in a change" in {
    def hashTagExtractor = HashTagsExtractorImpl(fileRepositoryName, testFileRepository.getRepository, GerritTestDaemon.getInstance(classOf[ChangeNotesCache]))
    val hashTag1 = "foo"
    val hashTag2 = "bar"
    val change = newChange()
    val commit = commitOfChange(change)

    change.setHashtags(new HashtagsInput(ImmutableSet.of(hashTag1, hashTag2)))
    hashTagExtractor.hashTagsOfCommit(commit) should contain only (hashTag1, hashTag2)
  }

  it should "extract empty hashTags when change note has no hashTags defined" in {
    def hashTagExtractor = HashTagsExtractorImpl(fileRepositoryName, testFileRepository.getRepository, GerritTestDaemon.getInstance(classOf[ChangeNotesCache]))
    val commit = commitOfChange(newChange())
    hashTagExtractor.hashTagsOfCommit(commit) shouldBe empty
  }

  private def newChange(): ChangeApi = {
    val changeInput = new ChangeInput(fileRepositoryName.get(), "master", "test Change")
    changeInput.newBranch = true
    GerritTestDaemon.getInstance(classOf[GerritApi]).changes().create(changeInput)
  }

  private def commitOfChange(changeApi: ChangeApi): RevCommit = {
    val commitId = changeApi.current().commit(false).commit
    new ChangeNotesCommit(ObjectId.fromString(commitId))
  }
}
