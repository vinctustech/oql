Installation
============

### TypeScript/JavaScript

There is a [Node.js](https://nodejs.org/en/) module available through the [npm registry](https://www.npmjs.com/).

Install using the [npm install](https://docs.npmjs.com/downloading-and-installing-packages-locally) command:

```bash
npm install @vinctus/oql
```

TypeScript declarations are included in the package.

### Scala.js

There is a [Scala.js](https://www.scala-js.org/) library available through [Github Packages](https://github.com/features/packages).

Add the following lines to your `build.sbt`:

```sbt
externalResolvers += "OQL" at "https://maven.pkg.github.com/vinctustech/oql"

libraryDependencies += "com.vinctus" %%% "-vinctus-oql" % "1.1.9"

Compile / npmDependencies ++= Seq(
  "pg" -> "8.10.0",
  "@types/pg" -> "8.6.6",
  "source-map-support": "0.5.21"
)
```
