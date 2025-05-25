ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.5.2"
Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .aggregate(server)
  .settings(
    name := "aboutMe",
    run := (server / Compile / run).evaluated
  )

lazy val server = (project in file("server"))
.enablePlugins(DockerPlugin, AshScriptPlugin)
.settings(
    name := "aboutMe-server",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.1.18",
      "dev.zio" %% "zio-http" % "3.3.0"
    ),

    Compile / mainClass := Some("org.reportflow.aboutMe.Main"),
    Compile / run := ((Compile / run) dependsOn (client / Compile / fastLinkJS)).evaluated,
    clean := clean.dependsOn(client / clean).value,
    Compile / resourceGenerators += Def.task {
      val log = streams.value.log

      val jsFile =  ((client / Compile / fastLinkJS / scalaJSLinkedFile).value.data)
      log.info(s"jsFile -> ${jsFile.toString}")

      val jsFileName = jsFile.name
      // Attempt to find the source map file
      val scalaJSOutputDir = (client / Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value
      val sourceMapBaseName = if (jsFileName.endsWith(".js")) jsFileName.dropRight(3) else jsFileName
      val sourceMapFile = scalaJSOutputDir / (sourceMapBaseName + ".js.map")


      val targetDir = (Compile / resourceManaged).value / "public"

      val copiedFiles = Seq.newBuilder[File]

      if (sourceMapFile.exists()) {
        val targetSourceMapFile = targetDir / sourceMapFile.name
        IO.copyFile(sourceMapFile, targetSourceMapFile)
        copiedFiles += targetSourceMapFile
        log.info(s"Copied SourceMap: ${sourceMapFile.name} to ${targetSourceMapFile.toString}")
      } else {
        log.warn(s"SourceMap file not found: ${sourceMapFile.toString}")
      }

      val targetJSFile = targetDir / jsFile.name
      IO.copyFile(jsFile, targetJSFile)
      copiedFiles += targetJSFile

      log.info(s"Looking for static resources in client/src/main/resources...")
      val clientStaticResources = (client / Compile / unmanagedResources).value

      clientStaticResources.foreach { staticFile =>
        if (staticFile.isFile) {
          val targetStaticFile = targetDir / staticFile.name
          try {
            IO.copyFile(staticFile, targetStaticFile)
            copiedFiles += targetStaticFile
            log.info(s"Copied static resource: ${staticFile.name} to ${targetStaticFile.toString}")
          } catch {
            case e: Exception => log.error(s"Could not copy ${staticFile.name}: ${e.getMessage}")
          }
        }
      }
      log.info("Resource Generator: Finished asset processing.")
      copiedFiles.result()
    }.taskValue,
  dockerBaseImage := "docker.io/library/eclipse-temurin:17-jre-alpine",
  dockerExposedPorts := Seq(10000),
  Docker / maintainer     := "Eric Smith",
  Docker / dockerUsername := Some("nestor9001"),
  Docker / packageName    := "reportflow-about-me",
  dockerRepository        := Some("quay.io"),
  dockerUpdateLatest      := true,
)

lazy val client = (project in file("client"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "aboutMe-client",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % "17.2.1"
    ),
    Compile / fastOptJS / artifactPath := target.value / "out" / "main.js",
    Compile / fullOptJS / artifactPath := target.value / "out" / "main.js"

  )