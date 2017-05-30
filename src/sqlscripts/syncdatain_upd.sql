/*обновление статуса и данных по результату применения входящих данных синхронизации*/
update APP.SYNCDATAIN set STATUS=?, ATTEMPTSCOUNT=ATTEMPTSCOUNT+1, UPDATEDATE=?, APPLIEDDATE=?, MSG='Data applied.'
where CHID=? and TABLENAME=? and KEYNAME=? and KEYVAL=?