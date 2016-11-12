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

package com.googlesource.gerrit.plugins.analytics;


import org.eclipse.jgit.lib.ObjectId;
// import org.gitective.core.stat.UserCommitActivity;
import org.gitective.core.stat.UserCommitActivity;

import java.util.ArrayList;
import java.util.List;

public class UserActivitySummary {
  public final String name;
  public final String email;
  public final int numCommits;
  public final List<CommitInfo> commits;
  public final long lastCommitDate;


  public UserActivitySummary(String name, String email, int numCommits,
      List<CommitInfo> commits, long lastCommitDate) {
    this.name = name;
    this.email = email;
    this.numCommits = numCommits;
    this.commits = commits;
    this.lastCommitDate = lastCommitDate;
  }

  public static UserActivitySummary fromUserActivity(UserCommitActivity uca) {
    return new UserActivitySummary(uca.getName(), uca.getEmail(),
        uca.getCount(), getCommits(uca.getIds(), uca.getTimes(),
            uca.getMerges()), uca.getLatest());
  }

  private static List<CommitInfo> getCommits(ObjectId[] ids, long[] times,
      boolean[] merges) {
    List<CommitInfo> commits = new ArrayList<>(ids.length);

    for (int i = 0; i < ids.length; i++) {
      commits.add(new CommitInfo(ids[i].name(), times[i], merges[i]));
    }

    return commits;
  }
}
