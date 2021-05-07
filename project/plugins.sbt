addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.6")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.5.1")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
//addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.0.0")
//addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.0")

addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta32")

resolvers += Resolver.bintrayRepo("edadma", "sbt-plugins")
addSbtPlugin("xyz.hyperreal" % "npm-plugin" % "0.1.13")

addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.2")
