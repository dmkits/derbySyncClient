/* обновление настроек БД для версии 1.5. */
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.user.APP','APP');

CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.user.APPSyncSrv','APPSyncSrv');

CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.connection.requireAuthentication','true');
