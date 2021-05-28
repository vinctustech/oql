ThisBuild / licenses += "ISC" -> url("https://opensource.org/licenses/ISC")

lazy val oql = crossProject(JSPlatform, JVMPlatform /*, NativePlatform*/ )
  .in(file("."))
  .enablePlugins(ScalablyTypedConverterPlugin)
  .settings(
    name := "@vinctus/oql",
    version := "1.0.0-RC.1",
    scalaVersion := "2.13.6",
    scalacOptions ++=
      Seq(
        "-deprecation",
        "-feature",
        "-unchecked",
        "-language:postfixOps",
        "-language:implicitConversions",
        "-language:existentials",
        "-language:dynamics",
        "-language:reflectiveCalls",
        "-Xasync"
      ),
    organization := "com.vinctus",
    githubOwner := "vinctustech",
    githubRepository := "oql",
    resolvers += Resolver.githubPackages("vinctustech"),
//    resolvers += Resolver.githubPackages("edadma", "rdb-sjs"),
    mainClass := Some("com.vinctus.oql.Main"),
    Test / mainClass := Some("com.vinctus.oql.Main"),
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.5" % "test",
    Test / parallelExecution := false,
    libraryDependencies += "org.scala-lang.modules" %%% "scala-parser-combinators" % "2.0.0",
//    libraryDependencies += "xyz.hyperreal" %%% "table" % "1.0.0-snapshot.3", // % "test",
//    libraryDependencies += "xyz.hyperreal" %%% "cross-platform" % "0.1.0-snapshot.3",
//    libraryDependencies += "xyz.hyperreal" %%% "importer" % "0.1.0-snapshot.1",
//    libraryDependencies += "xyz.hyperreal" %%% "rdb-sjs" % "0.1.0-snapshot.6",
    publishMavenStyle := true,
    publishTo := Some("GitHub Package Registry" at "https://maven.pkg.github.com/vinctustech/oql"),
    Test / publishArtifact := false,
    licenses += "ISC" -> url("https://opensource.org/licenses/ISC")
  )
  .jvmSettings(
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0" % "provided",
    libraryDependencies += "com.h2database" % "h2" % "1.4.200",
    libraryDependencies += "org.postgresql" % "postgresql" % "42.2.19",
    libraryDependencies ++= Seq(
      "org.antlr" % "antlr4-runtime" % "4.7.2",
      "org.antlr" % "ST4" % "4.3.1"
    ),
    libraryDependencies += "xyz.hyperreal" %% "pretty" % "0.2.1" // % "test",
  )
  .
//  nativeSettings(
//    nativeLinkStubs := true
//  ).
  jsSettings(
  jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv(),
  libraryDependencies += "com.vinctus" %%% "sjs-utils" % "0.1.0-snapshot.23",
  Compile / npmDependencies ++= Seq(
    "pg" -> "8.5.1",
    "@types/pg" -> "7.14.9"
  ),
  Test / scalaJSUseMainModuleInitializer := true,
  Test / scalaJSUseTestModuleInitializer := false,
//  Test / scalaJSUseMainModuleInitializer := false,
//  Test / scalaJSUseTestModuleInitializer := true,
  scalaJSUseMainModuleInitializer := true
)
