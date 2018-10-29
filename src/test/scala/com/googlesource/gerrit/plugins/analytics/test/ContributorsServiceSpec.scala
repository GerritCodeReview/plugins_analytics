package com.googlesource.gerrit.plugins.analytics.test

import java.util
import java.util.Date

import com.google.common.collect.ImmutableSortedSet
import com.google.gerrit.reviewdb.client.{AccountGroup, Project}
import com.google.gerrit.reviewdb.client.Project.NameKey
import com.google.gerrit.server.git.{
  GitRepositoryManager,
  LocalDiskRepositoryManager
}
import com.google.gerrit.server.project.{
  ProjectCache,
  ProjectCacheImpl,
  ProjectState
}
import com.googlesource.gerrit.plugins.analytics.ContributorsService
import com.googlesource.gerrit.plugins.analytics.common.AggregationStrategy.EMAIL_HOUR
import com.googlesource.gerrit.plugins.analytics.common.{
  GsonFormatter,
  UserActivityHistogram
}
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.{PersonIdent, Repository}
import org.eclipse.jgit.revwalk.RevCommit
import org.scalatest.{FlatSpec, Matchers}

class ContributorsServiceSpec extends FlatSpec with Matchers with GitTestCase {
  "Ciccio" should "dostuff" in {

    class MockRepoManager extends GitRepositoryManager {
      override def openRepository(name: Project.NameKey): Repository =
        new FileRepository(testRepo)

      override def createRepository(name: Project.NameKey): Repository =
        new FileRepository(testRepo)

      override def list(): util.SortedSet[Project.NameKey] =
        new util.TreeSet()

    }
    class MockedProjectCache extends ProjectCache {
      override def getAllProjects: ProjectState = ???

      override def getAllUsers: ProjectState = ???

      override def get(projectName: NameKey): ProjectState = ???

      override def checkedGet(projectName: NameKey): ProjectState = ???

      override def checkedGet(projectName: NameKey,
                              strict: Boolean): ProjectState = ???

      override def evict(p: Project): Unit = ???

      override def evict(p: NameKey): Unit = ???

      override def remove(p: Project): Unit = ???

      override def remove(name: NameKey): Unit = ???

      override def all(): ImmutableSortedSet[NameKey] = ???

      override def guessRelevantGroupUUIDs(): util.Set[AccountGroup.UUID] = ???

      override def byName(prefix: String): ImmutableSortedSet[NameKey] = ???

      override def onCreateProject(newProjectName: NameKey): Unit = ???
    }
    val cs = new ContributorsService(new MockRepoManager(),
                                     new MockedProjectCache(),
                                     new UserActivityHistogram(),
                                     new GsonFormatter())

    val c1 = commit("Ciccio", "newFile", "content")
    branch("another/branch")
    val c2 = commit("Ciccio", "newFile1", "content1")

    val userActivitySummary = cs
      .userActivitySummary(new FileRepository(testRepo),
                           List.empty,
                           Some(1000L),
                           None,
                           true,
                           EMAIL_HOUR)

    userActivitySummary should have size 2

    userActivitySummary.last.commits.size should be(1)
    userActivitySummary.head.commits.size should be(2)

  }

  def commit(committer: String,
             fileName: String,
             content: String): RevCommit = {
    val date = new Date()
    val person = newPersonIdent(committer, committer, date)
    add(testRepo, fileName, content, author = person, committer = author)
  }

}
