#!/bin/bash
set -e

echo "Building petradb backend..."
sbt '; project petradb; fullOptJS'

cp petradb/target/scala-3.8.2/scalajs-bundler/main/-vinctus-oql-petradb-opt.js petradb/npm/main.js
cp petradb/target/scala-3.8.2/scalajs-bundler/main/-vinctus-oql-petradb-opt.js.map petradb/npm/main.js.map

echo "petradb build complete → petradb/npm/"
