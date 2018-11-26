package com.googlesource.gerrit.plugins.analytics.common

import com.google.gerrit.server.change.ChangeFinder
import org.eclipse.jgit.revwalk.RevCommit
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

case class HashTagsExtractor(changeFinder: ChangeFinder) {
  val log = LoggerFactory.getLogger(classOf[Statistics])
  def findByCommit(commit: RevCommit): Seq[String] = {
    val c = changeFinder.findOne(commit.getName)
    // Consider the case case of the DB being out of sync with what's in the git repo
    if (c != null) {
      c.getHashtags.toSeq
    } else {
      log.warn(s"Missing commit ${commit.getName} from DB.")
      Seq.empty
    }
  }
}
