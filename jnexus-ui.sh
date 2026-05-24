#!/bin/sh
# JNexus UI - Terminal interface for Nexus Repository Manager
# Requires: ncurses library installed (libncurses-dev on Debian/Ubuntu)

# Find the latest JAR (handles version updates automatically)
JAR=$(ls -t target/jnexus-*-jar-with-dependencies.jar 2>/dev/null | head -1)

if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
    echo "Building project..."
    ./mvnw clean package -DskipTests
    JAR=$(ls -t target/jnexus-*-jar-with-dependencies.jar 2>/dev/null | head -1)
fi

echo "Using JAR: $JAR"

# Run the UI with preview features and native access enabled
exec java --enable-preview --enable-native-access=ALL-UNNAMED \
    -cp "$JAR" \
    org.flossware.jnexus.JNexusUI
