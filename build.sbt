name := "@vinctus/oql"//@vinctus/

version := "1.0.0-RC.3.14"

description := "Object Query Language"

scalaVersion := "2.13.6"

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-language:postfixOps", "-language:implicitConversions", "-language:existentials", "-language:dynamics")

organization := "com.vinctus"

githubOwner := "vinctustech"

githubRepository := "oql"

Global / onChangedBuildSource := ReloadOnSourceChanges

resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.githubPackages("vinctustech")

resolvers += Resolver.githubPackages("edadma", "importer")

resolvers += Resolver.githubPackages("edadma", "pretty")

enablePlugins(ScalaJSPlugin)

jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv()

enablePlugins(ScalablyTypedConverterPlugin)

stTypescriptVersion := "4.2.4"

stIgnore += "source-map-support"

Test / scalaJSUseMainModuleInitializer := true

Test / scalaJSUseTestModuleInitializer := false

Compile / npmDependencies ++= Seq(
  "pg" -> "8.5.1",
  "@types/pg" -> "7.14.9",
  "source-map-support" -> "0.5.19"
)

libraryDependencies ++= Seq(
  "org.scalatest" %%% "scalatest" % "3.2.5" % "test",
  "xyz.hyperreal" %%% "rdb-sjs" % "0.1.0-snapshot.7",
  "com.vinctus" %%% "sjs-utils" % "0.1.0-snapshot.23",
  "com.vinctus" %%% "mappable" % "0.1.1"
)

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %%% "scala-parser-combinators" % "2.0.0",
  //  "org.scala-lang.modules" %%% "scala-async" % "1.0.0-M1"
)

libraryDependencies ++= Seq(
  "io.github.cquiroz" %%% "scala-java-time" % "2.0.0"
)

mainClass := Some( "com.vinctus." + "oql" + ".Main" )

lazy val packageName = SettingKey[String]("packageName", "package name")

packageName := "oql"

publishMavenStyle := true

Test / publishArtifact := false

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
