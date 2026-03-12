#!/bin/bash
set -e

echo "Building petradb-engine backend..."
sbt '; project petradb-engine; fullOptJS'

cp petradb-engine/target/scala-3.8.2/scalajs-bundler/main/-vinctus-oql-petradb-engine-opt.js petradb-engine/npm/main.js
cp petradb-engine/target/scala-3.8.2/scalajs-bundler/main/-vinctus-oql-petradb-engine-opt.js.map petradb-engine/npm/main.js.map

echo "petradb-engine build complete → petradb-engine/npm/"
