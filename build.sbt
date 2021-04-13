lazy val oql2 = crossProject( /*JSPlatform,*/ JVMPlatform /*, NativePlatform*/ )
  .in(file("."))
  .settings(
    name := "oql2",
    version := "2.0.0-snapshot.1",
    scalaVersion := "2.13.5",
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
    mainClass := Some("com.vinctus.oql2.Main"),
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.5" % "test",
    libraryDependencies += "xyz.hyperreal" %%% "table" % "1.0.0-snapshot.3", // % "test",
    libraryDependencies += "xyz.hyperreal" %%% "cross-platform" % "0.1.0-snapshot.3",
    publishMavenStyle := true,
    publishArtifact in Test := false,
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
    libraryDependencies += "xyz.hyperreal" %% "pretty" % "0.2" // % "test",
  ) /*.
//  nativeSettings(
//    nativeLinkStubs := true
//  ).
  jsSettings(
    jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv(),
//    Test / scalaJSUseMainModuleInitializer := true,
//    Test / scalaJSUseTestModuleInitializer := false,
    Test / scalaJSUseMainModuleInitializer := false,
    Test / scalaJSUseTestModuleInitializer := true,
    scalaJSUseMainModuleInitializer := true,
  )*/
