#!/bin/bash
javac -classpath "../lib/*" -d . `find ../src -name '*.java' -print`
echo "Build server"
jar cvfm ../bin/server.jar server.mf com
echo "Build client"
jar cvfm ../bin/client.jar client.mf com
