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

package com.googlesource.gerrit.plugins.analytics.common

import java.security.InvalidParameterException
import java.time.ZoneOffset
import java.util.Date

import com.googlesource.gerrit.plugins.analytics.common.AggregatedCommitHistogram.AggregationStrategyMapping
import org.eclipse.jgit.lib.PersonIdent

sealed case class AggregationStrategy(name: String, aggregationStrategyMapping: AggregationStrategyMapping)

object AggregationStrategy {
  val values = List(EMAIL, EMAIL_HOUR, EMAIL_DAY, EMAIL_MONTH, EMAIL_YEAR)

  def apply(name: String): AggregationStrategy =
    values.find(_.name == name.toUpperCase)
    match {
      case Some(g) => g
      case None => throw new InvalidParameterException(
        s"Must be one of: ${values.map(_.name).mkString(",")}")
    }

  private def getAggregationStrategy(n: Int)(p: PersonIdent, d: Date) = {
    val utctime = d.toInstant.atZone(ZoneOffset.UTC).toLocalDateTime
    val fragments = List(f"${utctime.getYear}%04d", f"${utctime.getMonthValue}%02d", f"${utctime.getDayOfMonth}%02d", f"${utctime.getHour}%02d")
    s"${p.getEmailAddress}/${fragments.take(n).mkString("/")}${"/"*(4-n)}"
  }

  object EMAIL extends AggregationStrategy("EMAIL", (p, d) => p.getEmailAddress + "////")

  object EMAIL_YEAR extends AggregationStrategy("EMAIL_YEAR", getAggregationStrategy(1))

  object EMAIL_MONTH extends AggregationStrategy("EMAIL_MONTH", getAggregationStrategy(2))

  object EMAIL_DAY extends AggregationStrategy("EMAIL_DAY", getAggregationStrategy(3))

  object EMAIL_HOUR extends AggregationStrategy("EMAIL_HOUR", getAggregationStrategy(4))

}
