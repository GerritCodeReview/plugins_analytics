// Copyright (C) 2018 GerritForge Ltd
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.analytics.common

import scala.util.matching.Regex

trait BotLikeExtractor {
  def isBotLike(files: Set[String]): Boolean
}

class BotLikeExtractorImpl(botLikeIdentifiers: List[String]) extends BotLikeExtractor {
  private val MATCH_NOTHING = new Regex("^$")

  private val botRegexps = if(botLikeIdentifiers.isEmpty) MATCH_NOTHING else new Regex(botLikeIdentifiers.mkString("|"))

  override def isBotLike(files: Set[String]): Boolean = files.forall(botRegexps.findFirstIn(_).isDefined)
}
