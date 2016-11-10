#!/bin/sh

DIRNAME=`dirname $0`

# start Derby Sync Client
java -Djava.util.logging.config.file=$DIRNAME/logging.properties -Ddirname.path=$DIRNAME/ -jar $DIRNAME/derbysyncclient.jar -UPDATE_DB_2_33_TO_APP_1_5_1
