#!/bin/bash

BASEDIR=$(realpath $(dirname "$0"))
PROJECTDIR=$(realpath "$BASEDIR/../..")

mvn clean package
if [ $? -ne 0 ]; then
  exit 1
fi
cp $BASEDIR/target/*-emp-interpreter.jar $PROJECTDIR/bin/
