#!/bin/bash
javac -classpath "../lib/*" -d . `find ../src -name '*.java' -print`
echo "Build nimbus_server"
jar cvfm ../bin/nimbus_server.jar nimbus_server.mf com
echo "Build nimbus_client"
jar cvfm ../bin/nimbus_client.jar nimbus_client.mf com
