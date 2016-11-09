@echo off

REM Update database for Derby Sync Client

echo Starting Derby Sync Client in mode database update (updating sync objects to v.1.5.)

set DIRNAME=%~dp0

set JP="%DIRNAME%DerbySyncClient.jar"

java -Djava.util.logging.config.file="%DIRNAME%logging.properties" -Ddirname.path="%DIRNAME%./" -jar %JP% -UPDATE_DB_1_5