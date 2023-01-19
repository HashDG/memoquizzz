#!/bin/bash
export SOURCES=src
export CLASSES=classes
export CLASSPATH=`find lib -name "*.jar" | tr '\n' ':'`

javac -cp ${CLASSPATH} -sourcepath ${SOURCES} -d ${CLASSES} $@ `find src -not -path '*/.*' -type f -name '*.java'`
cp -r ressources/ classes/
