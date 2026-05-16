#!/bin/sh
# JNexus UI - Terminal interface for Nexus Repository Manager
# Requires: ncurses library installed (libncurses-dev on Debian/Ubuntu)

# Check if JAR exists
JAR="target/jnexus-1.0-jar-with-dependencies.jar"
if [ ! -f "$JAR" ]; then
    echo "Building project..."
    ./mvnw clean package -DskipTests
fi

# Run the UI with preview features and native access enabled
exec java --enable-preview --enable-native-access=ALL-UNNAMED \
    -cp "$JAR" \
    org.flossware.jnexus.JNexusUI
