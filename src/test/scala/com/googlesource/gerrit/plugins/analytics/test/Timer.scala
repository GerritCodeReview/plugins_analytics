package com.googlesource.gerrit.plugins.analytics.test

object Timer {
  def time[R](name: String)(block: => R): R = {
    val t0 = System.nanoTime()
    val result = block
    println(s"Elapsed time for '$name': " + (System.nanoTime.toDouble - t0)
      / 1000 / 1000 / 1000 +
      "s")
    result
  }
}
