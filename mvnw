#!/bin/sh
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Apache Maven Wrapper startup script, version 3.3.2

set -euf
[ "${MVNW_VERBOSE-}" != true ] || set -x

# Resolve base directory (portable)
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

# Locate Java executable
if [ -n "${JAVA_HOME-}" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
  if [ ! -x "$JAVACMD" ]; then
    echo "JAVA_HOME is set but java not found at: $JAVACMD" >&2
    exit 1
  fi
else
  JAVACMD="java"
fi

WRAPPER_JAR="$BASE_DIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

# Download maven-wrapper.jar if not present
if [ ! -f "$WRAPPER_JAR" ]; then
  WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
  if [ -f "$WRAPPER_PROPERTIES" ]; then
    while IFS='=' read -r key value; do
      value="${value%$'\r'}"
      case "$key" in wrapperUrl) WRAPPER_URL="$value" ;; esac
    done < "$WRAPPER_PROPERTIES"
  fi
  echo "Downloading maven-wrapper.jar..." >&2
  if command -v curl > /dev/null 2>&1; then
    curl --fail --silent --show-error --location "$WRAPPER_URL" -o "$WRAPPER_JAR"
  elif command -v wget > /dev/null 2>&1; then
    wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL"
  else
    echo "Cannot download maven-wrapper.jar: curl or wget required." >&2
    exit 1
  fi
fi

# MavenWrapperMain discovers maven-wrapper.properties via maven.multiModuleProjectDirectory
exec "$JAVACMD" \
  ${JAVA_OPTS:+$JAVA_OPTS} \
  -classpath "$WRAPPER_JAR" \
  "-Dmaven.multiModuleProjectDirectory=$BASE_DIR" \
  org.apache.maven.wrapper.MavenWrapperMain \
  "$@"
