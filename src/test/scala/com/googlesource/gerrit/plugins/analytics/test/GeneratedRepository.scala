package com.googlesource.gerrit.plugins.analytics.test

import java.util.concurrent.atomic.AtomicInteger
import Timer.time

import scala.util.Random

trait GeneratedRepository {
  val rnd = new Random(System.currentTimeMillis)
  val MEGA = 1024 * 1024
  val MAXCOMMITS = 1024 // number of commits to generate
  val MAXBRANCHES = 5   // number of branch labels to generate
  val LENBRANCH = 3     // length of generated branch labels
  val LENCOMMIT = 32    // length of generated commit names

  // populate branchNames and commits randomly, they are accessible via index
  // for random choosing
  val branchNames = Seq.fill(MAXBRANCHES)(nextName(LENBRANCH)).toVector
  val commits = Seq.fill(MAXCOMMITS)(nextName(LENCOMMIT)).toVector

  case class Commit(label: Option[String], commit: String,
                    parents: Set[String])
  // a simulated randomly generated repository with DAG relations inside
  // repository is a Map indexed by commitname, and contains commit, label,
  // parents
  val repository: Map[String, Commit] = time("creating table") {

    val trace = new AtomicInteger(0)
    def traceProgress() {
      val commitsDone = trace.addAndGet(1)
      if (commitsDone % 1000 == 0)
        println(s"... Progress $commitsDone")
    }
    def getAListOfParentsAfterThisIndex(i: Int) = {
      (for {
        j <- i + 1 until Math.min(i + 1 + 1000, MAXCOMMITS)
        parent = commits(j) if prob(0.1)
      } yield parent)
        .toSet // avoids duplicates
    }

    def getAnOptionalLabelFromTheListing = {
      if (prob(1)) Some(branchNames(rnd.nextInt(MAXBRANCHES)))
      else None
    }

    for {i <- 0 until MAXCOMMITS} yield {
      traceProgress()
      val commit = commits(i)
      commit -> Commit(getAnOptionalLabelFromTheListing, commit,
        getAListOfParentsAfterThisIndex(i))
    }
  }.toMap

  def showTable(): Unit = {
    for (i <- 0 until MAXCOMMITS) {
      val commit = repository(commits(i))
      println(s"$i Branch: ${commit.label}  Commit: ${commit.commit}  " +
        s"Parent: ${commit.parents.mkString(",")}")
    }
  }

  private def nextName(len: Int): String = rnd.alphanumeric.take(len).mkString

  private def prob(percentage: Double): Boolean = rnd.nextDouble() <
    percentage / 100

}
