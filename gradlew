#!/usr/bin/env bash

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolving links will be necessary if $0 is a symlink
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`/"$link"
    fi
done
SAVED="`pwd`"
CDPATH=""
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Use system gradle if available or run gradle via java
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi

# Fallback to wrapper JAR if present
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
if [ -f "$CLASSPATH" ]; then
    JAVACMD="java"
    if [ -n "$JAVA_HOME" ]; then
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    exec "$JAVACMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
fi

echo "Error: Could not find gradle or gradle-wrapper.jar" >&2
exit 1
