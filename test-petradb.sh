#!/bin/bash
set -e

echo "Running test suite against petradb-engine backend..."
(cd tests && OQL_BACKEND=petradb npm test)
