// Copyright (C) 2016 The Android Open Source Project
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

import java.util.Date

import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}

trait Dates {
  val MIN_DATE = "00000101"
  val MAX_DATE = "99991231"
  // Joda dateformatter is faster than SimpleDateFormat and threadsafe
  // ISODateTimeFormat accepts yyyyMMdd format
  private val date_formatter = ISODateTimeFormat.basicDate()

  def long_to_iso_date(d: Long): String = date_formatter.print(d)

  def parse_date(s: String) = new Date(date_formatter.parseMillis(s))

}
