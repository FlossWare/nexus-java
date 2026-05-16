#!/bin/sh

./mvnw clean package -DskipTests
java -jar target/nexus-1.0-jar-with-dependencies.jar "$@"