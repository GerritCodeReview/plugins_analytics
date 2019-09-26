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
import com.google.gerrit.extensions.api.changes.HashtagsInput
import com.google.gerrit.extensions.common.ChangeInput
import com.google.gerrit.server.notedb.{ChangeNotesCache, ChangeNotesCommit, HashTagsExtractor}
import com.googlesource.gerrit.plugins.analytics.test.GerritTestDaemon
import org.eclipse.jgit.lib.ObjectId
import org.scalatest.{FlatSpec, Matchers}

@UseLocalDisk
class HashTagsExtractorSpec extends FlatSpec with Matchers with GerritTestDaemon {

  behavior of "tagsOfCommit"

  lazy val changeNotesCache: ChangeNotesCache = GerritTestDaemon.getInstance(classOf[ChangeNotesCache])
  lazy val gApi: GerritApi = GerritTestDaemon.getInstance(classOf[GerritApi])

  it should "extract hashtags when they are available" in {
    val hashTag = "foo"
    def htExtractor = HashTagsExtractor(fileRepositoryName, testFileRepository.getRepository, changeNotesCache)

    val changeInput = new ChangeInput(fileRepositoryName.get(), "master", "test Change")
    changeInput.newBranch = true

    val change = gApi.changes().create(changeInput)
    val commitId = change.current().commit(false).commit
    val commit = new ChangeNotesCommit(ObjectId.fromString(commitId))

    change.setHashtags(new HashtagsInput(ImmutableSet.of(hashTag)))
    htExtractor.tagsOfCommit(commit) should contain only hashTag
  }

}
