enablePlugins(GitVersioning)

val gerritApiVersion = "2.15.3"

val pluginName = "analytics"

git.useGitDescribe := true

lazy val root = (project in file("."))
  .settings(
    name := pluginName,

    version := "2.14-SNAPSHOT",

    scalaVersion := "2.11.8",

    libraryDependencies ++= Seq(
      "io.fabric8" % "gitective-core" % "0.9.37"
        exclude ("org.eclipse.jgit", "org.eclipse.jgit"),

      "com.google.inject" % "guice" % "4.2.0" % Provided,
      "com.google.gerrit" % "gerrit-plugin-api" % gerritApiVersion % Provided withSources(),
      "com.google.code.gson" % "gson" % "2.8.0" % Provided,
      "joda-time" % "joda-time" % "2.9.9" % Provided,

      "org.scalatest" %% "scalatest" % "3.0.1" % Test,
      "net.codingwell" %% "scala-guice" % "4.1.0" % Test),

    assemblyJarName in assembly := s"$pluginName.jar",

    packageOptions in(Compile, packageBin) += Package.ManifestAttributes(
      ("Gerrit-ApiType", "plugin"),
      ("Gerrit-PluginName", pluginName),
      ("Gerrit-Module", "com.googlesource.gerrit.plugins.analytics.Module"),
      ("Gerrit-SshModule", "com.googlesource.gerrit.plugins.analytics.SshModule"),
      ("Implementation-Title", "Analytics plugin"),
      ("Implementation-URL", "https://gerrit.googlesource.com/plugins/analytics")
    )
  )

