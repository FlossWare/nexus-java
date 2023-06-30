#!/bin/sh

#MAVEN_OPTS="--spring.config.location=/home/sfloess/.flossware/nexus/*"

#./mvnw clean install $* exec:java -Dexec.mainClass=org.flossware.nexus.Nexus

./mvnw clean install $*
java -jar target/nexus-1.0.jar