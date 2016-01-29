import java.io.FileInputStream
import java.util.Properties

import sbtdocker.Instructions

val Scalaversion = "2.11.6"
val Scalatraversion = "2.3.1"
val Jettyversion = "9.2.10.v20150310"
val AwsSdkversion = "1.10.26"
val ScalaTestVersion = "2.2.4"
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

lazy val learningpath_api = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "learningpath-api",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions := Seq("-target:jvm-1.8"),
    libraryDependencies ++= Seq(
      "ndla" %% "logging" % "0.1-SNAPSHOT",
      "ndla" %% "network" % "0.1-SNAPSHOT",
      "org.scalatra" %% "scalatra" % Scalatraversion,
      "org.eclipse.jetty" % "jetty-webapp" % Jettyversion % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % Jettyversion % "container",
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "container;provided;test",
      "org.scalatra" %% "scalatra-json" % Scalatraversion,
      "org.json4s"   %% "json4s-native" % "3.2.11",
      "org.scalatra" %% "scalatra-swagger"  % Scalatraversion,
      "org.scalikejdbc" %% "scalikejdbc" % "2.2.8",
      "org.postgresql" % "postgresql" % "9.4-1201-jdbc4",
      "org.scalaj" %% "scalaj-http" % "1.1.5",
      "com.sksamuel.elastic4s" %% "elastic4s-core" % "1.7.4",
      "org.scalatest" % "scalatest_2.11" % ScalaTestVersion % "test",
      "org.mockito" % "mockito-all" % MockitoVersion % "test")
  ).enablePlugins(DockerPlugin).enablePlugins(GitVersioning).enablePlugins(JettyPlugin)

assemblyJarName in assembly := "learningpath-api.jar"
mainClass in assembly := Some("no.ndla.learningpathapi.JettyLauncher")
assemblyMergeStrategy in assembly := {
  case "mime.types" => MergeStrategy.filterDistinctLines
  case PathList("org", "joda", "convert", "ToString.class")  => MergeStrategy.first
  case PathList("org", "joda", "convert", "FromString.class")  => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

// Don't run Integration tests in default run
testOptions in Test += Tests.Argument("-l", "no.ndla.IntegrationTest")

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

credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.knowit.no", "ndla", "ndla")