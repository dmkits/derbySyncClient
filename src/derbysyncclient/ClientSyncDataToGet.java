/* derbysyncclient
 * @autor dmk.dp.ua 2014-04-15
 */
package derbysyncclient;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPMessage;

/** ClientSyncDataToGet
 * Формирование сообщения-запроса на прием данных с сервера и обработка сообщения-ответа с данными с сервера.
 * Формируется фсообщение-запрос на сервер на прием данных синхронизации с сервера.
 * Обрабатывается сообщение-ответ с данными из таблиц с исходящими данными синхронизации с сервера.
 * Полученные с сервера данные с информацией о данных синхронизации вставляются в таблицу с входящими данными синхронизации.
 * Формат сообщения-ответа: в теле сообщения-ответа указывается таблица в которой обновляются или в которую вставляются данные,
 * также в теле указываеся имя и значение ключевого поля таблицы, 
 * наменования полей и их значения содержатся в приложениях к сообщению.
 * Полученные в теле данные вставляются в таблицу с ифнормацией о входящих данных синхронизации.
 * Затем полученные в теле и приложениях данные применяются.
 * По результату применения обновляется статус, дата обновления, дата применения и сообщение принятых данных 
 * в таблице с ифнормацией о входящих данных синхронизации.
 * @author dmk.dp.ua 2014-04-16 */
public class ClientSyncDataToGet {
    
    private static final Logger logger = Logger.getLogger("derbysyncclient.ClientSyncDataToGet");

    private Session cvoDBSession = null;
    private HashMap cvoDataForRequest = null; //данные из таблицы синхронизации для сообщения-запроса
    private HashMap cvoDataFromResponse = null;//данные из сообщения-ответа

    public ClientSyncDataToGet(Session oDBSession) {
        cvoDBSession = oDBSession;
        cvoDataForRequest = new HashMap(); 
        cvoDataFromResponse = new HashMap(); 
    }

    /* Получение данных первой записи из таблицы с данными о базах данных синхронизации на клиенте. */
    private void getSyncDBPropsData() throws Exception {
        logger.log(Level.INFO, "-----Getting sync database properties data from database-----");
        String sQuery= TextFromResource.load("/sqlscripts/syncdbdata_sel.sql");
        try {
            //logger.log(Level.INFO, "Getting statement to database.");
            cvoDataForRequest.clear(); // очистка массива с данными
            Statement voSt=cvoDBSession.getConnection().createStatement();
            //logger.log(Level.INFO, "Executing syncdbdata_sel.sql.");
            ResultSet voRS= voSt.executeQuery(sQuery);
            if (voRS.next()) {
                cvoDataForRequest.put("DBID",voRS.getString("ID"));
                cvoDataForRequest.put("DBName",voRS.getString("DBNAME"));
                cvoDataForRequest.put("POSName",voRS.getString("POSNAME"));
                cvoDataForRequest.put("StockName",voRS.getString("STOCKNAME"));
                logger.log(Level.INFO, "Recieved sync information data from database by DBID: "+(String)cvoDataForRequest.get("DBID")); 
                voSt.close();
            } else {
                logger.log(Level.INFO, "No data for request message.");
                voSt.close();
                throw new Exception("FAILED to get sync database properties data from database! No data in database!");
            }
        } catch (Exception e) {
            throw new Exception("FAILED to get sync database properties data from database! "+e.getMessage());
        } 
    }

    /* Подготовка сообщения-запроса на прием данных с сервера.
     * формат сообщения: 
     *    заголовок (Header) - наименование посылаемого сообщения,
     *    тело (Body) - наименование базы от которой пришло сообщение,
     *    тело (Body)-(Child1) - наименование POS-клиента,
     *    тело (Body)-(Child2) - наименование склада POS-клиента. */
    public SOAPMessage getRequest() throws Exception {
        logger.log(Level.INFO, "----------Prepare request message to server sync data----------");
        getSyncDBPropsData(); //получение данных свойств базы данных.
        //---создание сообщения, заполнение заголовка и тела сообщения данными с информацией об отправляемых данных---
        logger.log(Level.INFO, "-----Creating SOAP request message-----");
        SOAPMessage request = null;
        try{
            request = SOAPMsgHandler.crMsg( //создание SOAP сообщения-запроса с данными заголовка и тела
                    MsgHeaderVal.REQUEST_FROM_CLIENT_TO_GET_SERVER_DATA_MSG_NAME, //заголовок сообщения
                    (String)cvoDataForRequest.get("DBName"), //тело сообщения
                    new String[] {"POSName",(String)cvoDataForRequest.get("POSName")}, 
                    new String[] {"StockName",(String)cvoDataForRequest.get("StockName")} );
        } catch (Exception e) {
            throw new Exception("FAILED to create SOAP request message! "+e.getMessage());
        }
        return request;
    }
    
