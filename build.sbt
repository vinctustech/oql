name := "@vinctus/oql"

version := "0.1.61"

description := "Object Query Language"

scalaVersion := "2.13.4"

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-language:postfixOps", "-language:implicitConversions", "-language:existentials", "-language:dynamics")

organization := "com.vinctus"

githubOwner := "vinctustech"

githubRepository := "oql"

//githubTokenSource := TokenSource.GitConfig("github.token")

Global / onChangedBuildSource := ReloadOnSourceChanges

resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += "Hyperreal Repository" at "https://dl.bintray.com/edadma/maven"

enablePlugins(ScalaJSPlugin)

enablePlugins(ScalablyTypedConverterPlugin)

Test / scalaJSUseMainModuleInitializer := true

Test / scalaJSUseTestModuleInitializer := false

jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv()

npmDependencies in Compile ++= Seq(
  "pg" -> "8.5.1",
  "@types/pg" -> "7.14.9"
)

libraryDependencies ++= Seq(
  "org.scalatest" %%% "scalatest" % "3.2.3" % "test",
  "xyz.hyperreal" %%% "rdb-sjs" % "0.1.0-snapshot.5" % "test",
  "com.vinctus" %%% "sjs-utils" % "0.1.0-snapshot.17"
)

libraryDependencies ++= Seq(
	"org.scala-lang.modules" %%% "scala-parser-combinators" % "1.1.2",
//  "org.scala-lang.modules" %%% "scala-async" % "1.0.0-M1"
)

libraryDependencies ++= Seq(
  "io.github.cquiroz" %%% "scala-java-time" % "2.0.0"
)

mainClass in (Compile, run) := Some( "com.vinctus." + "oql" + ".Main" )

lazy val packageName = SettingKey[String]("packageName", "package name")

packageName := "oql"

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses := Seq("ISC" -> url("https://opensource.org/licenses/ISC"))

homepage := Some(url("https://github.com/vinctustech/" + packageName.value))

pomExtra :=
  <scm>
    <url>git@github.com:vinctustech/{packageName.value}.git</url>
    <connection>scm:git:git@github.com:vinctustech/{packageName.value}.git</connection>
  </scm>
  <developers>
    <developer>
      <id>edadma</id>
      <name>Edward A. Maxedon, Sr.</name>
      <url>https://github.com/edadma</url>
    </developer>
  </developers>
