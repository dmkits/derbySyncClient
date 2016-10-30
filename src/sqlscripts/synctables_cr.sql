CREATE TABLE APP.SYNCDBPROPS/*базы данных между которыми осуществляется обмен*/ (
    ID INTEGER NOT NULL,/*ИД базы данных*/
    DBNAME VARCHAR(256) NOT NULL,/*имя базы данных*/
    POSNAME VARCHAR(256) NOT NULL,/*имя POS-терминала*/
    STOCKNAME VARCHAR(256) NOT NULL,/*имя склада*/
    PRIMARY KEY(ID)
);
INSERT INTO APP.SYNCDBPROPS(ID, DBNAME, POSNAME, STOCKNAME) VALUES (1, 'Bistro4', 'Касса Бистро славянка', 'Торговый зал Бистро Славянка');

CREATE TABLE APP.SYNCDATAOUT/*информация о пересылаемых серверу данных и результате пересылки*/ (
    ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),/*ИД записи*/
    TABLENAME VARCHAR(256) NOT NULL,/*таблица из которой извлекаются данные*/
    OTYPE VARCHAR(1) NOT NULL,/*тип операции: I -вставка (insert), U -обновление (update), D -удаление (delete)*/
    TABLEKEY1IDNAME VARCHAR(256) NOT NULL,/*имя поля 1-го ключа данных*/
    TABLEKEY1IDVAL VARCHAR(256) NOT NULL,/*значение 1-го ключа данных*/
    TABLEKEY2IDNAME VARCHAR(256),/*имя поля 2-го ключа данных*/
    TABLEKEY2IDVAL VARCHAR(256),/*значение 2-го ключа данных*/
    CRDATE TIMESTAMP,/*дата-время создания записи*/
    UPDATEDATE TIMESTAMP,/*дата-время обновления записи (обновление статуса и сообщения)*/
    STATUS INTEGER,/*статус: null -данные не приняты на сервере, 0 -данные приняты на сервере, но не обработаны, 1 -приняты и обработаны, -1 -приняты, но не обработаны-в процессе обработки возникла ошибка*/
    ATTEMPTSCOUNT INTEGER NOT NULL,/*кол-во попыток применения данных на сервере*/
    APPLIEDDATE TIMESTAMP,/*дата-время применения данных на сервере*/
    MSG VARCHAR(1024),/*сообщение с сервера*/
    PRIMARY KEY(ID)
);

CREATE TABLE APP.SYNCDATAIN/*информация входящих от сервера данных и результате применения*/ (
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

