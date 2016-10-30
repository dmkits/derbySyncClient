@echo off

REM Update database for Derby Sync Client

echo Starting Derby Sync Client in mode database update (creating sync objects)

set DIRNAME=%~dp0

set JP="%DIRNAME%DerbySyncClient.jar"

java -Djava.util.logging.config.file="%DIRNAME%logging.properties" -Ddirname.path="%DIRNAME%./" -jar %JP% -CREATE_DB_SYNC_OBJECTS