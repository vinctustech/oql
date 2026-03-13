#!/bin/bash
set -e

echo "Running test suite against pg backend..."
(cd tests && OQL_BACKEND=pg npm test)
