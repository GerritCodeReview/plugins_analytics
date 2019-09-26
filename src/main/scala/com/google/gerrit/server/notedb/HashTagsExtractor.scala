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

package com.google.gerrit.server.notedb

import java.util.function.Supplier

import com.google.gerrit.entities.Change.Id
import com.google.gerrit.entities.Project
import com.google.gerrit.server.notedb.ChangeNotesCommit.ChangeNotesRevWalk
import com.googlesource.gerrit.plugins.analytics.common.ManagedResource.use
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.{AnyObjectId, Ref, Repository}
import org.eclipse.jgit.revwalk.RevCommit
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.Try

final case class HashTagsExtractor(projectKey: Project.NameKey, repo: Repository, changeNotesCache: ChangeNotesCache) {
  private val log = LoggerFactory.getLogger(classOf[HashTagsExtractor])

  lazy val objectsToRefs: collection.Map[AnyObjectId, Set[Ref]] = {
    use(new Git(repo)) {
      _.getRepository.getAllRefsByPeeledObjectId.asScala.mapValues(_.asScala.toSet)
    }
  }

  lazy val refsToObjects: Map[String, AnyObjectId] =
    objectsToRefs.foldLeft(Map[String, AnyObjectId]()) { case (acc, (objectId, refs)) =>
      acc ++ refs.toSeq.flatMap { ref =>
        Map(ref.getName -> objectId)
      }.toMap
    }

  def tagsOfCommit(revCommit: RevCommit): Seq[String] = {
    val maybeRefs = objectsToRefs.get(revCommit.toObjectId)
    maybeRefs.map { refs =>
      log.debug(s"Extracting meta refs from ${refs.map(_.getName).mkString(",")}")
      // We can take the first one since we know there is a one to one mapping between change refs and commit objects in Gerrit
      refs.collectFirst {
        case c if c.getName.startsWith("refs/changes/") => {
          val changeName = c.getName
          val metaRefString = changeName.take(changeName.lastIndexOf("/")).concat("/meta")
          refsToObjects.get(metaRefString).map { metaRefObjectId =>
            val revWalkSupplier = new Supplier[ChangeNotesRevWalk] {
              override def get(): ChangeNotesRevWalk = ChangeNotesCommit.newRevWalk(repo)
            }
            Try {
              changeNotesCache
                .get(projectKey, Id.fromRef(metaRefString), metaRefObjectId.toObjectId, revWalkSupplier)
                .state()
                .hashtags()
                .asScala
                .toSeq
            }.recover {
              case e: Exception =>
                fallBack(Some(s"Could not get changeNote of '${metaRefString}' for project ${projectKey.get()}"), Some(e))
            }.get
          }.getOrElse(fallBack(Some(s"Could not find metaRef object '${metaRefString}'")))
        }
      }.getOrElse(fallBack())
    }.getOrElse(fallBack())
  }

  private def fallBack(maybeDescription: Option[String] = None, maybeException: Option[Throwable] = None): Seq[String] = {
    maybeDescription.foreach {
      log.error(_, maybeException.orNull)
    }
    Seq.empty[String]
  }
}

