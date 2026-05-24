#!/bin/sh
APP_BASE_NAME=$(basename "$0")
APP_HOME=$(cd "$(dirname "$0")" && pwd)
exec "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
