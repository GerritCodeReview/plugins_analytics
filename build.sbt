enablePlugins(GitVersioning)

val gerritApiVersion = "3.7.0-rc3"

val pluginName = "analytics"

git.useGitDescribe := true

concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)

lazy val root = (project in file("."))
  .settings(
    name := pluginName,
    version := gerritApiVersion,

    scalaVersion := "2.13.10",

    libraryDependencies ++= Seq(
      "io.fabric8" % "gitective-core" % "0.9.54"
        exclude ("org.eclipse.jgit", "org.eclipse.jgit"),

      "com.google.inject" % "guice" % "5.0.1" % Provided,
      "com.google.gerrit" % "gerrit-plugin-api" % gerritApiVersion % Provided withSources(),
      "com.google.code.gson" % "gson" % "2.8.5" % Provided,
      "joda-time" % "joda-time" % "2.9.9",
      "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.1",

      "com.google.gerrit" % "gerrit-acceptance-framework" % gerritApiVersion % Test,
      "org.bouncycastle" % "bcpg-jdk15on" % "1.61" % Test,
      "org.scalatest" %% "scalatest" % "3.2.15" % Test,
      "net.codingwell" %% "scala-guice" % "5.0.0" % Test),

    assembly / assemblyJarName := s"$pluginName.jar",

    packageOptions in(Compile, packageBin) += Package.ManifestAttributes(
      ("Gerrit-ApiType", "plugin"),
      ("Gerrit-PluginName", pluginName),
      ("Gerrit-Module", "com.googlesource.gerrit.plugins.analytics.Module"),
      ("Gerrit-SshModule", "com.googlesource.gerrit.plugins.analytics.SshModule"),
      ("Implementation-Title", "Analytics plugin"),
      ("Implementation-URL", "https://gerrit.googlesource.com/plugins/analytics")
    )
  )

