import ReleaseTransformations._
import sbtcrossproject.{crossProject, CrossType}
import microsites._

enablePlugins(ScalaJSPlugin)

mimaFailOnNoPrevious in ThisBuild := false
val mimaPreviousVersion = "1.0.0"

val defaultSettings = Seq(
  scalaVersion := "2.11.12",
  crossScalaVersions := List("2.11.12", "2.12.8", "2.13.0"),
  resolvers += Resolver.sonatypeRepo("releases"),
  homepage := Some(url("http://monovore.com/decline")),
  organization := "com.monovore",
  scalacOptions ++= Seq("-deprecation", "-feature", "-language:higherKinds"),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) =>
        Seq("-Xfatal-warnings")
      case _ =>
        Nil
    }
  },
  licenses += ("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  Global / PgpKeys.gpgCommand := "/usr/bin/gpg2",
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/bkirwi/decline"),
      "git@github.com:bkirwi/decline.git"
    )
  ),
  pomExtra := (
    <developers>
      <developer>
        <id>bkirwi</id>
        <name>Ben Kirwin</name>
        <url>http://ben.kirw.in/</url>
      </developer>
    </developers>
  ),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := Some(
    if (isSnapshot.value) Opts.resolver.sonatypeSnapshots
    else Opts.resolver.sonatypeStaging
  ),
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    releaseStepCommand("sonatypeReleaseAll"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

val catsVersion = "2.0.0"

lazy val root =
  project.in(file("."))
    .aggregate(declineJS, declineJVM, refinedJS, refinedJVM, effectJS, effectJVM, enumeratumJS, enumeratumJVM, doc)
    .settings(defaultSettings)
    .settings(noPublishSettings)

lazy val decline =
  crossProject(JSPlatform, JVMPlatform).in(file("core"))
    .settings(defaultSettings)
    .settings(addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3" cross CrossVersion.binary))
    .settings(
      name := "decline",
      description := "Composable command-line parsing for Scala",
      libraryDependencies ++= Seq(
        "org.typelevel"  %%% "cats-core"            % catsVersion,
        "org.typelevel"  %%% "cats-laws"            % catsVersion % "test",
        "org.typelevel"  %%% "discipline-scalatest" % "1.0.0-M1"  % "test"
      ),
    )
    .jvmSettings(
      mimaPreviousArtifacts := Set(organization.value %% moduleName.value % mimaPreviousVersion),
    )
    .jsSettings(
      libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.0.0-RC3"
    )

lazy val declineJVM = decline.jvm
lazy val declineJS = decline.js

lazy val bench =
  project.in(file("bench"))
    .enablePlugins(JmhPlugin)
    .dependsOn(declineJVM, refinedJVM)
    .settings(defaultSettings)
    .settings(noPublishSettings)
    .settings(fork := true)

lazy val refined =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("refined"))
    .settings(defaultSettings)
    .settings(
      name := "refined",
      moduleName := "decline-refined",
      libraryDependencies ++= {
        val refinedVersion = "0.9.10"

        Seq(
          "eu.timepit" %%% "refined"            % refinedVersion,
          "eu.timepit" %%% "refined-scalacheck" % refinedVersion % "test"
        )
      }
    )
    .dependsOn(decline % "compile->compile;test->test")
    .jvmSettings(
      mimaPreviousArtifacts := Set(organization.value %% moduleName.value % mimaPreviousVersion),
    )

lazy val refinedJVM = refined.jvm
lazy val refinedJS = refined.js

lazy val effect =
  crossProject(JSPlatform, JVMPlatform).in(file("effect"))
    .settings(defaultSettings)
    .settings(
      name := "effect",
      moduleName := "decline-effect",
      libraryDependencies ++= Seq(
        "org.typelevel" %%% "cats-effect" % catsVersion
      )
    )
    .dependsOn(decline % "compile->compile;test->test")
    .jvmSettings(
      mimaPreviousArtifacts := Set(organization.value %% moduleName.value % mimaPreviousVersion),
    )

lazy val effectJVM = effect.jvm
lazy val effectJS = effect.js

lazy val enumeratum =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("enumeratum"))
    .settings(defaultSettings)
    .settings(
      name := "enumeratum",
      moduleName := "decline-enumeratum",
      libraryDependencies += "com.beachape" %%% "enumeratum" % "1.5.15"
    )
    .dependsOn(decline % "compile->compile;test->test")
    .jvmSettings(
      mimaPreviousArtifacts := Set(organization.value %% moduleName.value % mimaPreviousVersion),
    )

lazy val enumeratumJVM = enumeratum.jvm
lazy val enumeratumJS = enumeratum.js

lazy val doc =
  project.in(file("doc"))
    .dependsOn(declineJVM, refinedJVM, effectJVM, enumeratumJVM)
    .enablePlugins(MicrositesPlugin)
    .settings(defaultSettings)
    .settings(noPublishSettings)
    .settings(
      micrositeName := "decline",
      micrositeDescription := "Composable command-line parsing for Scala",
      micrositeConfigYaml := microsites.ConfigYml(
        yamlInline = """kramdown: { input: GFM, hard_wrap: false }"""
      ),
      micrositeBaseUrl := "/decline",
      micrositeGithubOwner := "bkirwi",
      micrositeGithubRepo := "decline",
      micrositeGitterChannel := false,
      micrositeShareOnSocial := false,
      micrositeHighlightTheme := "solarized-light",
      micrositeDocumentationUrl := "usage.html",
      micrositePalette := Map(
        "brand-primary"     -> "#B58900",
        "brand-secondary"   -> "#073642",
        "brand-tertiary"    -> "#002b36",
        "gray-dark"         -> "#453E46",
        "gray"              -> "#837F84",
        "gray-light"        -> "#E3E2E3",
        "gray-lighter"      -> "#F4F3F4",
        "white-color"       -> "#fdf6e3"
      ),
      mdocVariables := Map(
        "DECLINE_VERSION" -> "1.0.0",
      ),
      micrositeCompilingDocsTool := WithMdoc,
      mdocIn := tutSourceDirectory.value,
    )
