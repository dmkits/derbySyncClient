/*запись данных в таблицу входящих данных синхронизации*/
insert into APP.SYNCDATAIN(CHID, TABLENAME, KEYNAME, KEYVAL, CRDATE, STATUS, ATTEMPTSCOUNT, MSG)
    values(?, ?, ?, ?, ?, 0, 0, 'Data recieved.')