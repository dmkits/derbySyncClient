#!/bin/bash

DIRNAME=`dirname $0`

cd "$(dirname "$(readlink -fn "$0")")"

xterm -e java -Djava.util.logging.config.file=$DIRNAME/logging.properties -Ddirname.path=$DIRNAME/ -jar derbysyncclient.jar