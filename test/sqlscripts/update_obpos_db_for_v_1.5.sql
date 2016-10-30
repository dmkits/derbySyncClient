select * from SYNCDATAIN;

/* обновление настроек БД для версии 1.5. */
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.user.APP','APP');
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.user.APPSyncSrv','APPSyncSrv');
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.connection.requireAuthentication','true');

DROP TABLE SYNCDATAIN;
CREATE TABLE SYNCDATAIN/*информация входящих от сервера данных и результате применения*/ (
  ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),/*ИД записи*/
  CHID VARCHAR(256) NOT NULL,/*ИД пересылаемых данных из таблицы синхронизации данных на отправку в базе сервера*/
  TABLENAME VARCHAR(256) NOT NULL,/*таблица в которой обновляются или в которую встявляются данные*/
  KEYNAME VARCHAR(256) NOT NULL,/*имя поля ключа данных*/
  KEYVAL VARCHAR(256) NOT NULL,/*значение ключа данных*/
  CRDATE TIMESTAMP,/*дата-время создания записи на сервере*/
  UPDATEDATE TIMESTAMP,/*дата-время обновления записи (обновление статуса и сообщения)*/
  STATUS INTEGER,/*статус: 0 -данные приняты-записаны в таблицу синхронизации, но не обработаны-применены, 1 -приняты и применены, -1 -приняты, но не применены-в процессе применения возникла ошибка*/
  ATTEMPTSCOUNT INTEGER NOT NULL,/*кол-во попыток применения данных*/
  APPLIEDDATE TIMESTAMP,/*дата-время применения данных*/
  MSG VARCHAR(1024),/*сообщение клиента о применении или ошибке применения*/
  PRIMARY KEY(ID)
);

