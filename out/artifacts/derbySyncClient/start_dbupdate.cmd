@echo off

REM
REM
REM
REM
REM
REM

set DIRNAME=%~dp0

set JP="%DIRNAME%derbySyncClient.jar"

java -Djava.util.logging.config.file="%DIRNAME%logging.properties" -Ddirname.path="%DIRNAME%./" -jar %JP% -CREATE_DB_SYNC_OBJECTS