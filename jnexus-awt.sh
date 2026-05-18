#!/bin/bash

# JNexus AWT UI Launcher
# Launches the AWT-based graphical interface for Nexus Repository Manager

# Determine the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Path to the JAR file
JAR_FILE="$SCRIPT_DIR/target/jnexus-1.0-jar-with-dependencies.jar"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please build the project first:"
    echo "  ./mvnw clean package"
    exit 1
fi

# Launch AWT UI
echo "Starting JNexus AWT UI..."
java --enable-preview -cp "$JAR_FILE" org.flossware.jnexus.JNexusAWT "$@"
