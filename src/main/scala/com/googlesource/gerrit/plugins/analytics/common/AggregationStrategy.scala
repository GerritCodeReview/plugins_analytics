package com.googlesource.gerrit.plugins.analytics.common

import java.security.InvalidParameterException
import java.time.ZoneOffset
import java.util.Date

import com.googlesource.gerrit.plugins.analytics.common.AggregatedCommitHistogram.AggregationStrategyMapping
import org.eclipse.jgit.lib.PersonIdent

sealed case class AggregationStrategy(name: String, aggregationStrategyMapping: AggregationStrategyMapping)

object AggregationStrategy {
  val values = List(EMAIL, HOUR, DAY, MONTH, YEAR)

  def apply(name: String): AggregationStrategy =
    values.find(_.name == name.toUpperCase)
    match {
      case Some(g) => g
      case None => throw new InvalidParameterException(
        s"Must be one of: ${values.map(_.name).mkString(",")}")
    }

  private def getAggregatedKey(n: Int)(p: PersonIdent, d: Date) = {
    val utctime = d.toInstant.atZone(ZoneOffset.UTC).toLocalDateTime
    val fragments = List(f"${utctime.getYear}%04d", f"${utctime.getMonthValue}%02d", f"${utctime.getDayOfMonth}%02d", f"${utctime.getHour}%02d")
    s"${p.getEmailAddress}/${fragments.take(n).mkString("/")}"
  }

  object EMAIL extends AggregationStrategy("EMAIL", (p, d) => p.getEmailAddress)

  object YEAR extends AggregationStrategy("YEAR", getAggregatedKey(1))

  object MONTH extends AggregationStrategy("MONTH", getAggregatedKey(2))

  object DAY extends AggregationStrategy("DAY", getAggregatedKey(3))

  object HOUR extends AggregationStrategy("HOUR", getAggregatedKey(4))

}
