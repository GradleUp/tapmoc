#!/usr/bin/env sh
set -e
set -x

# AGP 8 works best with Java 17.
# `JAVA_HOME_17_X64` is set on GitHub runners, fallback to the macOS JDK locator.
JAVA_17_HOME="${JAVA_HOME_17_X64:-$(/usr/libexec/java_home -v 17)}"

cd tests/agp8-java && ./gradlew build -Dorg.gradle.java.home="$JAVA_17_HOME"
cd ../agp8-kotlin && ./gradlew build -Dorg.gradle.java.home="$JAVA_17_HOME"
cd ../agp9-kmp && ./gradlew build
cd ../agp9-kotlin && ./gradlew build
cd ../java && ./gradlew build
cd ../jvm && ./gradlew build
cd ../wasm-js && ./gradlew build
cd ../gradle-plugin && ./gradlew build
cd ../kmp && ./gradlew build
