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
import java.time.{LocalDateTime, ZoneOffset}
import java.util.Date

import com.googlesource.gerrit.plugins.analytics.common.AggregatedCommitHistogram.AggregationStrategyMapping

sealed case class AggregationStrategy(name: String, mapping: AggregationStrategyMapping)

object AggregationStrategy {
  val values = List(EMAIL, EMAIL_HOUR, EMAIL_DAY, EMAIL_MONTH, EMAIL_YEAR)

  def apply(name: String): AggregationStrategy =
    values.find(_.name == name.toUpperCase) match {
      case Some(g) => g
      case None => throw new InvalidParameterException(
        s"Must be one of: ${values.map(_.name).mkString(",")}")
    }

  implicit class PimpedDate(val d: Date) extends AnyVal {
    def utc: LocalDateTime = d.toInstant.atZone(ZoneOffset.UTC).toLocalDateTime
  }

  object EMAIL extends AggregationStrategy("EMAIL",
    (p, _) => s"${p.getEmailAddress}////")

  object EMAIL_YEAR extends AggregationStrategy("EMAIL_YEAR",
    (p, d) => s"${p.getEmailAddress}/${d.utc.getYear}///")

  object EMAIL_MONTH extends AggregationStrategy("EMAIL_MONTH",
    (p, d) => s"${p.getEmailAddress}/${d.utc.getYear}/${d.utc.getMonthValue}//")

  object EMAIL_DAY extends AggregationStrategy("EMAIL_DAY",
    (p, d) => s"${p.getEmailAddress}/${d.utc.getYear}/${d.utc.getMonthValue}/${d.utc.getDayOfMonth}/")

  object EMAIL_HOUR extends AggregationStrategy("EMAIL_HOUR",
    (p, d) => s"${p.getEmailAddress}/${d.utc.getYear}/${d.utc.getMonthValue}/${d.utc.getDayOfMonth}/${d.utc.getHour}")
}