#!/bin/bash
set -e

echo "Running test suite against petradb backend..."
(cd tests && OQL_BACKEND=petradb npm test)
