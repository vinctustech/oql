#!/bin/bash
set -e

echo "Building pg backend..."
sbt '; project pg; fullOptJS'

cp pg/target/scala-3.8.2/scalajs-bundler/main/-vinctus-oql-pg-opt.js pg/npm/main.js
cp pg/target/scala-3.8.2/scalajs-bundler/main/-vinctus-oql-pg-opt.js.map pg/npm/main.js.map

(cd pg/npm && npm install --silent)

echo "pg build complete → pg/npm/"
