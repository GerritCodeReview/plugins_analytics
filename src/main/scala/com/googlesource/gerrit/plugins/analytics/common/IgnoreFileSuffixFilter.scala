// Copyright (C) 2019 The Android Open Source Project
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

import com.google.inject.{Inject, Singleton}
import com.googlesource.gerrit.plugins.analytics.AnalyticsConfig
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.TreeFilter
import org.gitective.core.PathFilterUtils

@Singleton
case class IgnoreFileSuffixFilter @Inject() (config: AnalyticsConfig) extends TreeFilter {

  private lazy val ignoreFilter =
    if (config.ignoreFileSuffixes.nonEmpty)
      PathFilterUtils.orSuffix(config.ignoreFileSuffixes:_*).negate()
    else
      TreeFilter.ALL

  override def include(treeWalk: TreeWalk): Boolean = treeWalk.isSubtree || ignoreFilter.include(treeWalk)
  override def shouldBeRecursive(): Boolean = ignoreFilter.shouldBeRecursive()
  override def clone(): TreeFilter = this
}
