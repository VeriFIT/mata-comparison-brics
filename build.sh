#!/bin/bash
basedir=$(realpath $(dirname "$0"))

mvn clean package
if [ $? -ne 0 ]; then
  exit 1
fi
cp $basedir/target/*-emp-interpreter.jar ~/bin/
