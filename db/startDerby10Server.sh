#!/bin/sh

export DERBY_HOME=/home/dmkits/derby10

export DERBY_OPTS='-Duser.language=en -Dderby.drda.debug=false -d64 -Xms1024m -Xmx1g'

cd /home/dmkits/derby_dbs

sh $DERBY_HOME/bin/startNetworkServer
