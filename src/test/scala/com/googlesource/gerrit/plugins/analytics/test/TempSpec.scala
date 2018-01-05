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

package com.googlesource.gerrit.plugins.analytics.test

import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random


class TempSpec extends FlatSpec with Matchers {

  val rnd = new Random(System.currentTimeMillis)
  val MEGA = 1024 * 1024
  val MAXCOMMITS = 10000
  val MAXBRANCHES = 5
  val LENBRANCH = 3
  val LENCOMMIT = 32
  val branchNames = Seq.fill(MAXBRANCHES)(nextName(LENBRANCH)).toVector
  val commits = Seq.fill(MAXCOMMITS)(nextName(LENCOMMIT)).toVector
  val trace = new AtomicInteger(0)

  val table: Map[String, Commit] = time("creating table") {

    for {i <- 0 until MAXCOMMITS} yield {
      val label = if (prob(1)) Some(branchNames(rnd.nextInt(MAXBRANCHES)))
      else None
      val commit = commits(i)
      val parents = for {
        j <- i + 1 until MAXCOMMITS
        parent = commits(j) if prob(0.1)
      } yield parent


      val commitsDone = trace.addAndGet(1)
      if (commitsDone % 1000 == 0)
        println(s"... Progress $commitsDone")

      commit -> Commit(label, commit, parents.toSet)
    }
  }.toMap


  def time[R](name: String)(block: => R): R = {
    val t0 = System.nanoTime()
    val result = block
    println(s"Elapsed time for '$name': " + (System.nanoTime.toDouble - t0)
      / 1000 / 1000 / 1000 +
      "s")
    result
  }

  def showTable(): Unit = {
    for (i <- 0 until MAXCOMMITS) {
      val commit = table(commits(i))
      println(s"$i Branch: ${commit.label}  Commit: ${commit.commit}  " +
        s"Parent: ${commit.parents.mkString(",")}")
    }
  }

  private def nextName(len: Int): String = rnd.alphanumeric.take(len).mkString

  private def prob(percentage: Double): Boolean = rnd.nextDouble() <
    percentage / 100

  private def enrich(commit: Commit): Set[String] = {
    //println(s"analyzing commit $commit")
    commit.label.fold(commit.parents.flatMap(commitName => enrich(table
    (commitName))))(label => Set[String](label))
  }

  case class Commit(label: Option[String], commit: String,
                    parents: Set[String])


  "Test" should "work" in {
    time("enriching table") {
      table.values.map { c => {
        enrich(c)

      }
      }
    }
  }
}
