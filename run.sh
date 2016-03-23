#!/bin/sh
#To Debug
export MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,address=5005,server=y,suspend=n"
/usr/bin/mvn3 jetty:run-war
