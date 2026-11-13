#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/build.sh
java -ea -cp out/main:out/test dev.aduproof.AllTests
