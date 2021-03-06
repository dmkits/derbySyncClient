@echo off

REM Update database for Derby Sync Client

echo Starting Derby Sync Client in mode database update (updating sync objects to v.1.5.1.)

set DIRNAME=%~dp0
cd %DIRNAME%
echo Set working dir: %DIRNAME%

set JP="%DIRNAME%derbysyncclient.jar"

java -Djava.util.logging.config.file="%DIRNAME%logging.properties" -Ddirname.path="%DIRNAME%./" -jar %JP% -UPDATE_DB_2_33_TO_APP_1_5_1

exit