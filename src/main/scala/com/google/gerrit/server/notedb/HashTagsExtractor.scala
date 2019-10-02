// Copyright (C) 2019 GerritForge Ltd
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
package com.google.gerrit.server.notedb

import java.util.function.Supplier

import com.google.gerrit.reviewdb.client.{Change, Project, RefNames}
import com.google.gerrit.server.notedb.ChangeNotesCommit.ChangeNotesRevWalk
import com.google.inject.Inject
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.util.Try

final case class HashTagsExtractor(projectKey: Project.NameKey, repo: Repository, changeNotesCache: ChangeNotesCache) {
  private val log = LoggerFactory.getLogger(classOf[HashTagsExtractor])

  def hashTagsOfCommit(revCommit: RevCommit): Set[String] = {

    lazy val maybeMetaRefs = repo.getRefDatabase.getTipsWithSha1(revCommit).flatMap { ref =>
      Option(Change.Id.fromRef(ref.getName)).map(RefNames.changeMetaRef)
    }

    maybeMetaRefs.headOption.map { metaRef =>
      val metaRefObjectId = repo.getRefDatabase.exactRef(metaRef).getObjectId
      val revWalkSupplier = new Supplier[ChangeNotesRevWalk] {
        override def get(): ChangeNotesRevWalk = ChangeNotesCommit.newRevWalk(repo)
      }

      Try {
        changeNotesCache.get(projectKey, Change.Id.fromRef(metaRef), metaRefObjectId.toObjectId, revWalkSupplier).state().hashtags().toSet
      }.recover {
        case e: Exception => fallBack(Some(s"Could not get changeNote of '${metaRef}' for project ${projectKey.get()}"), Some(e))
      }.get
    }.getOrElse(fallBack())
  }

  private def fallBack(maybeDescription: Option[String] = None, maybeException: Option[Throwable] = None): Set[String] = {
    maybeDescription.foreach {
      log.error(_, maybeException.orNull)
    }
    Set.empty[String]
  }
}
