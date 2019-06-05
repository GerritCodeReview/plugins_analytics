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

package com.googlesource.gerrit.plugins.analytics

import com.google.gerrit.extensions.annotations.PluginName
import com.google.gerrit.server.config.PluginConfigFactory
import com.google.inject.{ImplementedBy, Inject}
import org.eclipse.jgit.lib.Config

@ImplementedBy(classOf[AnalyticsConfigImpl])
trait AnalyticsConfig {
  def botlikeFilenameRegexps: List[String]
  def isExtractIssues: Boolean
  def isIgnoreBinaryFiles: Boolean
}

class AnalyticsConfigImpl @Inject() (val pluginConfigFactory: PluginConfigFactory, @PluginName val pluginName: String) extends AnalyticsConfig{
  lazy val botlikeFilenameRegexps: List[String] = pluginConfigBotLikeFilenameRegexp
  lazy val isExtractIssues: Boolean = pluginConfig.getBoolean(Contributors, null, ExtractIssues, false)
  lazy val isIgnoreBinaryFiles: Boolean = pluginConfig.getBoolean(Contributors, null, IgnoreBinaryFiles, false)

  private lazy val pluginConfig: Config = pluginConfigFactory.getGlobalPluginConfig(pluginName)
  private val Contributors = "contributors"
  private val BotlikeFilenameRegexp = "botlike-filename-regexp"
  private val ExtractIssues = "extract-issues"
  private val IgnoreBinaryFiles = "ignore-binary-files"
  private lazy val pluginConfigBotLikeFilenameRegexp = pluginConfig.getStringList(Contributors, null, BotlikeFilenameRegexp).toList
}
