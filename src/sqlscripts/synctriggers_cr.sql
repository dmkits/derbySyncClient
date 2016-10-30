/*триггеры регистрации записей в таблице информации исходящих данных синхронизации для версии 1.5.*/
CREATE TRIGGER APP.TICKETS_SYNC_REG_INS
    AFTER INSERT ON APP.TICKETS
    REFERENCING NEW_TABLE AS NEW_TABLE
    FOR EACH STATEMENT 
        insert into APP.SYNCDATAOUT (OTYPE, TABLENAME, TABLEKEY1IDNAME, TABLEKEY1IDVAL, CRDATE, ATTEMPTSCOUNT)
        select 'I','APP.TICKETS','ID',ID,CURRENT_TIMESTAMP,0 from NEW_TABLE where SESSION_USER<>'APPSYNCSRV';

CREATE TRIGGER APP.TICKETS_SYNC_REG_UPD
    AFTER UPDATE ON APP.TICKETS
    REFERENCING NEW_TABLE AS NEW_TABLE
    FOR EACH STATEMENT 
        insert into APP.SYNCDATAOUT (OTYPE, TABLENAME, TABLEKEY1IDNAME, TABLEKEY1IDVAL, CRDATE, ATTEMPTSCOUNT)
        select 'U','APP.TICKETS','ID',ID,CURRENT_TIMESTAMP,0 from NEW_TABLE where SESSION_USER<>'APPSYNCSRV';

CREATE TRIGGER APP.TICKETS_SYNC_REG_DEL
    AFTER DELETE ON APP.TICKETS
    REFERENCING OLD_TABLE AS OLD_TABLE
    FOR EACH STATEMENT 
        insert into APP.SYNCDATAOUT (OTYPE, TABLENAME, TABLEKEY1IDNAME, TABLEKEY1IDVAL, CRDATE, ATTEMPTSCOUNT)
        select 'D','APP.TICKETS','ID',ID,CURRENT_TIMESTAMP,0 from OLD_TABLE where SESSION_USER<>'APPSYNCSRV';


CREATE TRIGGER APP.TICKETLINES_SYNC_REG_INS
    AFTER INSERT ON APP.TICKETLINES
    REFERENCING NEW_TABLE AS NEW_TABLE
    FOR EACH STATEMENT  
        insert into APP.SYNCDATAOUT (OTYPE, TABLENAME, TABLEKEY1IDNAME, TABLEKEY1IDVAL, TABLEKEY2IDNAME, TABLEKEY2IDVAL, CRDATE, ATTEMPTSCOUNT)
        select 'I','APP.TICKETLINES','TICKET',TICKET,'LINE',CHAR(LINE),CURRENT_TIMESTAMP,0 from NEW_TABLE where SESSION_USER<>'APPSYNCSRV';

CREATE TRIGGER APP.TICKETLINES_SYNC_REG_UPD
    AFTER UPDATE ON APP.TICKETLINES
    REFERENCING NEW_TABLE AS NEW_TABLE
    FOR EACH STATEMENT 
        insert into APP.SYNCDATAOUT (OTYPE, TABLENAME, TABLEKEY1IDNAME, TABLEKEY1IDVAL, TABLEKEY2IDNAME, TABLEKEY2IDVAL, CRDATE, ATTEMPTSCOUNT)
        select 'U','APP.TICKETLINES','TICKET',TICKET,'LINE',CHAR(LINE),CURRENT_TIMESTAMP,0 from NEW_TABLE where SESSION_USER<>'APPSYNCSRV';

CREATE TRIGGER APP.TICKETLINES_SYNC_REG_DEL
    AFTER DELETE ON APP.TICKETLINES
    REFERENCING OLD_TABLE AS OLD_TABLE
    FOR EACH STATEMENT 
        insert into APP.SYNCDATAOUT (OTYPE, TABLENAME, TABLEKEY1IDNAME, TABLEKEY1IDVAL, TABLEKEY2IDNAME, TABLEKEY2IDVAL, CRDATE, ATTEMPTSCOUNT)
        select 'D','APP.TICKETLINES','TICKET',TICKET,'LINE',CHAR(LINE),CURRENT_TIMESTAMP,0 from OLD_TABLE where SESSION_USER<>'APPSYNCSRV';


