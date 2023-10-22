#!/bin/bash

BASEDIR=$(realpath $(dirname "$0"))
PROJECTDIR=$(realpath "$BASEDIR/../..")

wget -q --spider http://google.com
if [ $? -ne 0 ]; then
  echo -e "\e[1;33mwarning: you are offline"
  echo -e "In order to build with maven, please, turn your network on."
  echo -e "Or use our prebuilt jar archive in '../../bin' directory.\e[0m"
  exit 0
fi

mvn clean package
if [ $? -ne 0 ]; then
  exit 1
fi
cp $BASEDIR/target/*-emp-interpreter.jar $PROJECTDIR/bin/
