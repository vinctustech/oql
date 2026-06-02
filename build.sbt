lazy val commonSettings = Seq(
  scalaVersion := "3.8.2",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-language:existentials",
    "-language:dynamics",
    "-explain",
  ),
  organization := "com.vinctus",
  resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/",
  scalaJSLinkerConfig ~= { _.withESFeatures(_.withESVersion(org.scalajs.linker.interface.ESVersion.ES2021)) },
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false },
  licenses := Seq("ISC" -> url("https://opensource.org/licenses/ISC")),
  homepage := Some(url("https://github.com/vinctustech/oql")),
  pomExtra :=
    <scm>
      <url>git@github.com:vinctustech/oql.git</url>
      <connection>scm:git:git@github.com:vinctustech/oql.git</connection>
    </scm>
      <developers>
        <developer>
          <id>edadma</id>
          <name>Edward A. Maxedon, Sr.</name>
          <url>https://github.com/edadma</url>
        </developer>
      </developers>
)

lazy val root = project.in(file("."))
  .aggregate(core, pg, petradb)
  .enablePlugins(ParadoxPlugin)
  .enablePlugins(ParadoxMaterialThemePlugin)
  .settings(
    name := "oql",
    publish / skip := true,
    Compile / paradox / target := baseDirectory.value / "docs",
    Compile / paradoxMaterialTheme := {
      ParadoxMaterialTheme()
        .withColor("teal", "indigo")
        .withFavicon("assets/images/favicon.ico")
        .withLogo("assets/images/vinctus.png")
        .withRepository(uri("https://github.com/vinctustech/oql"))
        .withCopyright("© Vinctus Technologies Inc. All Rights Reserved 2021")
        .withSocial(uri("https://github.com/vinctustech"))
        .withLanguage(java.util.Locale.ENGLISH)
        .withSearch(tokenizer = "[\\s\\-\\.]+")
    },
    Compile / paradoxProperties ++=
      Map(
        "image.base_url" -> ".../assets/images",
      ),
  )

lazy val core = project.in(file("core"))
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings)
  .settings(
    name := "oql-core",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-parser-combinators" % "2.1.1",
      "io.github.cquiroz"      %%% "scala-java-time"          % "2.6.0",
      "com.lihaoyi"            %%% "pprint"                   % "0.9.3",
    ),
  )

lazy val pg = project.in(file("pg"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name := "@vinctus/oql-pg",
    version := "1.4.2-alpha.2",
    scalaJSUseMainModuleInitializer := true,
    jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv(),
    Compile / npmDependencies ++= Seq(
      "pg"                 -> "8.13.1",
      "source-map-support" -> "0.5.21",
    ),
//    Test / scalaJSUseMainModuleInitializer := true,
//    Test / scalaJSUseTestModuleInitializer := false,
//    libraryDependencies ++= Seq(
//      "org.scalatest" %%% "scalatest" % "3.2.19" % "test",
//    ),
  )

lazy val petradb = project.in(file("petradb"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name := "@vinctus/oql-petradb",
    version := "1.4.2-alpha.2",
    scalaJSUseMainModuleInitializer := false,
    jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv(),
    libraryDependencies ++= Seq(
      "io.github.edadma" %%% "petradb-engine" % "1.5.4",
    ),
  )
