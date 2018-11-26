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
import org.eclipse.jgit.lib.PersonIdent

sealed trait AggregationStrategy {
  def name: String
  def mapping: AggregationStrategyMapping
}

object AggregationStrategy {
  val baseValues = List(EMAIL, EMAIL_HOUR, EMAIL_DAY, EMAIL_MONTH, EMAIL_YEAR)

  def apply(name: String): AggregationStrategy =
    baseValues.find(_.name == name.toUpperCase) match {
      case Some(g) => g
      case None => throw new InvalidParameterException(
        s"Must be one of: ${baseValues.map(_.name).mkString(",")}")
    }

  implicit class PimpedDate(val d: Date) extends AnyVal {
    def utc: LocalDateTime = d.toInstant.atZone(ZoneOffset.UTC).toLocalDateTime
  }

  case class AggregationKey(email: String,
                            year: Option[Int] = None,
                            month: Option[Int] = None,
                            day: Option[Int] = None,
                            hour: Option[Int] = None,
                            branch: Option[String] = None,
                            hashtag: Option[String] = None
                           ) {
    override def toString: String = {
      s"$email/${year.getOrElse("")}/${month.getOrElse("")}/${day.getOrElse("")}/${hour.getOrElse("")}/${branch.getOrElse("")}/${hashtag.getOrElse("")}"
    }
  }

  object EMAIL extends AggregationStrategy {
    val name: String = "EMAIL"
    val mapping: (PersonIdent, Date) => AggregationKey = (p, _) =>
      AggregationKey(email = p.getEmailAddress)
  }

  object EMAIL_YEAR extends AggregationStrategy {
    val name: String = "EMAIL_YEAR"
    val mapping: (PersonIdent, Date) => AggregationKey = (p, d) =>
      AggregationKey(email = p.getEmailAddress, year = Some(d.utc.getYear))
  }

  object EMAIL_MONTH extends AggregationStrategy {
    val name: String = "EMAIL_MONTH"
    val mapping: (PersonIdent, Date) => AggregationKey = (p, d) =>
      AggregationKey(email = p.getEmailAddress,
                     year = Some(d.utc.getYear),
                     month = Some(d.utc.getMonthValue))
  }

  object EMAIL_DAY extends AggregationStrategy {
    val name: String = "EMAIL_DAY"
    val mapping: (PersonIdent, Date) => AggregationKey = (p, d) =>
      AggregationKey(email = p.getEmailAddress,
                     year = Some(d.utc.getYear),
                     month = Some(d.utc.getMonthValue),
                     day = Some(d.utc.getDayOfMonth))
  }

  object EMAIL_HOUR extends AggregationStrategy {
    val name: String = "EMAIL_HOUR"
    val mapping: (PersonIdent, Date) => AggregationKey = (p, d) =>
      AggregationKey(email = p.getEmailAddress,
                     year = Some(d.utc.getYear),
                     month = Some(d.utc.getMonthValue),
                     day = Some(d.utc.getDayOfMonth),
                     hour = Some(d.utc.getHour))
  }

  case class DYNAMIC_AGGREGATION(baseAggregationStrategy: AggregationStrategy, mapping: AggregationStrategyMapping)
    extends AggregationStrategy {
    val name: String = s"DYNAMIC_AGGREGATION"
  }

}
