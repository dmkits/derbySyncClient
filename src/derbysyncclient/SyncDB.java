/* derbysyncclient
 * @autor dmk.dp.ua 2014-04-15
 */
package derbysyncclient;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** SyncDB
 * Операции в синхронизируемой базе данных.
 * Создание, обновление объектов синхронизации в базе данных клиента.
 * Создание доп. настроек в базе данных.
 * Скрипты создания читаются из ресурсов в пакете sqlscripts.
 * @author dmk.dp.ua 2014-04-15 */
public class SyncDB {
    
    private static final Logger logger = Logger.getLogger("derbysyncclient.SyncDB");

    public static String POS_ClientSyncName= "POS.clientSyncName";

    private static final String ID= "ID";
    private static final String CRDATE= "CRDATE";
    private static final String TABLENAME= "TABLENAME";
    private static final String OTYPE= "OTYPE";
    private static final String TABLEKEY1IDNAME= "TABLEKEY1IDNAME";
    private static final String TABLEKEY1IDVAL= "TABLEKEY1IDVAL";
    private static final String TABLEKEY2IDNAME= "TABLEKEY2IDNAME";
    private static final String TABLEKEY2IDVAL= "TABLEKEY2IDVAL";

    public static String SERVER_SYNC_DATA_ID = "ServerSyncDataID";
    public static String SERVER_CREATE_DATE = "ServerCreateDate";

    /* Регистрация драйвера базы данных. */
    public static void regDBDriver(String sDBDriverLib, String sDBDriver) throws Exception {
        try {
            ClassLoader cloader =
                    new URLClassLoader(new URL[] {new File(sDBDriverLib).toURI().toURL()});
            DriverManager.registerDriver(
                    (Driver) Class.forName(sDBDriver, true, cloader).newInstance());
            logger.log(Level.INFO, "Database driver registered.");
        } catch (Exception e) {
            throw new Exception("FAILED to register database driver! "+e.getMessage());
        }
    }
    /* Подключение к базе данных. */
    public static Session connectToDB(String sDBUrl, String sDBUser, String sDBPassword) throws Exception {
        try {
            Session dbSession = new Session(); //
            dbSession.setConnectParams(sDBUrl, sDBUser, sDBPassword);
            dbSession.connect();
            logger.log(Level.INFO, "Connected to database: \"" + sDBUrl + "\"");
            return dbSession;
        } catch (Exception e) {
            throw new Exception("FAILED to connect to database: \""+sDBUrl+"\"! "+e.getMessage());
        }
    }

    /* Создание объектов синхронизации в базе данных. Создаются таблицы синхронизации и триггеры синхронизации. */
    public static void createSyncObjects(Session poDBSession) throws Exception {
        logger.log(Level.INFO, "----------Creating sync objects in database----------");
        int[] isRes = {0,0};
        String sQuery_upddb= TextFromResource.load("/sqlscripts/update_db_1_5.sql"); //чтение sql-скрипта из ресурса - обновление настроек бд
        execSript(poDBSession, sQuery_upddb, isRes); //выполнение скрипта
        String sQuery_cr= TextFromResource.load("/sqlscripts/synctables_cr.sql") ; //чтение sql-скрипта из ресурса - создание таблиц синхр.
        execSript(poDBSession, sQuery_cr, isRes); //выполнение скрипта
        String sQuery_cr2= TextFromResource.load("/sqlscripts/synctriggers_cr.sql"); //чтение sql-скрипта из ресурса - создание триггеров синхр.
        execSript(poDBSession, sQuery_cr2, isRes); //выполнение скрипта
        logger.log(Level.WARNING, "UPDATE RESULT: statements to execute {0}, executed {1}", new Object[]{isRes[0], isRes[1]});
        logger.log(Level.INFO, "----------Sync objects created in database.----------");
    }
    
