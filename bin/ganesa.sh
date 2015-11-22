#!/bin/bash

JAVA=java

ARGS="$@"
PRG="$0"
PRGDIR=`dirname $PRG`

# JAVA options
# You can set JVM additional options here if you want
if [ -z "$JVM_OPTS" ]; then
    JVM_OPTS="-server -Xms128m -Xmx512m -XX:MetaspaceSize=250m -XX:MaxMetaspaceSize=250m -XX:ReservedCodeCacheSize=150m -XX:+UseConcMarkSweepGC -XX:SoftRefLRUPolicyMSPerMB=50 -ea -Dsun.io.useCanonCaches=false -Djava.net.preferIPv4Stack=true -Dawt.useSystemAAFontSettings=lcd -XX:+TieredCompilation"
fi
# Set up security options
SECURITY_OPTS="-Djava.security.debug=failure"

export JAVA_OPTS="$SECURITY_OPTS $JAVA_OPTS $JVM_OPTS"

for JAVA_EXE in "${JAVA}" "${JAVA_HOME}/bin/java" "${JAVA_HOME}/Home/bin/java" "/usr/bin/java" "/usr/local/bin/java"
do
  if [ -x "$JAVA_EXE" ]
  then
    break
  fi
done

if [ ! -x "$JAVA_EXE" ]
then
  echo "Unable to locate Java. Please set JAVA_HOME environment variable."
  exit
fi

# start ganesa
echo "Starting ganesa"
cd "$PRGDIR"
exec "$JAVA_EXE" $JAVA_OPTS -jar lib/ganesa.jar $ARGS &