    /* Обработка ответа на запрос на прием данных от клиента. */
    public boolean handlingResponse(SOAPMessage response) throws Exception {
        logger.log(Level.INFO, "-----------Handling response message-----------");
        if (response==null) { 
            throw new Exception("FAILED to handle response message! Response message is NULL!");
        }
        
        String sChID;
        try {//---чтение данных тела из сообщения-ответа---
            logger.log(Level.INFO, "-----Reading response message body-----");
            String sHeaderVal = SOAPMsgHandler.getMsgHeader(response); //заголовок сообщения-ответа
            cvoDataFromResponse = SOAPMsgHandler.getMsgBody(response); //данные из тела из сообщения-ответа
            logger.log(Level.INFO,"RESPONSE FROM \""+sHeaderVal+"\" TO \""+(String)cvoDataFromResponse.get("Body")+"\""); // !!!IT'S FOR TESTING!!!
            sChID=(String)cvoDataFromResponse.get("ChID");
            if (sChID.equals("-")) { //пустое сообщение посылаемое сервером когда на сервере нет данных для клиента
                logger.log(Level.INFO, "On server has not data for client");
                return false; 
            }
            logger.log(Level.INFO,"SyncData ChID \""+sChID+"\""); // !!!IT'S FOR TESTING!!! 
            logger.log(Level.INFO,"SyncData server TableName "+(String)cvoDataFromResponse.get("TableName")+
                    ", KeyDataName "+(String)cvoDataFromResponse.get("KeyDataName")+", KeyDataVal \""+(String)cvoDataFromResponse.get("KeyDataVal")+"\", CRDate "+(String)cvoDataFromResponse.get("CRDate")); // !!!IT'S FOR TESTING!!! 
        } catch (Exception e) {
            throw new Exception("FAILED to read response message body! Wrong message structure! "+e.getMessage());
        }
        recSyncDataIn();//запись данных из тела сообщения в таблицу входящих данных синхронизации
        applySynDataIn(response); //применение полученных данных на клиенте
        updStatus(); //обновление статуса полученных данных в таблице входящих данных синхронизации
        return true;
    }
    