    /* выполнение скрипта. в скрипте отдельные запросы разделены знаками ; 
     * возвращаемые значения: кол-во запросов в скрипте и кол-во удачно выполненных запросов */
    private static void execSript(Session poDBSession, String psScriptQuery, int[] pisRes) throws Exception {
        //разбор строки с sql-скриптом на отдельные запросы, разделенные ";" и поочередное выпонение полученных запросов
        StringTokenizer sST = new StringTokenizer(psScriptQuery,";");
        while (sST.hasMoreTokens()) {
            String sQuery= sST.nextToken();
            if (sQuery==null || sQuery.equals("") || sQuery.equals("\n")) { break; }
            pisRes[0]++;
            //подготовка обработчика запросов к БД и выполнение запроса-создания объектов синхронизации в базе данных
            try {
                Statement voSt=poDBSession.getConnection().createStatement();
                logger.log(Level.INFO, "Executing statement {0}: {1}", new Object[]{pisRes[0],sQuery});
                voSt.execute(sQuery);
                pisRes[1]++;
                voSt.close();
                logger.log(Level.INFO, "Statement {0} executed.", pisRes[0]);
            } catch (Exception e) {
                logger.log(Level.WARNING, "FAILED to execute database statement {0}! {1}", new Object[]{pisRes[0],e.getMessage()});
            }
        }
    }
    
    /* обновление настроек и объектов базы данных предыдущих версий до v.1.5. */
    public static void updateDB_1_5(Session poDBSession) throws Exception {
        logger.log(Level.INFO, "----------Updating database to DerbySyncClient v.1.5.----------");
        int[] isRes = {0,0};
        String sQuery_upddb= TextFromResource.load("/sqlscripts/update_db_1_5.sql"); //обновление настроек бд
        execSript(poDBSession, sQuery_upddb, isRes); //выполнение скрипта
        String sQuery_deltr= TextFromResource.load("/sqlscripts/synctriggers_dr.sql"); //удаление триггеров
        execSript(poDBSession, sQuery_deltr, isRes); //выполнение скрипта
        String sQuery_crtrg= TextFromResource.load("/sqlscripts/synctriggers_cr.sql"); //создание новых триггеров
        execSript(poDBSession, sQuery_crtrg, isRes); //выполнение скрипта
        logger.log(Level.WARNING, "UPDATE RESULT: statements to execute {0}, executed {1}", new Object[]{isRes[0], isRes[1]});
        logger.log(Level.INFO, "----------Database updated to DerbySyncClient v.1.5.----------");
    }
    /* обновление настроек и объектов базы данных предыдущих версий до v.1.5. */
    public static void updateDB_2_33_TO_APP_1_5_1(Session poDBSession) throws Exception {
        logger.log(Level.INFO, "----------Updating database v.2.33 to DerbySyncClient v.1.5.1.----------");
        int[] isRes = {0,0};
        String sQuery_upddb= TextFromResource.load("/sqlscripts/update_db_1_5_1.sql"); //обновление
        execSript(poDBSession, sQuery_upddb, isRes); //выполнение скрипта
        logger.log(Level.WARNING, "UPDATE RESULT: statements to execute {0}, executed {1}", new Object[]{isRes[0], isRes[1]});
        logger.log(Level.INFO, "----------Database updated to DerbySyncClient v.1.5.1.----------");
    }

    public static HashMap<String,String> getPOSClientInformation(Session dbs) throws Exception {
        try {
            String sQuery= TextFromResource.load("/sqlscripts/syncClientName_sel.sql");
            HashMap<String, String> result = new HashMap<>();
            Statement voSt=dbs.getConnection().createStatement();
            ResultSet voRS= voSt.executeQuery(sQuery);
            String sPOSClientSyncName = null;
            if (voRS.next()) {
                Blob blob = voRS.getBlob("CONTENT");
                if (blob!=null && blob.length()>0){
                    byte[] bdata = blob.getBytes(1, (int) blob.length());
                    sPOSClientSyncName = new String(bdata);
                }
                voSt.close();
            } else {
                voSt.close();
                throw new Exception("FAILED to get POS client information! Reason: cannot finded POS client information in database!");
            }
            if (sPOSClientSyncName==null)
                throw new Exception("FAILED to get POS client information! POS client sync name no setted!");
            result.put(POS_ClientSyncName, sPOSClientSyncName);
            logger.log(Level.INFO, "Getted POS client information from database: POS.clientSyncName={0}",new String[]{sPOSClientSyncName});
            return result;
        } catch (Exception e) {
            throw new Exception("FAILED to get POS client information! Reason: "+e.getMessage());
        }
    }

