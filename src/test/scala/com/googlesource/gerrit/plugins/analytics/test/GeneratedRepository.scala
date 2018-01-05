package com.googlesource.gerrit.plugins.analytics.test

import java.util.concurrent.atomic.AtomicInteger
import Timer.time

import scala.util.Random

trait GeneratedRepository {
  val rnd = new Random(System.currentTimeMillis)
  val MEGA = 1024 * 1024
  val MAXCOMMITS = 10 // number of commits to generate
  val MAXBRANCHES = 5 // number of branch labels to generate
  val LENBRANCH = 3 // length of generated branch labels
  val LENCOMMIT = 4 // length of generated commit names


  val commits = Seq.fill(MAXCOMMITS)(nextName(LENCOMMIT)).toVector

  case class Commit(commit: String,
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
        parent = commits(j) if prob(30)
      } yield parent)
        .toSet // avoids duplicates
    }

    for {i <- 0 until MAXCOMMITS} yield {
      traceProgress()
      val commit = commits(i)
      commit -> Commit(commit, getAListOfParentsAfterThisIndex(i))
    }
  }.toMap

  val branchNames = Seq.fill(MAXBRANCHES)(nextName(LENBRANCH)).toVector

  case class BranchHead(name: String, commit: String)

  val branchHeads = branchNames.map(
    name => (name, commits(rnd.nextInt(MAXCOMMITS)))
  ).toMap

  val labeledCommits = commits.map(commit =>
    (commit, getBranchesFromCommit(commit))
  ).toMap

  def getBranchesFromCommit(commit: String): Map[String, Set[String]] = {
    def checkRecursiveCommit(branch: String, branchChainElement: Commit)
    : Set[String]
    = {
      if (branchChainElement.commit == commit) Set(branch)
      else branchChainElement.parents.flatMap {
        parent =>
          checkRecursiveCommit(branch, repository(parent))
      }
    }

    branchHeads.map { case (name, head) => (commit, checkRecursiveCommit(name,
      repository(head)))
    }

  }


  def showTable(): Unit = {
    for (i <- 0 until MAXCOMMITS) {
      val commit = repository(commits(i))
      println(s"$i Commit: ${commit.commit}  " +
        s"Parent: ${commit.parents.mkString(",")}")
    }
  }

  private def nextName(len: Int): String = rnd.alphanumeric.take(len).mkString

  private def prob(percentage: Double): Boolean = rnd.nextDouble() <
    percentage / 100

}