CREATE TRIGGER APP.RECEIPTS_SYNC_REG_INS
    AFTER INSERT ON APP.RECEIPTS
    REFERENCING NEW_TABLE AS NEW_TABLE
    FOR EACH STATEMENT 
        insert into APP.SYNCDATAOUT (OTYPE, TABLENAME, TABLEKEY1IDNAME, TABLEKEY1IDVAL, CRDATE, ATTEMPTSCOUNT)
        select 'I','APP.RECEIPTS','ID',ID,CURRENT_TIMESTAMP,0 from NEW_TABLE where SESSION_USER<>'APPSYNCSRV';

CREATE TRIGGER APP.RECEIPTS_SYNC_REG_UPD
    AFTER UPDATE ON APP.RECEIPTS
    REFERENCING NEW_TABLE AS NEW_TABLE
    FOR EACH STATEMENT 
        insert into APP.SYNCDATAOUT (OTYPE, TABLENAME, TABLEKEY1IDNAME, TABLEKEY1IDVAL, CRDATE, ATTEMPTSCOUNT)
        select 'U','APP.RECEIPTS','ID',ID,CURRENT_TIMESTAMP,0  from NEW_TABLE where SESSION_USER<>'APPSYNCSRV';

CREATE TRIGGER APP.RECEIPTS_SYNC_REG_DEL
    AFTER DELETE ON APP.RECEIPTS
    REFERENCING OLD_TABLE AS OLD_TABLE
    FOR EACH STATEMENT 
        insert into APP.SYNCDATAOUT (OTYPE, TABLENAME, TABLEKEY1IDNAME, TABLEKEY1IDVAL, CRDATE, ATTEMPTSCOUNT)
        select 'D','APP.RECEIPTS','ID',ID,CURRENT_TIMESTAMP,0  from OLD_TABLE where SESSION_USER<>'APPSYNCSRV';


CREATE TRIGGER APP.PAYMENTS_SYNC_REG_INS
    AFTER INSERT ON APP.PAYMENTS
    REFERENCING NEW_TABLE AS NEW_TABLE
    FOR EACH STATEMENT 
        insert into APP.SYNCDATAOUT (OTYPE, TABLENAME, TABLEKEY1IDNAME, TABLEKEY1IDVAL, CRDATE, ATTEMPTSCOUNT)
        select 'I','APP.PAYMENTS','ID',ID,CURRENT_TIMESTAMP,0  from NEW_TABLE where SESSION_USER<>'APPSYNCSRV';

CREATE TRIGGER APP.PAYMENTS_SYNC_REG_UPD
    AFTER UPDATE ON APP.PAYMENTS
    REFERENCING NEW_TABLE AS NEW_TABLE
    FOR EACH STATEMENT 
        insert into APP.SYNCDATAOUT (OTYPE, TABLENAME, TABLEKEY1IDNAME, TABLEKEY1IDVAL, CRDATE, ATTEMPTSCOUNT)
        select 'U','APP.PAYMENTS','ID',ID,CURRENT_TIMESTAMP,0  from NEW_TABLE where SESSION_USER<>'APPSYNCSRV';

CREATE TRIGGER APP.PAYMENTS_SYNC_REG_DEL
    AFTER DELETE ON APP.PAYMENTS
    REFERENCING OLD_TABLE AS OLD_TABLE
    FOR EACH STATEMENT 
        insert into APP.SYNCDATAOUT (OTYPE, TABLENAME, TABLEKEY1IDNAME, TABLEKEY1IDVAL, CRDATE, ATTEMPTSCOUNT)
        select 'D','APP.PAYMENTS','ID',ID,CURRENT_TIMESTAMP,0  from OLD_TABLE where SESSION_USER<>'APPSYNCSRV';