    /* Получение данных первой не отправленной записи из БД из таблицы с информацией о данных синхронизации. */
    public static HashMap<String,String> getSyncDataOutItem(Session dbs) throws Exception {
        try {
            HashMap<String,String> result= new HashMap<>();
            Statement voSt=dbs.getConnection().createStatement();
            String sQuery= TextFromResource.load("/sqlscripts/syncdataout_sel.sql");
            ResultSet voRS= voSt.executeQuery(sQuery);
            if (!voRS.next()) {
                logger.log(Level.INFO, "No output sync data for send to server.");
                voSt.close();
                return null;
            }
            String sID=voRS.getString("ID");
            result.put(ID,sID);
            result.put(CRDATE,voRS.getString("CRDATE"));
            result.put(TABLENAME,voRS.getString("TABLENAME"));
            result.put(OTYPE,voRS.getString("OTYPE"));
            result.put(TABLEKEY1IDNAME,voRS.getString("TABLEKEY1IDNAME"));
            result.put(TABLEKEY1IDVAL,voRS.getString("TABLEKEY1IDVAL"));
            result.put(TABLEKEY2IDNAME,voRS.getString("TABLEKEY2IDNAME"));
            result.put(TABLEKEY2IDVAL, voRS.getString("TABLEKEY2IDVAL"));
            voSt.close();
            logger.log(Level.INFO, "Getted output sync data from database: ID={0} \n {1}",new Object[]{sID,result});
            return result;
        } catch (Exception e) {
            throw new Exception("FAILED to get output sync data from database! "+e.getLocalizedMessage());
        }
    }
    public static HashMap<String,String> getOutSyncDataValues(Session dbs, HashMap<String,String> outputSyncData) throws Exception {
        PreparedStatement voPSt;
        ResultSet voRS2 = null;
        try { //---выборка из БД данных синхронизации---
            //---получаем все колонки таблицы sTableName по ИД sIDName=sIDVal---
            String sQuery="select * from "+outputSyncData.get(TABLENAME)+" where "+outputSyncData.get(TABLEKEY1IDNAME)+"=?";
            if (outputSyncData.get(TABLEKEY2IDNAME)!=null)
                sQuery= sQuery + " and "+outputSyncData.get(TABLEKEY2IDNAME)+"=?";
            voPSt=dbs.getConnection().prepareStatement(sQuery);
            logger.log(Level.FINE, "Preparing parameter 1 to database statement (" + outputSyncData.get(TABLEKEY1IDVAL) + ").");
            voPSt.setString(1, outputSyncData.get(TABLEKEY1IDVAL)); //sKey1IDVal
            if (outputSyncData.get(TABLEKEY2IDNAME)!=null) {
                logger.log(Level.FINE, "Preparing parameter 2 to database statement (" + outputSyncData.get(TABLEKEY2IDVAL) + ").");
                voPSt.setString(2, outputSyncData.get(TABLEKEY2IDVAL));//sKey2IDVal
            }
            voRS2= voPSt.executeQuery();
        } catch (Exception e) {
            throw new Exception("FAILED to read out sync data values from database! "+e.getMessage());
        }
        try {
            HashMap<String,String> result= new HashMap<>();
            voRS2.next();
            //---поочередно все колонки (имя и значение) из запроса сохраняем---
            for (int i=1;i<=voRS2.getMetaData().getColumnCount();i++) {
                String sColName= voRS2.getMetaData().getColumnName(i); //column name
                if (!"ATTRIBUTES".equals(sColName)){
                    String sColVal= voRS2.getString(i); // column value
                    if (sColVal==null) {sColVal="";}
                    result.put(sColName, sColVal);
                }
            }
            voPSt.close();
            logger.log(Level.INFO, "Getted output sync data values from database: \n {0}", new Object[]{result});
            return result;
        } catch (Exception e) {
            throw new Exception("FAILED to get out sync data values! "+e.getMessage());
        }
    }
    public static boolean updSyncDataOutStateStoreOnServer(Session dbs, String sID, HashMap<String, Object> serverResult) throws Exception{
        boolean result= false;
        String sStatus = null, sMsg= null;
        Object appliedDate = null;
        if(serverResult==null){
        } else if(serverResult.get("error")!=null){
            sMsg = "Server Sync Service Failed to store! Reason:"+serverResult.get("error").toString();
        } else if(serverResult.get("resultItem")==null){
            sMsg = "Server Sync Service Failed to store! Reason: no result item!";
        } else {
            HashMap<String,Object> serverResultItem= (HashMap)serverResult.get("resultItem");
            Object srvState = serverResultItem.get("STATE");
            if(srvState!=null&& ("0".equals(srvState.toString())||"1".equals(srvState.toString()))) {
                sStatus=srvState.toString();
                appliedDate= serverResultItem.get("APPLIED_DATE");
                result= true;
            } else if(srvState!=null) {
                sStatus=srvState.toString();
            }
            Object srvMsg = serverResultItem.get("MSG");
            sMsg= (srvMsg!=null)?srvMsg.toString():"!NO SERVER STORE RESULT MESSAGE!";
        }
        try { //---подготовка обработчика запросов к БД и выполнение запроса-обновления данных в таблице синхронизации данных на отправку---
            updOutData(dbs, sID, sStatus, sMsg, appliedDate);
            logger.log(Level.INFO, "Updated output sync data status and message: status={0}, msg={1}", new Object[]{sStatus,sMsg});
            return result;
        } catch (Exception e) {
            throw new Exception("FAILED to upd sync out data state store on server! Reason:"+e.getMessage());
        }
    }
    /* Получение данных первой не примененной записи из БД из таблицы с информацией о данных синхронизации. */
    public static HashMap<String,String> getNotAppliedOutputSyncData(Session dbs) throws Exception {
        try {
            HashMap<String,String> result= new HashMap<>();
            Statement voSt=dbs.getConnection().createStatement();
            String sQuery= TextFromResource.load("/sqlscripts/syncdataoutwithstatus_sel.sql");
            ResultSet voRS= voSt.executeQuery(sQuery);
            if (!voRS.next()) {
                logger.log(Level.INFO, "No not applied output sync data for send to server.");
                return null;
            }
            String sID=voRS.getString("ID");
            result.put("ID", sID);
            logger.log(Level.INFO, "Getted not applied output sync data from database:{0}",result);
            return result;

        } catch (Exception e) {
            throw new Exception("FAILED to get not applied output sync data from database! "+e.getMessage());
        }
    }
    public static boolean updOutDataStateApplyOnServer(Session dbs, String sID, HashMap<String,Object> serverResult) throws Exception {
        boolean result=false;
        String sStatus = "-1", sMsg= null;
        Object appliedDate=null;
        if(serverResult==null){
        } else if(serverResult.get("error")!=null){
            sMsg = "Server Sync Service Failed to apply! Reason:"+serverResult.get("error").toString();
        } else if(serverResult.get("resultItem")==null){
            sMsg = "Server Sync Service Failed to apply! Reason: no result item!";
        } else {
            HashMap<String,Object> serverResultItem= (HashMap)serverResult.get("resultItem");
            Object srvState = serverResultItem.get("STATE");
            if(srvState!=null&&"1".equals(srvState.toString())) {
                sStatus="1"; result= true;
            }else if(srvState!=null) {
                sStatus=srvState.toString();
            }
            Object srvMsg = serverResultItem.get("MSG");
            sMsg= (srvMsg!=null)?srvMsg.toString():"!NO SERVER APPLY RESULT MESSAGE!";
            appliedDate= serverResultItem.get("APPLIED_DATE");
        }
        try {//выполнение запроса-обновления данных в таблице синхронизации данных на отправку
            updOutData(dbs, sID, sStatus, sMsg, appliedDate);
            logger.log(Level.INFO, "Updated output sync data status and message: status={0}, msg={1}, AppliedDate={2}", new Object[]{sStatus, sMsg, appliedDate});
            return result;
        } catch (Exception e) {
            throw new Exception("FAILED upd sync out data state apply on server! Reason:"+e.getMessage());
        }
    }

