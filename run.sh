#!/bin/bash
cp -r ressources/* classes/
cd classes
export CLASSPATH=`find ../lib -name "*.jar" | tr '\n' ':'`
java -cp ${CLASSPATH}:. -Xss1G $@
cd ..

