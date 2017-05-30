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
public class ClientSyncDataToSend extends ClientSyncData {
    
    private static final Logger logger = Logger.getLogger("derbysyncclient.ClientSyncDataToSend");

    private Session cvoDBSession = null;
    private HashMap<String,String> cvoDataForRequest = null; //данные из таблицы синхронизации для сообщения-запроса
            
    public ClientSyncDataToSend(Session oDBSession) {
        cvoDBSession = oDBSession;
        cvoDataForRequest = new HashMap(); 
    }
    
    /* Получение данных первой не отправленной записи из БД из таблицы с информацией о данных синхронизации. */
    public boolean getSyncDataInf() throws Exception {
        logger.log(Level.INFO, "-----Getting sync information data from database-----");
        getPOSClientSyncNAME(cvoDataForRequest, cvoDBSession); //получение данных свойств базы данных.
        try {
            //logger.log(Level.INFO, "Getting statement to database.");
            Statement voSt=cvoDBSession.getConnection().createStatement();
            String sQuery= TextFromResource.load("/sqlscripts/syncdataout_sel.sql");
            //logger.log(Level.INFO, "Executing syncdataout_sel.sql.");
            ResultSet voRS= voSt.executeQuery(sQuery); 
            if (voRS.next()) {
                cvoDataForRequest.put(SYNC_DATA_ID,voRS.getString("ID"));
                cvoDataForRequest.put("CreateDate",voRS.getString("CRDATE"));
                cvoDataForRequest.put(TABLE_NAME,voRS.getString("TABLENAME"));
                cvoDataForRequest.put(OPERATION_TYPE,voRS.getString("OTYPE"));
                cvoDataForRequest.put(TABLE_KEY1_NAME,voRS.getString("TABLEKEY1IDNAME"));
                cvoDataForRequest.put(TABLE_KEY1_VALUE,voRS.getString("TABLEKEY1IDVAL"));
                cvoDataForRequest.put(TABLE_KEY2_NAME,voRS.getString("TABLEKEY2IDNAME"));
                cvoDataForRequest.put(TABLE_KEY2_VALUE, voRS.getString("TABLEKEY2IDVAL"));
                logger.log(Level.INFO, "Recieved sync information data from database by ID: " + cvoDataForRequest.get(SYNC_DATA_ID));
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
     *    тело (Body)-(Child4)-(CreateDate) - дата создания записи синхронизации на стороне клиента,
     *    тело (Body)-(Child5)-(TableName) - наименование таблицы, из которой посылаются данные синхронизации,
     *    тело (Body)-(Child6)-(OType) - тип операции (вставка,изменение,удаление),
     *    тело (Body)-(Child7)-(TableKey1IDName) - имя поля первичного ключа синхронизируемой таблицы клиента,
     *    тело (Body)-(Child8)-(TableKey1IDVal) - значение поля первичного ключа синхронизируемой таблицы клиента,
     *    тело (Body)-(Child9)-(TableKey2IDName) - имя поля второго первичного ключа синхронизируемой таблицы клиента,
     *    тело (Body)-(Child10)-(TableKey2IDVal) - значение поля второго первичного ключа синхронизируемой таблицы клиента,
     *    вложения (AttachmentPart) - перечень названий полей и данных из синхронизируемой таблицы. */
    public SOAPMessage getRequest() throws Exception  {
        logger.log(Level.INFO, "----------Preparing request message with sync data from database----------");
        SOAPMessage request = null;
        try{//---создание сообщения, заполнение заголовка и тела сообщения данными с информацией об отправляемых данных---
            logger.log(Level.INFO, "-----Creating SOAP request message-----");
            request = crMsg( //создание SOAP сообщения-запроса с данными заголовка и тела
                    MsgHeaderVal.REQUEST_FROM_CLIENT_WITH_CLIENT_DATA_MSG_NAME, //заголовок сообщения
                    (String)cvoDataForRequest.get(POS_CLIENT_SYNC_NAME), //тело сообщения
                    new String[] {SYNC_DATA_ID,cvoDataForRequest.get(SYNC_DATA_ID)},
                    new String[] {"CreateDate",cvoDataForRequest.get("CreateDate")},
                    new String[] {TABLE_NAME,cvoDataForRequest.get(TABLE_NAME)},
                    new String[] {OPERATION_TYPE,cvoDataForRequest.get("OType")},
                    new String[] {TABLE_KEY1_NAME,cvoDataForRequest.get(TABLE_KEY1_NAME)},
                    new String[] {TABLE_KEY1_VALUE,cvoDataForRequest.get(TABLE_KEY1_VALUE)},
                    new String[] {TABLE_KEY2_NAME,cvoDataForRequest.get(TABLE_KEY2_NAME)},
                    new String[] {TABLE_KEY2_VALUE,cvoDataForRequest.get(TABLE_KEY2_VALUE)});
        } catch (Exception e) {
            throw new Exception("FAILED to create SOAP request message! "+e.getMessage());
        }
        String sQuery;
        PreparedStatement voPSt;
        ResultSet voRS2 = null;
        try { //---выборка из БД данных синхронизации---
            logger.log(Level.INFO, "-----Getting syncdata from database-----");
            //---получаем все колонки таблицы sTableName по ИД sIDName=sIDVal---
            sQuery="select * from "+cvoDataForRequest.get(TABLE_NAME)+" where "+cvoDataForRequest.get(TABLE_KEY1_NAME)+"=?";
            if (cvoDataForRequest.get(TABLE_KEY2_NAME)!=null) {
                sQuery= sQuery + " and "+cvoDataForRequest.get(TABLE_KEY2_NAME)+"=?";
            }
            //logger.log(Level.INFO, "Preparing database statement.");
            voPSt=cvoDBSession.getConnection().prepareStatement(sQuery);
            logger.log(Level.INFO, "Preparing parameter 1 to database statement (" + cvoDataForRequest.get(TABLE_KEY1_VALUE) + ").");
            voPSt.setString(1, cvoDataForRequest.get(TABLE_KEY1_VALUE)); //sKey1IDVal
            if (cvoDataForRequest.get(TABLE_KEY2_NAME)!=null) {
                logger.log(Level.INFO, "Preparing parameter 2 to database statement (" + cvoDataForRequest.get(TABLE_KEY2_VALUE) + ").");
                voPSt.setString(2, cvoDataForRequest.get(TABLE_KEY2_VALUE));//sKey2IDVal
            }
            //logger.log(Level.INFO, "Executing prepeared statement.");
            voRS2= voPSt.executeQuery();
        } catch (Exception e) {
            throw new Exception("FAILED to read syncdata from database! "+e.getMessage());
        } 
        try { //---заполнение сообщения данными синхронизации из БД---
            voRS2.next();
            //---поочередно все колонки (имя и значение) из запроса сохраняем вложениями к сообщению---
            for (int i=1;i<=voRS2.getMetaData().getColumnCount();i++) {
                String sColName= voRS2.getMetaData().getColumnName(i); //column name
                if (!"ATTRIBUTES".equals(sColName)){
                    String sColVal= voRS2.getString(i); // column value
                    if (sColVal==null) {sColVal="";};
                    //---добавление вложения к сообщению---
                    //logger.log(Level.INFO, "Adding attachment to request message ("+sColName+", "+sColVal+").");
                    AttachmentPart attachment = request.createAttachmentPart();
                    attachment.setContent(sColVal, "text/plain; charset=utf-8");
                    attachment.setContentId(sColName);
                    request.addAttachmentPart(attachment);
                }
            }
            voPSt.close();
        } catch (Exception e) {
            throw new Exception("FAILED to put syncdata to request message! "+e.getMessage());
        } 
        return request;
    }
    
    /* Обработка ответа на запрос на прием данных от клиента.
     * Результат - полученный статус данных.
     * формат сообщения: 
     *    заголовок (Header) - наименование посылаемого сообщения,
     *    тело (Body) - наименование базы для которой пришло сообщение,
     *    тело (Body)-(Child1)-(ID) - ИД данных синхронизации на стороне клиента,
     *    тело (Body)-(Child2)-(ChID) - ИД данных синхронизации со стороны сервера,
     *    тело (Body)-(Child3)-(Status) - статус данных синхронизации на сервере,
     *    тело (Body)-(Child4)-(Msg) - сообщение о результате приема-применения данных синхронизации на сервере,
     *    тело (Body)-(Child5)-(AppliedDate) - дата-время применения данных на сервере. */
    public int handlingResponse(SOAPMessage response) throws Exception {
        logger.log(Level.INFO, "-----------Handling response message-----------");
        if (response==null) { //если полученное сообщение = null
            throw new Exception("FAILED to handle response message! Response message is NULL!"); 
        }
        HashMap oDataFromResponse = null;
        try { //---чтение данных тела из сообщения-ответа---
            logger.log(Level.INFO, "-----Reading response message body-----");
            String sHeaderVal = getMsgHeader(response); //заголовок сообщения-ответа
            oDataFromResponse = getMsgBody(response); //данные из тела из сообщения-ответа
            logger.log(Level.INFO,"RESPONSE FROM \""+sHeaderVal+"\" TO \""+oDataFromResponse.get(POS_CLIENT_SYNC_NAME)+"\""); // !!!IT'S FOR TESTING!!!
            logger.log(Level.INFO,"SyncData ID \""+oDataFromResponse.get(SYNC_DATA_ID) +"\""); // !!!IT'S FOR TESTING!!!
            logger.log(Level.INFO, "SyncData server ChID " + oDataFromResponse.get(SERVER_SYNC_DATA_ID)
                    + ", Status " + oDataFromResponse.get("Status")
                    + ", Msg \"" + oDataFromResponse.get("Msg")
                    + "\", AppliedDate " + oDataFromResponse.get("AppliedDate")); // !!!IT'S FOR TESTING!!!
        } catch (Exception e) {
            throw new Exception("FAILED to read response message body! Wrong message structure! "+e.getMessage()); 
        }
        String sStatus = (String)oDataFromResponse.get("Status");
        String sQuery_upd= TextFromResource.load("/sqlscripts/syncdataout_upd.sql"); //чтение sql-запроса из ресурса
        try { //---подготовка обработчика запросов к БД и выполнение запроса-обновления данных в таблице синхронизации данных на отправку---
            //logger.log(Level.INFO, "Preparing database statement from resource syncdataout_upd.sql.");
            PreparedStatement voPSt=cvoDBSession.getConnection().prepareStatement(sQuery_upd);
            //logger.log(Level.INFO, "Preparing parameters to database statement ("+(String)oData.get("STATUS")+", "+(String)oData.get("MSG")+").");
            voPSt.setString(1,sStatus); //Status
            voPSt.setString(2,(String)oDataFromResponse.get("Msg")); //Msg
            voPSt.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis()));//UPDATEDATE
            voPSt.setString(4,(String)oDataFromResponse.get(SYNC_DATA_ID)); //ID
            //logger.log(Level.INFO, "Executing prepeared statement.");
            voPSt.executeUpdate();
            voPSt.close();
        } catch (Exception e) {
            throw new Exception("FAILED to prepare and execute database statement from resource syncdataout_upd.sql "+e.getMessage());
        }
        try {
            return Integer.parseInt(sStatus);
        } catch (Exception e){
            throw new Exception("FAILED to finish handle response message! Response: state in not correct!");
        }
    }
}