    public static void updOutData(Session dbs, String sID, String sStatus, String sMsg, Object appliedDate) throws Exception {
        String sQuery_upd= TextFromResource.load("/sqlscripts/syncdataout_upd.sql"); //чтение sql-запроса из ресурса
        PreparedStatement voPSt=dbs.getConnection().prepareStatement(sQuery_upd);
        voPSt.setString(1,sStatus); //Status
        voPSt.setString(2, sMsg); //Msg
        voPSt.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis()));//UPDATEDATE
        if (appliedDate==null||"".equals(appliedDate.toString())) { //APPLIEDDATE
            voPSt.setNull(4, java.sql.Types.TIMESTAMP);
        } else {
            voPSt.setString(4,appliedDate.toString());
        }
        voPSt.setString(5, sID); //ID
        voPSt.executeUpdate();
        voPSt.close();
    }
//    /*Запись входящих данных синхронизации в таблицу входящих данных синхронизации.*/
//    private void recSyncDataIn() throws Exception {
//        String sID = null;
//        logger.log(Level.INFO, "Selecting sync data from syncdatain table.");
//        String sQuery_sel= TextFromResource.load("/sqlscripts/syncdatain_sel.sql");
//        try {
//            PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery_sel);
//            //logger.log(Level.INFO, "Executing syncClientName_sel.sql.");
//            voPSt.setString(1, cvoDataFromResponse.get(SERVER_SYNC_DATA_ID));
//            ResultSet voRS = voPSt.executeQuery();
//            if (voRS.next()) { sID = voRS.getString("ID"); }
//            voPSt.close();
//        } catch (Exception e) {
//            throw new Exception("FAILED to get sync data from syncdatain table! "+e.getMessage());
//        }
//        if (sID!=null) {//удаление старой записи
//            logger.log(Level.INFO, "Deleting sync data from syncdatain table.");
//            String sQuery_ins= TextFromResource.load("/sqlscripts/syncdatain_del.sql");
//            try {
//                PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery_ins);
//                voPSt.setString(1,sID);
//                voPSt.executeUpdate();
//                voPSt.close();
//            } catch (Exception e) {
//                throw new Exception("FAILED to delete sync data from syncdatain table! "+e.getMessage());
//            }
//        }
//        logger.log(Level.INFO, "Inserting sync data in into syncdatain table.");
//        String sQuery_ins= TextFromResource.load("/sqlscripts/syncdatain_ins.sql");
//        try {
//            PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery_ins);
//            voPSt.setString(1, cvoDataFromResponse.get(SERVER_SYNC_DATA_ID));
//            voPSt.setString(2, cvoDataFromResponse.get(TABLE_NAME));
//            voPSt.setString(3, cvoDataFromResponse.get(TABLE_KEY1_NAME));
//            voPSt.setString(4, cvoDataFromResponse.get(TABLE_KEY1_VALUE));
//            voPSt.setString(5, cvoDataFromResponse.get(SERVER_CREATE_DATE));
//            voPSt.executeUpdate();
//            voPSt.close();
//        } catch (Exception e) {
//            throw new Exception("FAILED to insert get sync data into syncdatain table! "+e.getMessage());
//        }
//    }
//
//    /*Применение входящих данных синхронизации.*/
//    private void applySynDataIn(SOAPMessage response) throws Exception {
//        boolean bExists = false;
//        String sTable = cvoDataFromResponse.get(TABLE_NAME);
//        String sKeyName = cvoDataFromResponse.get(TABLE_KEY1_NAME);
//        String sKeyVal = cvoDataFromResponse.get(TABLE_KEY1_VALUE);
//        try {
//            String sQuery = "select "+sKeyName+" from "+sTable+" where "+sKeyName+" = ?";
//            PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery);
//            voPSt.setString(1,sKeyVal);
//            ResultSet voRS = voPSt.executeQuery();
//            if (voRS.next()) { bExists=true; }
//            voPSt.close();
//        } catch (Exception e) {
//            throw new Exception("FAILED to select sync data key! "+e.getMessage());
//        }
//        if (bExists) {//update data
//            try {
//                logger.log(Level.INFO,"Applying sync data (updating).");
//                String sFieldsSet = null;
//                @SuppressWarnings("rawtypes") java.util.Iterator it = response.getAttachments();
//                while (it.hasNext()) {//подготовка списка полей
//                    AttachmentPart attachment = (AttachmentPart)it.next();
//                    String sDataName= attachment.getContentId();
//                    //String sDataVal= ""+attachment.getContent();
//                    //logger.log(Level.INFO,"Attachment: SyncDataName:" + sDataName + " SyncDataVal:" + sDataVal); // !!!IT'S FOR TESTING!!!
//                    if (sFieldsSet==null) { sFieldsSet=sDataName+"=?"; } else { sFieldsSet=sFieldsSet+","+sDataName+"=?"; }
//                }
//                String sQuery = "update "+sTable+" set "+sFieldsSet+" where "+sKeyName+"=?";
//                logger.log(Level.INFO,"Updating query: " + sQuery + "."); // !!!IT'S FOR TESTING!!!
//                PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery);
//                @SuppressWarnings("rawtypes") java.util.Iterator it2 = response.getAttachments();
//                int i = 0;
//                while (it2.hasNext()) {//подготовка параметров для полей
//                    AttachmentPart attachment = (AttachmentPart)it2.next();
//                    String sDataVal= ""+attachment.getContent();
//                    i++;
//                    //logger.log(Level.INFO,"Parameter:" + i + " value:" + sDataVal); // !!!IT'S FOR TESTING!!!
//                    voPSt.setString(i,sDataVal);
//                }
//                voPSt.setString(i+1,sKeyVal);
//                voPSt.executeUpdate();
//                voPSt.close();
//            } catch (Exception e) {
//                throw new Exception("FAILED to apply sync data! "+e.getMessage());
//            }
//        } else {//insert data
//            try {
//                logger.log(Level.INFO,"Applying sync data (inserting).");
//                String sFields = null;
//                String sFieldsVal = null;
//                @SuppressWarnings("rawtypes") java.util.Iterator it = response.getAttachments();
//                while (it.hasNext()) {//подготовка списка полей
//                    AttachmentPart attachment = (AttachmentPart)it.next();
//                    String sDataName= attachment.getContentId();
//                    //String sDataVal= ""+attachment.getContent();
//                    //logger.log(Level.INFO,"Attachment: SyncDataName:" + sDataName + " SyncDataVal:" + sDataVal); // !!!IT'S FOR TESTING!!!
//                    if (sFields==null) { sFields=sDataName; sFieldsVal="?"; } else { sFields=sFields+","+sDataName; sFieldsVal=sFieldsVal+",?"; }
//                }
//                String sQuery = "insert into "+sTable+" ("+sKeyName+","+sFields+") values(?,"+sFieldsVal+")";
//                logger.log(Level.INFO,"Inserting query: " + sQuery + "."); // !!!IT'S FOR TESTING!!!
//                PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery);
//                voPSt.setString(1,sKeyVal);
//                @SuppressWarnings("rawtypes") java.util.Iterator it2 = response.getAttachments();
//                int i = 0;
//                while (it2.hasNext()) {//подготовка параметров для полей
//                    AttachmentPart attachment = (AttachmentPart)it2.next();
//                    String sDataVal= ""+attachment.getContent().toString();
//                    i++;
//                    //logger.log(Level.INFO,"Parameter:" + (i+1) + " value:" + sDataVal); // !!!IT'S FOR TESTING!!!
//                    voPSt.setString(1+i,sDataVal);
//                }
//                voPSt.executeUpdate();
//                voPSt.close();
//            } catch (Exception e) {
//                throw new Exception("FAILED to apply sync data! "+e.getMessage());
//            }
//        }
//    }
//
//    /*Обновление статуса и данных в таблице входящих данных синхронизации по результату применения данных.*/
//    private void updStatus() throws Exception {
//        logger.log(Level.INFO, "Updating sync data status in syncdatain table.");
//        String sQuery_ins= TextFromResource.load("/sqlscripts/syncdatain_upd.sql");
//        try {
//            PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery_ins);
//            //logger.log(Level.INFO, "Executing syncClientName_sel.sql.");
//            voPSt.setString(1, "1");//Status
//            voPSt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));//UPDATEDATE
//            voPSt.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis()));//APPLIEDDATE
//            voPSt.setString(4, cvoDataFromResponse.get(SERVER_SYNC_DATA_ID));
//            voPSt.setString(5, cvoDataFromResponse.get(TABLE_NAME));
//            voPSt.setString(6, cvoDataFromResponse.get(TABLE_KEY1_NAME));
//            voPSt.setString(7, cvoDataFromResponse.get(TABLE_KEY1_VALUE));
//            voPSt.executeUpdate();
//            voPSt.close();
//        } catch (Exception e) {
//            throw new Exception("FAILED to update sync data status in syncdatain table! "+e.getMessage());
//        }
//    }
//    /*Формирование сообщения-запроса на сервер с данными о результате приема и применения данных с сервера.*/
//    public SOAPMessage getRequestWithResult() throws Exception {
//        logger.log(Level.INFO, "----------Prepare request message with result recieved and applied data from server----------");
//        logger.log(Level.INFO, "Selecting sync data from syncdatain table.");
//        String sQuery_sel= TextFromResource.load("/sqlscripts/syncdatain_sel.sql");
//        try {
//            PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery_sel);
//            //logger.log(Level.INFO, "Executing syncClientName_sel.sql.");
//            voPSt.setString(1, cvoDataFromResponse.get(SERVER_SYNC_DATA_ID));
//            ResultSet voRS = voPSt.executeQuery();
//            if (voRS.next()) {
//                cvoDataForRequest.put(SYNC_DATA_ID,voRS.getString("ID"));
//                cvoDataForRequest.put("Status",voRS.getString("Status"));
//                cvoDataForRequest.put("AppliedDate",voRS.getString("APPLIEDDATE"));
//                cvoDataForRequest.put("Msg",voRS.getString("Msg"));
//            } else {
//                throw new Exception("FAILED to get sync data from syncdatain table! No data!");
//            }
//            voPSt.close();
//        } catch (Exception e) {
//            throw new Exception("FAILED to get sync data from syncdatain table! "+e.getMessage());
//        }
//        //---создание сообщения, заполнение заголовка и тела сообщения данными с информацией об отправляемых данных---
//        logger.log(Level.INFO, "-----Creating SOAP request message-----");
//        SOAPMessage request = null;
//        try{
//            request = crMsg( //создание SOAP сообщения-запроса с данными заголовка и тела
//                    MsgHeaderVal.REQUEST_FROM_CLIENT_TO_UPD_SATATUS_SERVER_DATA_MSG_NAME, //заголовок сообщения
//                    cvoDataForRequest.get(POS_CLIENT_SYNC_NAME), //тело сообщения
//                    new String[] {SERVER_SYNC_DATA_ID,cvoDataFromResponse.get(SERVER_SYNC_DATA_ID)},
//                    new String[] {"Status",cvoDataForRequest.get("Status")},
//                    new String[] {"AppliedDate",cvoDataForRequest.get("AppliedDate")},
//                    new String[] {SYNC_DATA_ID,cvoDataForRequest.get(SYNC_DATA_ID)},
//                    new String[] {"Msg",cvoDataForRequest.get("Msg")} );
//        } catch (Exception e) {
//            throw new Exception("FAILED to create SOAP request message! "+e.getMessage());
//        }
//        return request;
//    }

    /* удаление примененных данных из БД. Удаляются данные и записи журнала исходящих данных синхронизации. */
    public static void delApplSyncData(Session poDBSession, int piTermStorageDay) throws Exception {
        //чтение sql-запроса из ресурса
        logger.log(Level.INFO, "----------Deleting applied data----------");
        /* получение списка ID последних при закрытии чека операций отправленных и примененных записей синхронизации */
        String sQuery_sel= TextFromResource.load("/sqlscripts/syncdataout_sel_applied.sql") ;
        PreparedStatement oPSt = poDBSession.getConnection().prepareStatement(sQuery_sel);
        Calendar cgCalendar = new GregorianCalendar();//cur datetime
        cgCalendar.add(Calendar.DAY_OF_YEAR, -piTermStorageDay); //cur datetime - piTermStorageDay days
        oPSt.setDate(1, (new java.sql.Date(cgCalendar.getTime().getTime())) ); //cur datetime - piTermStorageDay days
        ResultSet oRS = null;
        try {
            oRS= oPSt.executeQuery();
        } catch (Exception e) {
            throw new Exception("FAILED to get applied sync information data from database! "+e.getMessage());
        }
        while (true) { //цикл по ID
            int iID_End = 0;
            try {
                if (oRS.next()) {
                    iID_End = oRS.getInt("ID");
                    logger.log(Level.INFO, "Got ID group applied sync information from database. ID: {0}", iID_End);
                } else {
                    logger.log(Level.INFO, "No applied sync infomation for delete in database.");
                    break; //выход
                }
            } catch (Exception e) {
                throw new Exception("FAILED to get ID group applied sync information from database! "+e.getMessage());
            } 
            // удаление группы записей с iID_End
            delGroupAppData(poDBSession, iID_End);
        }
        oPSt.close();
    }
    
    /* удаление группы записей отправленных и примененных данных синхронизации начиная с последнего ID группы */
    private static void delGroupAppData(Session poDBSession, int piID_End) throws Exception {

        logger.log(Level.INFO, "------Deleting group applied data------");
        
        /* получение списка ID отправленных и примененных записей синхронизации в обратном порядке начиная с параметра piID_End */
        String sQuery_sel= TextFromResource.load("/sqlscripts/syncdataout_sel_deleting.sql") ;
        PreparedStatement oPSt = poDBSession.getConnection().prepareStatement(sQuery_sel);
        oPSt.setInt(1,piID_End); //End ID
        ResultSet oRS = null;
        try {
            oRS= oPSt.executeQuery();
        } catch (Exception e) {
            throw new Exception("FAILED to get group sync information for delete from database! "+e.getMessage());
        }
        while (true) { //цикл по ID записей группы
            int iID = 0;
            String sTable, sKey1Field, sKey1Val, sKey2Field, sKey2Val;
            try {
                if (oRS.next()) {
                    iID = oRS.getInt("ID");
                    sTable = oRS.getString("TABLENAME");
                    sKey1Field = oRS.getString("TABLEKEY1IDNAME");
                    sKey1Val = oRS.getString("TABLEKEY1IDVAL");
                    sKey2Field = oRS.getString("TABLEKEY2IDNAME");
                    sKey2Val = oRS.getString("TABLEKEY2IDVAL");
                    logger.log(Level.INFO, "Got ID applied sync information for delete from database. ID: {0}", iID);
                } else {
                    break; //выход
                }
            } catch (Exception e) {
                throw new Exception("FAILED to get ID applied sync information for delete from database! "+e.getMessage());
            } 
            // удаление данных
            if (sTable.equals("APP.RECEIPTS")) { 
                delData(poDBSession, "APP.TAXLINES", "RECEIPT", sKey1Val, null, null); }
            delData(poDBSession, sTable, sKey1Field, sKey1Val, sKey2Field, sKey2Val);
            // удаление исходящих данных синхронизации с iID
            delSyncOutData(poDBSession, iID);
        }
        oPSt.close();
    }
    
    /* удаление данных из таблицы psTable, по ключу psKey1Field со значением psKey1Val и доп.клучу psKey2Field со значением psKey2Val */
    private static void delData(Session poDBSession, String psTable, 
            String psKey1Field, String psKey1Val, String psKey2Field, String psKey2Val) throws Exception {
        
        logger.log(Level.INFO, "---Deleting applied data ({0})---",psTable);
        /* получение списка ID отправленных и примененных записей синхронизации в обратном порядке начиная с параметра piID_End */
        String sQuery_del= "delete from "+psTable+" where "+psKey1Field+"=?";
        if (psKey2Field!=null) { sQuery_del=sQuery_del+" and "+psKey2Field+"=?"; }
        PreparedStatement oPSt = poDBSession.getConnection().prepareStatement(sQuery_del);
        oPSt.setString(1,psKey1Val); 
        if (psKey2Field!=null) { oPSt.setString(2,psKey2Val); }
        //ResultSet oRS = null;
        try {
            oPSt.executeUpdate();
            oPSt.close();
        } catch (Exception e) {
            throw new Exception("FAILED to delete data for from database! "+e.getMessage());
        }
    }

    /* удаление данных из таблицы исходящих данных синхронизации по ключу piID */
    private static void delSyncOutData(Session poDBSession, int piID) throws Exception {
        
        logger.log(Level.INFO, "---Deleting sync data (ID={0})---",piID);
        /* получение списка ID отправленных и примененных записей синхронизации в обратном порядке начиная с параметра piID_End */
        String sQuery_del= "delete from APP.SYNCDATAOUT where ID=?";
        PreparedStatement oPSt = poDBSession.getConnection().prepareStatement(sQuery_del);
        oPSt.setInt(1,piID); 
        try {
            oPSt.executeUpdate();
            oPSt.close();
        } catch (Exception e) {
            throw new Exception("FAILED to delete sync data for from database! "+e.getMessage());
        }
    }
}
