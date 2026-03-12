#!/bin/bash
set -e

echo "Running pg test suite..."
(cd tests && npm test)
