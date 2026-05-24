#!/bin/bash

# JNexus Swing UI Launcher
# Launches the Swing-based graphical interface for Nexus Repository Manager

# Determine the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Find the latest JAR file (handles version updates automatically)
JAR_FILE=$(ls -t "$SCRIPT_DIR"/target/jnexus-*-jar-with-dependencies.jar 2>/dev/null | head -1)

# Check if JAR exists
if [ -z "$JAR_FILE" ] || [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found in $SCRIPT_DIR/target/"
    echo "Please build the project first:"
    echo "  ./mvnw clean package"
    exit 1
fi

echo "Using JAR: $JAR_FILE"

# Launch Swing UI
echo "Starting JNexus Swing UI..."
java --enable-preview -cp "$JAR_FILE" org.flossware.jnexus.JNexusSwing "$@"
