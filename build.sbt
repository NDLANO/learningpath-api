import java.io.FileInputStream
import java.util.Properties

import sbtdocker.Instructions

val Scalaversion = "2.11.8"
val Scalatraversion = "2.3.1"
val Jettyversion = "9.2.10.v20150310"
val AwsSdkversion = "1.10.26"
val ScalaTestVersion = "2.2.6"
val MockitoVersion = "1.10.19"

val appProperties = settingKey[Properties]("The application properties")

appProperties := {
  val prop = new Properties()
  IO.load(prop, new File("build.properties"))
  prop
}

lazy val commonSettings = Seq(
  organization := appProperties.value.getProperty("NDLAOrganization"),
  version := appProperties.value.getProperty("NDLAComponentVersion"),
  scalaVersion := Scalaversion
)

lazy val ITest = config("it") extend(Test)

lazy val learningpath_api = (project in file(".")).
  configs(ITest).
  settings(commonSettings: _*).
  settings(inConfig(ITest)(Defaults.testTasks): _*).
  settings(
    name := "learningpath-api",
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
    scalacOptions := Seq("-target:jvm-1.7"),
    libraryDependencies ++= Seq(
      "ndla" %% "logging" % "0.1-SNAPSHOT",
      "ndla" %% "logging" % "0.1-SNAPSHOT" % "test" classifier "tests",
      "ndla" %% "network" % "0.1-SNAPSHOT",
      "ndla" %% "mapping" % "0.1-SNAPSHOT",
      "org.specs2" %% "specs2-core" % "2.4.14" % "test",
      "org.scalatra" %% "scalatra" % Scalatraversion,
      "org.eclipse.jetty" % "jetty-webapp" % Jettyversion % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % Jettyversion % "container",
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "container;provided;test",
      "org.scalatra" %% "scalatra-json" % Scalatraversion,
      "org.json4s"   %% "json4s-native" % "3.2.11",
      "org.scalatra" %% "scalatra-swagger"  % Scalatraversion,
      "org.scalikejdbc" %% "scalikejdbc" % "2.2.8",
      "org.postgresql" % "postgresql" % "9.4-1201-jdbc4",
      "mysql" % "mysql-connector-java" % "5.1.36",
      "org.scalaj" %% "scalaj-http" % "1.1.5",
      "com.sksamuel.elastic4s" %% "elastic4s-core" % "2.3.0",
      "com.netaporter" %% "scala-uri" % "0.4.12",
      "org.jsoup" % "jsoup" % "1.7.3",
      "org.scalatest" % "scalatest_2.11" % ScalaTestVersion % "test",
      "org.mockito" % "mockito-all" % MockitoVersion % "test",
      "com.h2database"  %  "h2" % "1.4.191",
      "org.flywaydb" % "flyway-core" % "4.0")
  ).enablePlugins(DockerPlugin).enablePlugins(GitVersioning).enablePlugins(JettyPlugin)

assemblyJarName in assembly := "learningpath-api.jar"
mainClass in assembly := Some("no.ndla.learningpathapi.JettyLauncher")
assemblyMergeStrategy in assembly := {
  case "mime.types" => MergeStrategy.filterDistinctLines
  case PathList("org", "joda", "convert", "ToString.class")  => MergeStrategy.first
  case PathList("org", "joda", "convert", "FromString.class")  => MergeStrategy.first
  case PathList("org", "joda", "time", "base", "BaseDateTime.class")  => MergeStrategy.filterDistinctLines
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

// Don't run Integration tests in default run
def itFilter(name: String): Boolean = name endsWith "IntegrationTest"
def unitFilter(name: String): Boolean = (name endsWith "Test") && !itFilter(name)
testOptions in Test := Seq(Tests.Filter(unitFilter))
testOptions in ITest := Seq(Tests.Filter(itFilter))

// Make the docker task depend on the assembly task, which generates a fat JAR file
docker <<= (docker dependsOn assembly)

dockerfile in docker := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("java")

    add(artifact, artifactTargetPath)
    entryPoint("java", "-Dorg.scalatra.environment=production", "-jar", artifactTargetPath)
  }
}

imageNames in docker := Seq(
  ImageName(
    namespace = Some(organization.value),
    repository = name.value,
    tag = Some(System.getProperty("docker.tag", "SNAPSHOT")))
)

publishTo := {
  val nexus = "https://nexus.knowit.no/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/ndla-snapshots")
  else
    Some("releases"  at nexus + "content/repositories/ndla-releases")
}

resolvers ++= Seq(
  "Snapshot Sonatype Nexus Repository Manager" at "https://nexus.knowit.no/content/repositories/ndla-snapshots",
  "Release Sonatype Nexus Repository Manager" at "https://nexus.knowit.no/content/repositories/ndla-releases"
)

credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.knowit.no", "ndla", "1814Ndla")