    /*Запись входящих данных синхронизации в таблицу входящих данных синхронизации.*/
    private void recSyncDataIn() throws Exception {
        String sID = null;
        logger.log(Level.INFO, "Selecting sync data from syncdatain table.");
        String sQuery_sel= TextFromResource.load("/sqlscripts/syncdatain_sel.sql");
        try {
            PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery_sel);
            //logger.log(Level.INFO, "Executing syncdbdata_sel.sql.");
            voPSt.setString(1,(String)cvoDataFromResponse.get("ChID"));
            ResultSet voRS = voPSt.executeQuery();
            if (voRS.next()) { sID = voRS.getString("ID"); }
            voPSt.close();
        } catch (Exception e) {
            throw new Exception("FAILED to get sync data from syncdatain table! "+e.getMessage());
        }
        if (sID!=null) {//удаление старой записи
            logger.log(Level.INFO, "Deleting sync data from syncdatain table.");
            String sQuery_ins= TextFromResource.load("/sqlscripts/syncdatain_del.sql");
            try {
                PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery_ins);
                //logger.log(Level.INFO, "Executing syncdbdata_sel.sql.");
                voPSt.setString(1,sID);
                voPSt.executeUpdate();
                voPSt.close();
            } catch (Exception e) {
                throw new Exception("FAILED to delete sync data from syncdatain table! "+e.getMessage());
            }
        }
        logger.log(Level.INFO, "Inserting sync data in into syncdatain table.");
        String sQuery_ins= TextFromResource.load("/sqlscripts/syncdatain_ins.sql");
        try {
            PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery_ins);
            //logger.log(Level.INFO, "Executing syncdbdata_sel.sql.");
            voPSt.setString(1,(String)cvoDataFromResponse.get("ChID"));
            voPSt.setString(2,(String)cvoDataFromResponse.get("TableName"));
            voPSt.setString(3,(String)cvoDataFromResponse.get("KeyDataName"));
            voPSt.setString(4,(String)cvoDataFromResponse.get("KeyDataVal"));
            voPSt.setString(5,(String)cvoDataFromResponse.get("CRDate"));
            voPSt.executeUpdate();
            voPSt.close();
        } catch (Exception e) {
            throw new Exception("FAILED to insert get sync data into syncdatain table! "+e.getMessage());
        }
    }
    
    /*Применение входящих данных синхронизации.*/
    private void applySynDataIn(SOAPMessage response) throws Exception {
        //response.writeTo(System.out);//IT'S FOR TESTING!!!
        boolean bExists = false;
        String sTable = (String)cvoDataFromResponse.get("TableName");
        String sKeyName = (String)cvoDataFromResponse.get("KeyDataName");
        String sKeyVal = (String)cvoDataFromResponse.get("KeyDataVal");
        try {
            String sQuery = "select "+sKeyName+" from "+sTable+" where "+sKeyName+" = ?";
            PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery);
            voPSt.setString(1,sKeyVal);
            ResultSet voRS = voPSt.executeQuery();
            if (voRS.next()) { bExists=true; }
            voPSt.close();
        } catch (Exception e) {
            throw new Exception("FAILED to select sync data key! "+e.getMessage());
        }
        if (bExists) {//update data
            try {
                logger.log(Level.INFO,"Applying sync data (updating).");
                String sFieldsSet = null;
                @SuppressWarnings("rawtypes") java.util.Iterator it = response.getAttachments();
                while (it.hasNext()) {//подготовка списка полей
                    AttachmentPart attachment = (AttachmentPart)it.next();
                    String sDataName= attachment.getContentId();
                    //String sDataVal= ""+attachment.getContent();
                    //logger.log(Level.INFO,"Attachment: SyncDataName:" + sDataName + " SyncDataVal:" + sDataVal); // !!!IT'S FOR TESTING!!!
                    if (sFieldsSet==null) { sFieldsSet=sDataName+"=?"; } else { sFieldsSet=sFieldsSet+","+sDataName+"=?"; } 
                }
                String sQuery = "update "+sTable+" set "+sFieldsSet+" where "+sKeyName+"=?";
                logger.log(Level.INFO,"Updating query: " + sQuery + "."); // !!!IT'S FOR TESTING!!!
                PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery);
                @SuppressWarnings("rawtypes") java.util.Iterator it2 = response.getAttachments();
                int i = 0;
                while (it2.hasNext()) {//подготовка параметров для полей
                    AttachmentPart attachment = (AttachmentPart)it2.next();
                    String sDataVal= ""+attachment.getContent();
                    i++;
                    //logger.log(Level.INFO,"Parameter:" + i + " value:" + sDataVal); // !!!IT'S FOR TESTING!!!
                    voPSt.setString(i,sDataVal);
                }
                voPSt.setString(i+1,sKeyVal);
                voPSt.executeUpdate();
                voPSt.close();
            } catch (Exception e) {
                throw new Exception("FAILED to apply sync data! "+e.getMessage());
            }
        } else {//insert data
            try {
                logger.log(Level.INFO,"Applying sync data (inserting).");
                String sFields = null; 
                String sFieldsVal = null;
                @SuppressWarnings("rawtypes") java.util.Iterator it = response.getAttachments();
                while (it.hasNext()) {//подготовка списка полей
                    AttachmentPart attachment = (AttachmentPart)it.next();
                    String sDataName= attachment.getContentId();
                    //String sDataVal= ""+attachment.getContent();
                    //logger.log(Level.INFO,"Attachment: SyncDataName:" + sDataName + " SyncDataVal:" + sDataVal); // !!!IT'S FOR TESTING!!!
                    if (sFields==null) { sFields=sDataName; sFieldsVal="?"; } else { sFields=sFields+","+sDataName; sFieldsVal=sFieldsVal+",?"; } 
                }
                String sQuery = "insert into "+sTable+" ("+sKeyName+","+sFields+") values(?,"+sFieldsVal+")";
                logger.log(Level.INFO,"Inserting query: " + sQuery + "."); // !!!IT'S FOR TESTING!!!
                PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery);
                voPSt.setString(1,sKeyVal);
                @SuppressWarnings("rawtypes") java.util.Iterator it2 = response.getAttachments();
                int i = 0;
                while (it2.hasNext()) {//подготовка параметров для полей
                    AttachmentPart attachment = (AttachmentPart)it2.next();
                    String sDataVal= ""+attachment.getContent().toString();
                    i++;
                    //logger.log(Level.INFO,"Parameter:" + (i+1) + " value:" + sDataVal); // !!!IT'S FOR TESTING!!!
                    voPSt.setString(1+i,sDataVal);
                }
                voPSt.executeUpdate();
                voPSt.close();
            } catch (Exception e) {
                throw new Exception("FAILED to apply sync data! "+e.getMessage());
            }
        }
    }
    
    /*Обновление статуса и данных в таблице входящих данных синхронизации по результату применения данных.*/
    private void updStatus() throws Exception {
        logger.log(Level.INFO, "Updating sync data status in syncdatain table.");
        String sQuery_ins= TextFromResource.load("/sqlscripts/syncdatain_upd.sql");
        try {
            PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery_ins);
            //logger.log(Level.INFO, "Executing syncdbdata_sel.sql.");
            voPSt.setString(1,"1");//Status
            voPSt.setTimestamp(2,new java.sql.Timestamp(System.currentTimeMillis()));//UPDATEDATE
            voPSt.setTimestamp(3,new java.sql.Timestamp(System.currentTimeMillis()));//APPLIEDDATE
            voPSt.setString(4,(String)cvoDataFromResponse.get("ChID"));
            voPSt.executeUpdate();
            voPSt.close();
        } catch (Exception e) {
            throw new Exception("FAILED to update sync data status in syncdatain table! "+e.getMessage());
        }
    }
    
    /*Формирование сообщения-запроса на сервер с данными о результате приема и применения данных с сервера.*/
    public SOAPMessage getRequestWithResult() throws Exception {
        logger.log(Level.INFO, "----------Prepare request message with result recieved and applied data from server----------");
        logger.log(Level.INFO, "Selecting sync data from syncdatain table.");
        String sQuery_sel= TextFromResource.load("/sqlscripts/syncdatain_sel.sql");
        try {
            PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery_sel);
            //logger.log(Level.INFO, "Executing syncdbdata_sel.sql.");
            voPSt.setString(1,(String)cvoDataFromResponse.get("ChID"));
            ResultSet voRS = voPSt.executeQuery();
            if (voRS.next()) { 
                cvoDataForRequest.put("ID",voRS.getString("ID"));
                cvoDataForRequest.put("Status",voRS.getString("Status"));
                cvoDataForRequest.put("AppliedDate",voRS.getString("APPLIEDDATE"));
                cvoDataForRequest.put("Msg",voRS.getString("Msg"));
            } else {
                throw new Exception("FAILED to get sync data from syncdatain table! No data!");
            }
            voPSt.close();
        } catch (Exception e) {
            throw new Exception("FAILED to get sync data from syncdatain table! "+e.getMessage());
        }
        //---создание сообщения, заполнение заголовка и тела сообщения данными с информацией об отправляемых данных---
        logger.log(Level.INFO, "-----Creating SOAP request message-----");
        SOAPMessage request = null;
        try{
            request = SOAPMsgHandler.crMsg( //создание SOAP сообщения-запроса с данными заголовка и тела
                    MsgHeaderVal.REQUEST_FROM_CLIENT_TO_UPD_SATATUS_SERVER_DATA_MSG_NAME, //заголовок сообщения
                    (String)cvoDataForRequest.get("DBName"), //тело сообщения
                    new String[] {"POSName",(String)cvoDataForRequest.get("POSName")}, 
                    new String[] {"StockName",(String)cvoDataForRequest.get("StockName")},
                    new String[] {"ChID",(String)cvoDataFromResponse.get("ChID")},
                    new String[] {"Status",(String)cvoDataForRequest.get("Status")},
                    new String[] {"AppliedDate",(String)cvoDataForRequest.get("AppliedDate")},
                    new String[] {"ClientDataID",(String)cvoDataForRequest.get("ID")},
                    new String[] {"Msg",(String)cvoDataForRequest.get("Msg")} );
        } catch (Exception e) {
            throw new Exception("FAILED to create SOAP request message! "+e.getMessage());
        }
        return request;
    }
}
