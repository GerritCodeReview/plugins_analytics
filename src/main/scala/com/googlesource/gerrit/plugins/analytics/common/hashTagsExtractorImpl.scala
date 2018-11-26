// Copyright (C) 2019 The Android Open Source Project
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

import com.google.gerrit.reviewdb.client.Change.Id
import com.google.gerrit.reviewdb.client.{Change, Project}
import com.google.gerrit.server.notedb.{NoteDbMetrics, _}
import com.googlesource.gerrit.plugins.analytics.common.ManagedResource.use
import javax.inject.Inject
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib._
import org.slf4j.LoggerFactory
import org.eclipse.jgit.revwalk.RevCommit
import com.google.gerrit.server.notedb._

import scala.collection.JavaConversions._
import scala.util.Try

trait HashTagsExtractor {
  def tagsOfCommit(revCommit: RevCommit): Seq[String]
}

case class HashTagsExtractorImpl @Inject()(
  changeNotesArgs: AbstractChangeNotes.Args,
  changeNoteJson: ChangeNoteJson,
  legacyChangeNoteRead: LegacyChangeNoteRead,
  noteDbMetrics : NoteDbMetrics,
  repo: Repository,
  project: Project.NameKey
) extends HashTagsExtractor {
  private val log = LoggerFactory.getLogger(classOf[HashTagsExtractorImpl])

  def tagsOfCommit(revCommit: RevCommit): Seq[String] = {
    val maybeRefs = objectsToRefs.get(revCommit.toObjectId)

    maybeRefs.map{ refs =>
      log.debug(s"Extracting meta refs from ${refs.map(_.getName).mkString(",")}")
      // We can take the first one since we know there is a one to one mapping between change refs and commit objects in Gerrit
      val changeRef = refs.filter(x => x.getName.startsWith("refs/changes/")).head
      val metaRefString = changeRef.getName.take(changeRef.getName.lastIndexOf("/")).concat("/meta")
      log.debug(s"Extracted meta ref '$metaRefString' from change ref '$changeRef'")
      val maybeMetaRefObjectId = refsToObjects.get(metaRefString)

      val rw = ChangeNotesCommit.newRevWalk(repo)

      val change: Change = ChangeNotes.Factory.newNoteDbOnlyChange(project, Id.fromRef(metaRefString))

      maybeMetaRefObjectId.map { metaRefObjectId =>
        val cnp = new ChangeNotesParser(change.getId, metaRefObjectId.toObjectId, rw, changeNoteJson, legacyChangeNoteRead, noteDbMetrics, true)
        //XXX Don't catch...temporarily catching to avoid Gerrit errors when extracting hashtags
        val changeNoteState = Try(cnp.parseAll()).toOption
        changeNoteState.map(_.hashtags().toSeq).getOrElse(Seq.empty)
      }.getOrElse{
        log.error(s"Cannot find meta ref object for $metaRefString")
        Seq.empty[String]
      }
    }.getOrElse(Seq.empty[String])
  }

  lazy val objectsToRefs: collection.Map[AnyObjectId, Set[Ref]] = {
    use(new Git(repo)) { _.getRepository.getAllRefsByPeeledObjectId.mapValues(_.toSet) }
  }

  lazy val refsToObjects: Map[String, AnyObjectId] =
    objectsToRefs.foldLeft(Map[String, AnyObjectId]()){case (acc, (objectId,refs)) =>
      acc ++ refs.toSeq.flatMap{ ref =>
        Map(ref.getName -> objectId)
      }.toMap
  }
}
