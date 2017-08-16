package com.googlesource.gerrit.plugins.analytics.common

import java.security.InvalidParameterException

sealed abstract class Granularity(val name: String)

object Granularity {
  val values = List(HOUR, DAY, WEEK, MONTH, YEAR)

  def withName(s: String): Granularity = values.find(_.name == s.toUpperCase) match {
    case Some(g) => g
    case None => throw new InvalidParameterException(
      s"Must be one of: ${values.map(_.name).mkString(",")}")
  }
  object HOUR extends Granularity("HOUR")

  object DAY extends Granularity("DAY")

  object WEEK extends Granularity("WEEK")

  object MONTH extends Granularity("MONTH")

  object YEAR extends Granularity("YEAR")

}
