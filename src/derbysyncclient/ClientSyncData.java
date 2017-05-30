package derbysyncclient;

import javax.xml.soap.*;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by dmkits on 27.11.16.
 * reading header and body from SOAP message
 * creating SOAP message
 */
public abstract class ClientSyncData {

    public static String POS_CLIENT_SYNC_NAME = "POSClientSyncName";

    public static String SYNC_DATA_ID = "SyncDataID";
    public static String TABLE_NAME = "TableName";
    public static String TABLE_KEY1_NAME = "TableKey1Name";
    public static String TABLE_KEY1_VALUE = "TableKey1Value";
    public static String TABLE_KEY2_NAME = "TableKey2Name";
    public static String TABLE_KEY2_VALUE = "TableKey2Value";
    public static String OPERATION_TYPE = "OType";

    public static String SERVER_SYNC_DATA_ID = "ServerSyncDataID";
    public static String SERVER_CREATE_DATE = "ServerCreateDate";

    private static final Logger logger = Logger.getLogger(ClientSyncData.class.getName());

    public static String getMsgHeader(SOAPMessage msg) throws SOAPException {
        SOAPHeader header = msg.getSOAPHeader();
        String sHeaderVal = header.getTextContent();
        return sHeaderVal;
    }
    public static HashMap getMsgBody(SOAPMessage msg) throws SOAPException {
        HashMap hm = new HashMap();
        SOAPBody body = msg.getSOAPBody();
        hm.put(POS_CLIENT_SYNC_NAME, body.getValue()); //Body
        for (int i=1; i<body.getChildNodes().getLength();i++) { //Body children
            hm.put(body.getChildNodes().item(i).getLocalName(),body.getChildNodes().item(i).getTextContent());
        }
        return hm;
    }

    public static SOAPMessage crMsg() throws SOAPException {
        MessageFactory mf = MessageFactory.newInstance();
        SOAPMessage msg = mf.createMessage();
        return msg;
    }
    public static SOAPMessage crMsg(String sHeader) throws SOAPException {
        SOAPMessage msg = crMsg();
        SOAPPart soapPart = msg.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        SOAPHeader header = envelope.getHeader();
        header.addTextNode(sHeader); //заголовок сообщения
        return msg;
    }
    public static SOAPMessage crMsg(String sHeader, String sBody) throws SOAPException {
        SOAPMessage msg = crMsg(sHeader);
        SOAPBody body = msg.getSOAPBody();
        body.addTextNode(sBody);
        return msg;
    }
    public static SOAPMessage crMsg(String sHeader, String sBody, String[]... sBodyCh) throws SOAPException {
        SOAPMessage msg = crMsg(sHeader,sBody);
        SOAPBody body = msg.getSOAPBody();
        for (String[] sBCh: sBodyCh) {
            String sNodeName = sBCh[0];
            String sNodeValue = sBCh[1];
            if (sNodeName!=null&&sNodeValue!=null)
                body.addChildElement(sBCh[0]).addTextNode(sBCh[1]);
        }
        return msg;
    }

    protected void getPOSClientSyncNAME(HashMap<String, String> dataForRequest, Session dbs) throws Exception {
        logger.log(Level.INFO, "-----Getting POS client sync name-----");
        String sQuery= TextFromResource.load("/sqlscripts/syncClientName_sel.sql");
        try {
            dataForRequest.clear(); // очистка массива с данными
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
                logger.log(Level.INFO, "No data for request message.");
                voSt.close();
                throw new Exception("FAILED to get POS client sync name! Request to server impossible!");
            }
            if (sPOSClientSyncName==null) {
                logger.log(Level.INFO, "POS client sync name no setted!");
                throw new Exception("FAILED to get POS client sync name! POS client sync name no setted! Request to server impossible!");
            }
            dataForRequest.put(POS_CLIENT_SYNC_NAME, sPOSClientSyncName);
        } catch (Exception e) {
            throw new Exception("FAILED to get POS client sync name! Reason: "+e.getMessage());
        }
    }
}
