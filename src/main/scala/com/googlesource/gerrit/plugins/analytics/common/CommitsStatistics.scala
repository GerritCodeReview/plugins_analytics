// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.analytics.common

import org.eclipse.jgit.diff.{DiffFormatter, RawTextComparator}
import org.eclipse.jgit.lib.{ObjectId, Repository}
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.{CanonicalTreeParser, EmptyTreeIterator}
import org.eclipse.jgit.util.io.DisabledOutputStream

import scala.collection.JavaConversions._

case class CommitsStatistics(numFiles: Int, addedLines: Int, deletedLines: Int)

class Statistics(repo: Repository) {

  def find(objectIds: Seq[ObjectId]): CommitsStatistics =
    objectIds.foldLeft(CommitsStatistics(0, 0, 0)) {
      (acc, objectId) => {
        val stats = find(objectId)
        CommitsStatistics(acc.numFiles + stats.numFiles, acc.addedLines + stats.addedLines, acc.deletedLines + stats.deletedLines)
      }
    }

  def find(objectId: ObjectId): CommitsStatistics = {
    // I can imagine this kind of statistics is already being available in Gerrit but couldn't understand how to access it
    // which Injection can be useful for this task?
    val rw = new RevWalk(repo)
    try {
      val reader = repo.newObjectReader()
      val commit = rw.parseCommit(objectId)

      val oldTree = {
        // protects against initial commit
        if (commit.getParentCount == 0)
          new EmptyTreeIterator
        else
          new CanonicalTreeParser(null, reader, rw.parseCommit(commit.getParent(0).getId).getTree)
      }

      val newTree = new CanonicalTreeParser(null, reader, commit.getTree)

      val df = new DiffFormatter(DisabledOutputStream.INSTANCE)
      df.setRepository(repo)
      df.setDiffComparator(RawTextComparator.DEFAULT)
      df.setDetectRenames(true)
      val diffs = df.scan(oldTree, newTree)
      case class Lines(deleted: Int, added: Int)
      val lines = for {
        diff <- diffs
        edit <- df.toFileHeader(diff).toEditList
      } yield Lines(edit.getEndA - edit.getBeginA, edit.getEndB - edit.getBeginB)
      val ret = CommitsStatistics(diffs.size, lines.map(_.added).sum, lines.map(_.deleted).sum)
      ret

    } finally {
      rw.dispose()
    }
  }


}
