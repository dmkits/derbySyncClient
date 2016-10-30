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
import javax.xml.soap.SOAPMessage;

/** ClientSyncDataToUpdStatus
 * Формирование сообщения-запроса и обработка сообщения-ответа при отправке данных серверу на применение.
 * Для сообщения-запроса извлекаются данные первой записи со статусом 0 из таблицы с информацией о данных синхронизации на отправку.
 * Данные помещаются в тело сообщения-запроса.
 * При обработке ответа обновляется статус записи в таблице с информацией о данных синхронизации на отправку 
 * по ИД полученному в ответе, также обновляется поле сообщения и дата применения.
 * @author dmk.dp.ua 2014-04-15
 */
public class ClientSyncDataToUpdStatus {

    private static final Logger logger = Logger.getLogger("derbysyncclient.ClientSyncDataToUpdStatus");
    
    private Session cvoDBSession = null;
    private HashMap cvoDataForRequest = null; //данные из таблицы синхронизации для сообщения-запроса
    
    public ClientSyncDataToUpdStatus(Session oDBSession) {
        cvoDBSession = oDBSession;
        cvoDataForRequest = new HashMap();
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

    /* Получение данных первой не примененной записи из БД из таблицы с информацией о данных синхронизации. */
    public boolean getSyncDataInf() throws Exception {
        logger.log(Level.INFO, "-----Getting sync information data with status=0 from database-----");
        getSyncDBPropsData(); //получение данных свойств базы данных.
        Statement voSt= null;
        try {
            //logger.log(Level.INFO, "Getting statement to database.");
            voSt=cvoDBSession.getConnection().createStatement();
            String sQuery= TextFromResource.load("/sqlscripts/syncdataoutwithstatus_sel.sql");
            //logger.log(Level.INFO, "Executing syncdataoutwithstatus_sel.sql.");
            ResultSet voRS= voSt.executeQuery(sQuery);
            if (voRS.next()) {
                cvoDataForRequest.put("ID",voRS.getString("ID"));
                logger.log(Level.INFO, "Recieved sync information data from database by ID: "+(String)cvoDataForRequest.get("ID"));
                return true;
            } else {
                logger.log(Level.INFO, "No data for request message.");
                return false;
            }
        } catch (Exception e) {
            throw new Exception("FAILED to get sync information data from database! "+e.getMessage());
        } 
    }
    
    /* Подготовка сообщения-запроса на обновление статуса- применение данных на сервере. 
     * формат сообщения: 
     *    заголовок (Header) - наименование посылаемого сообщения,
     *    тело (Body) - наименование базы от которой пришло сообщение,
     *    тело (Body)-(Child1) - наименование POS-клиента,
     *    тело (Body)-(Child2) - наименование склада POS-клиента,
     *    тело (Body)-(Child3) - ИД данных синхронизации со стороны клиента. */
    public SOAPMessage getRequest() throws Exception {
        logger.log(Level.INFO, "----------Preparing request message to update status sync data from database----------");
        SOAPMessage request = null;
        try{//---создание сообщения, заполнение заголовка и тела сообщения данными с информацией о данных синхронизации, которые нужно применить---
            logger.log(Level.INFO, "-----Creating SOAP request message-----");
            request = SOAPMsgHandler.crMsg( //создание SOAP сообщения-запроса с данными заголовка и тела
                    MsgHeaderVal.REQUEST_FROM_CLIENT_TO_UPD_STATUS_DATA_MSG_NAME, //заголовок сообщения
                    (String)cvoDataForRequest.get("DBName"), //тело сообщения
                    new String[] {"POSName",(String)cvoDataForRequest.get("POSName")}, 
                    new String[] {"StockName",(String)cvoDataForRequest.get("StockName")},
                    new String[] {"ID",(String)cvoDataForRequest.get("ID")} );
        } catch (Exception e) {
            logger.log(Level.WARNING, "FAILED to create SOAP request message!", e);
            throw new Exception("FAILED to create SOAP request message! "+e.getMessage());
        }
        return request;
    }
    
    /* Обработка ответа на запрос на изменение статуса-применение данных на сервере. */
    public boolean handlingResponse(SOAPMessage response) throws Exception {
        logger.log(Level.INFO, "-----------Handling response message-----------");
        if (response==null) { 
            throw new Exception("FAILED to handle response message! Response message is NULL!");
        }
        HashMap oDataFromResponse = null; 
        try {
            logger.log(Level.INFO, "-----Reading response message body-----");
            String sHeaderVal = SOAPMsgHandler.getMsgHeader(response); //заголовок сообщения-ответа
            oDataFromResponse = SOAPMsgHandler.getMsgBody(response); //данные из тела из сообщения-ответа
            logger.log(Level.INFO,"RESPONSE FROM \""+sHeaderVal+"\" TO \""+(String)oDataFromResponse.get("Body")+"\""); // !!!IT'S FOR TESTING!!!
            logger.log(Level.INFO,"SyncData ID \""+(String)oDataFromResponse.get("ID") +"\""); // !!!IT'S FOR TESTING!!! 
            logger.log(Level.INFO,"SyncData server ChID "+(String)oDataFromResponse.get("ChID")+
                    ", Status "+(String)oDataFromResponse.get("Status")+", Msg \""+(String)oDataFromResponse.get("Msg")+"\", AppliedDate "+(String)oDataFromResponse.get("AppliedDate")); // !!!IT'S FOR TESTING!!! 
        } catch (Exception e) {
            throw new Exception("FAILED to read response message body! Wrong message structure! "+e.getMessage());
        }
        String sQuery_upd= TextFromResource.load("/sqlscripts/syncdataoutwithstatus_upd.sql"); //чтение sql-запроса из ресурса
        //подготовка обработчика запросов к БД и выполнение запроса-обновления данных в таблице синхронизации данных на отправку
        try {
            //logger.log(Level.INFO, "Preparing database statement from resource syncdataout_upd.sql.");
            PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery_upd);
            //logger.log(Level.INFO, "Preparing parameters to database statement ("+(String)oData.get("Status")+", "+(String)oData.get("Msg")+", "+(String)oData.get("AppliedDate")+").");
            voPSt.setString(1,(String)oDataFromResponse.get("Status")); //Status
            voPSt.setString(2,(String)oDataFromResponse.get("Msg")); //Msg
            voPSt.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis()));//UPDATEDATE
            if ("".equals((String)oDataFromResponse.get("AppliedDate"))) { //APPLIEDDATE 
                voPSt.setNull(4, java.sql.Types.TIMESTAMP);
            } else {
                voPSt.setString(4,(String)oDataFromResponse.get("AppliedDate"));
            }
            voPSt.setString(5,(String)oDataFromResponse.get("ID")); //ID
            //logger.log(Level.INFO, "Executing prepeared statement.");
            voPSt.executeUpdate();
            voPSt.close();
        } catch (Exception e) {
            throw new Exception("FAILED to prepare and execute database statement from resource syncdataoutwithstatus_upd.sql! "+e.getMessage());
        }
        return true;
    }
}
