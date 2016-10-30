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

/** ClientSyncDataToSend
 * Формирование сообщения-запроса и обработка сообщения-ответа при отправке данных серверу на принятие.
 * На отправку из базы клиента из таблицы с информацией о данных синхронизации на отправку 
 * извлекаются данные из первой записи со статусом NULL.
 * Извлеченные данные помещаются в тело сообщения-запроса, приложениями к сообщению пристегиваются
 * данные синхронизации, извлеченные из таблицы, указанной в извлеченных ранее данных.
 * При обработке ответа, обновляется статус записи в таблице с информацией о данных синхронизации на отправку,
 * по ИД полученному в ответе, таже обновляется поле сообщения и поле даты обновления.
 * @author dmk.dp.ua 2014-04-15 */
public class ClientSyncDataToSend {
    
    private static final Logger logger = Logger.getLogger("derbysyncclient.ClientSyncDataToSend");

    private Session cvoDBSession = null;
    private HashMap cvoDataForRequest = null; //данные из таблицы синхронизации для сообщения-запроса
            
    public ClientSyncDataToSend(Session oDBSession) {
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

    /* Получение данных первой не отправленной записи из БД из таблицы с информацией о данных синхронизации. */
    public boolean getSyncDataInf() throws Exception {
        logger.log(Level.INFO, "-----Getting sync information data from database-----");
        getSyncDBPropsData(); //получение данных свойств базы данных.
        try {
            //logger.log(Level.INFO, "Getting statement to database.");
            Statement voSt=cvoDBSession.getConnection().createStatement();
            String sQuery= TextFromResource.load("/sqlscripts/syncdataout_sel.sql");
            //logger.log(Level.INFO, "Executing syncdataout_sel.sql.");
            ResultSet voRS= voSt.executeQuery(sQuery); 
            if (voRS.next()) {
                cvoDataForRequest.put("ID",voRS.getString("ID"));
                cvoDataForRequest.put("TableName",voRS.getString("TABLENAME"));
                cvoDataForRequest.put("OType",voRS.getString("OTYPE"));
                cvoDataForRequest.put("TableKey1IDName",voRS.getString("TABLEKEY1IDNAME"));
                cvoDataForRequest.put("TableKey1IDVal",voRS.getString("TABLEKEY1IDVAL"));
                cvoDataForRequest.put("TableKey2IDName",voRS.getString("TABLEKEY2IDNAME"));
                cvoDataForRequest.put("TableKey2IDVal",voRS.getString("TABLEKEY2IDVAL"));
                logger.log(Level.INFO, "Recieved sync information data from database by ID: "+(String)cvoDataForRequest.get("ID")); 
                voSt.close();
                return true;
            } else {
                logger.log(Level.INFO, "No data for request message.");
                voSt.close();
                return false;
            }
        } catch (Exception e) {
            throw new Exception("FAILED to get sync information data from database! "+e.getMessage());
        } 
    }
            
     /* Подготовка сообщения-запроса на прием данных на сервере на основании данных из БД клиента.
     * формат сообщения: 
     *    заголовок (Header) - наименование посылаемого сообщения,
     *    тело (Body) - наименование базы от которой пришло сообщение,
     *    тело (Body)-(Child1)-(POSName) - наименование POS-клиента,
     *    тело (Body)-(Child2)-(StockName) - наименование склада POS-клиента,
     *    тело (Body)-(Child3)-(ID) - ИД данных синхронизации со стороны клиента,
     *    тело (Body)-(Child4)-(TableName) - наименование таблицы, из которой посылаются данные синхронизации,
     *    тело (Body)-(Child5)-(OType) - тип операции (вставка,изменение,удаление),
     *    вложения (AttachmentPart) - перечень названий полей и данных из синхронизируемой таблицы. */
    public SOAPMessage getRequest() throws Exception  {
        logger.log(Level.INFO, "----------Preparing request message with sync data from database----------");
        SOAPMessage request = null;
        try{//---создание сообщения, заполнение заголовка и тела сообщения данными с информацией об отправляемых данных---
            logger.log(Level.INFO, "-----Creating SOAP request message-----");
            request = SOAPMsgHandler.crMsg( //создание SOAP сообщения-запроса с данными заголовка и тела
                    MsgHeaderVal.REQUEST_FROM_CLIENT_WITH_CLIENT_DATA_MSG_NAME, //заголовок сообщения
                    (String)cvoDataForRequest.get("DBName"), //тело сообщения
                    new String[] {"POSName",(String)cvoDataForRequest.get("POSName")}, 
                    new String[] {"StockName",(String)cvoDataForRequest.get("StockName")},
                    new String[] {"ID",(String)cvoDataForRequest.get("ID")},
                    new String[] {"TableName",(String)cvoDataForRequest.get("TableName")},
                    new String[] {"OType",(String)cvoDataForRequest.get("OType")} );
        } catch (Exception e) {
            throw new Exception("FAILED to create SOAP request message! "+e.getMessage());
        }
        String sQuery;
        PreparedStatement voPSt;
        ResultSet voRS2 = null;
        try { //---выборка из БД данных синхронизации---
            logger.log(Level.INFO, "-----Getting syncdata from database-----");
            //---получаем все колонки таблицы sTableName по ИД sIDName=sIDVal---
            sQuery="select * from "+(String)cvoDataForRequest.get("TableName")+" where "+(String)cvoDataForRequest.get("TableKey1IDName")+"=?"; 
            if ((String)cvoDataForRequest.get("TableKey2IDName")!=null) {
                sQuery= sQuery + " and "+(String)cvoDataForRequest.get("TableKey2IDName")+"=?"; 
            }
            //logger.log(Level.INFO, "Preparing database statement.");
            voPSt=cvoDBSession.getConnection().prepareStatement(sQuery);
            logger.log(Level.INFO, "Preparing parameter 1 to database statement ("+(String)cvoDataForRequest.get("TableKey1IDVal")+")."); 
            voPSt.setString(1,(String)cvoDataForRequest.get("TableKey1IDVal")); //sKey1IDVal
            if ((String)cvoDataForRequest.get("TableKey2IDName")!=null) {
                logger.log(Level.INFO, "Preparing parameter 2 to database statement ("+(String)cvoDataForRequest.get("TableKey2IDVal")+")."); 
                voPSt.setString(2,(String)cvoDataForRequest.get("TableKey2IDVal"));//sKey2IDVal
            }
            //logger.log(Level.INFO, "Executing prepeared statement.");
            voRS2= voPSt.executeQuery();
        } catch (Exception e) {
            throw new Exception("FAILED to read syncdata from database! "+e.getMessage());
        } 
        try { //---заполнение сообщения данными синхронизации из БД---
            voRS2.next();
            //---поочередно все колонки (имя и значение) из запроса сохраняем вложениями к сообщению---
            for (int i=1;i<voRS2.getMetaData().getColumnCount();i++) {
                String sColName= voRS2.getMetaData().getColumnName(i); //column name
                String sColVal= voRS2.getString(i); // column value
                if (sColVal==null) {sColVal="";};
                //---добавление вложения к сообщению---
                //logger.log(Level.INFO, "Adding attachment to request message ("+sColName+", "+sColVal+").");
                AttachmentPart attachment = request.createAttachmentPart(); 
                attachment.setContent(sColVal, "text/plain; charset=utf-8");
                attachment.setContentId(sColName);
                request.addAttachmentPart(attachment); 
            }
            //request.saveChanges();
            //System.out.println("REQUEST:"); // !!!IT'S FOR TESTING!!!
            //request.writeTo(System.out); // !!!IT'S FOR TESTING!!!
            //System.out.println(""); // !!!IT'S FOR TESTING!!!
            voPSt.close();
        } catch (Exception e) {
            throw new Exception("FAILED to put syncdata to request message! "+e.getMessage());
        } 
        return request;
    }
    
    /* Обработка ответа на запрос на прием данных от клиента. 
     * формат сообщения: 
     *    заголовок (Header) - наименование посылаемого сообщения,
     *    тело (Body) - наименование базы для которой пришло сообщение,
     *    тело (Body)-(Child1)-(ID) - ИД данных синхронизации на стороне клиента,
     *    тело (Body)-(Child2)-(ChID) - ИД данных синхронизации со стороны сервера,
     *    тело (Body)-(Child3)-(Status) - статус данных синхронизации на сервере,
     *    тело (Body)-(Child4)-(Msg) - сообщение о результате приема-применения данных синхронизации на сервере,
     *    тело (Body)-(Child5)-(AppliedDate) - дата-время применения данных на сервере. */
    public boolean handlingResponse(SOAPMessage response) throws Exception {
        logger.log(Level.INFO, "-----------Handling response message-----------");
        if (response==null) { //если полученное сообщение = null
            throw new Exception("FAILED to handle response message! Response message is NULL!"); 
        }
        HashMap oDataFromResponse = null;
        try { //---чтение данных тела из сообщения-ответа---
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
        String sQuery_upd= TextFromResource.load("/sqlscripts/syncdataout_upd.sql"); //чтение sql-запроса из ресурса
        try { //---подготовка обработчика запросов к БД и выполнение запроса-обновления данных в таблице синхронизации данных на отправку---
            //logger.log(Level.INFO, "Preparing database statement from resource syncdataout_upd.sql.");
            PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery_upd);
            //logger.log(Level.INFO, "Preparing parameters to database statement ("+(String)oData.get("STATUS")+", "+(String)oData.get("MSG")+").");
            voPSt.setString(1,(String)oDataFromResponse.get("Status")); //Status
            voPSt.setString(2,(String)oDataFromResponse.get("Msg")); //Msg
            voPSt.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis()));//UPDATEDATE
            voPSt.setString(4,(String)oDataFromResponse.get("ID")); //ID
            //logger.log(Level.INFO, "Executing prepeared statement.");
            voPSt.executeUpdate();
            voPSt.close();
        } catch (Exception e) {
            throw new Exception("FAILED to prepare and execute database statement from resource syncdataout_upd.sql "+e.getMessage());
        }
        return true;
    }
}
