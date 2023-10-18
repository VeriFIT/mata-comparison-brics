#!/bin/bash
basedir=$(realpath $(dirname "$0"))

mvn clean package
cp $basedir/target/*-emp-interpreter.jar ~/bin/
