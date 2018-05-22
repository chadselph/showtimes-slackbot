val Http4sVersion = "0.18.10"
val Specs2Version = "4.0.2"
val LogbackVersion = "1.2.3"
val circeVersion = "0.9.0-M3"

lazy val root = (project in file("."))
  .settings(
    organization := "me.chadrs",
    name := "movie-scraping-api",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.4",
    assemblyJarName in assembly := "movie-scraping-api.jar",
    assemblyMergeStrategy in assembly := {
      case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)

    },
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "org.specs2"     %% "specs2-core"          % Specs2Version % "test",
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion,

      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      // Optional for string interpolation to JSON model
      "io.circe" %% "circe-literal" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "io.github.howardjohn" %% "http4s-lambda" % "0.3.0",
      "software.amazon.awssdk" % "s3" % "2.0.0-preview-9"
    )
  )

