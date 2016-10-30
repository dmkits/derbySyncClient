/*извлечение данных по ИД базы данных и ИД данных с сервера*/
select ID,UPDATEDATE,STATUS,APPLIEDDATE,MSG from APP.SYNCDATAIN where CHID=?