@echo off

echo Starting Derby Sync Client

set DIRNAME=%~dp0
cd %DIRNAME%
echo Set working dir: %DIRNAME%

set JP="%DIRNAME%derbySyncClient.jar"

java -Djava.util.logging.config.file="%DIRNAME%logging.properties" -Ddirname.path="%DIRNAME%./" -jar %JP% 

exit