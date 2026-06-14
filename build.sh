#!/bin/sh
# Compile the mod sources against the Starsector API and package jars/SlayTheSector.jar
set -e

STARSECTOR="/home/ubik/Downloads/starsector"
PROJECT="$(cd "$(dirname "$0")" && pwd)"
OUT_JAR="$PROJECT/jars/SlayTheSector.jar"

# Locate javac (JDK 17). Order: JAVA_HOME, then PATH.
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC="$JAVA_HOME/bin/javac"
elif command -v javac >/dev/null 2>&1; then
    JAVAC="javac"
else
    echo "ERROR: javac not found. Install a JDK 17:  sudo apt install openjdk-17-jdk" >&2
    exit 1
fi

# Classpath = every jar shipped in the install root (starfarer.api.jar etc.).
CP="$(printf '%s:' "$STARSECTOR"/*.jar)"

rm -rf "$PROJECT/bin"
mkdir -p "$PROJECT/bin" "$PROJECT/jars"

echo "Compiling with $JAVAC ..."
find "$PROJECT/src" -name '*.java' > "$PROJECT/bin/sources.txt"
"$JAVAC" -encoding UTF-8 --release 17 -classpath "$CP" -d "$PROJECT/bin" @"$PROJECT/bin/sources.txt"

echo "Packaging $OUT_JAR ..."
jar cf "$OUT_JAR" -C "$PROJECT/bin" com

echo "Build OK -> $OUT_JAR"
