#!/bin/sh

# Build if needed
if [ ! -d "target" ] || [ -z "$(ls target/jnexus-*-jar-with-dependencies.jar 2>/dev/null)" ]; then
    echo "Building project..."
    ./mvnw clean package -DskipTests
fi

# Find the latest JAR
JAR=$(ls -t target/jnexus-*-jar-with-dependencies.jar 2>/dev/null | head -1)

if [ -z "$JAR" ]; then
    echo "Error: No JAR file found in target/"
    exit 1
fi

echo "Using JAR: $JAR"
java -jar "$JAR" "$@"