#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p out/main out/test
find src/main/java -name '*.java' | sort > out/main-sources.txt
javac -encoding UTF-8 -Xlint:all -Werror -d out/main @out/main-sources.txt
cp -R src/main/resources/. out/main/
find src/test/java -name '*.java' | sort > out/test-sources.txt
javac -encoding UTF-8 -Xlint:all -cp out/main -d out/test @out/test-sources.txt
printf 'Compiled Java 21 sources successfully.\n'
