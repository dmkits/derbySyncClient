/* Обновление статуса исходящих данных, отправляемых на сервер.
   Обновляются поля: статус, сообщение, дата обновления, кол-во попыток, дата применения
   в таблице исходящих данных синхронизации по ID записи. */
update APP.SYNCDATAOUT
    set Status=?, Msg=?, UPDATEDATE=?, ATTEMPTSCOUNT=ATTEMPTSCOUNT+1, APPLIEDDATE=?
    where ID=?
