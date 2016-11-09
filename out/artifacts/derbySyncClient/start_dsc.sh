#!/bin/sh

DIRNAME=`dirname $0`

# start Derby Sync Client
java -Djava.util.logging.config.file=$DIRNAME/logging.properties -Ddirname.path=$DIRNAME/ -jar $DIRNAME/derbysyncclient.jar
