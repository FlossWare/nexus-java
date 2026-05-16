#!/bin/sh

./mvnw clean package -DskipTests
java -jar target/jnexus-1.0-jar-with-dependencies.jar "$@"