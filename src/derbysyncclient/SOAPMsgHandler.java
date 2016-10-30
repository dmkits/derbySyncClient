/* derbysyncclient
 * @autor dmk.dp.ua 2014-04-15
 */
package derbysyncclient;

import java.util.HashMap;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

/** SOAPMsgHandler
 * Обработчик для SOAPMessage.
 * Создание SOAPMessage с данными (заполнение заголовка и тела).
 * Чтение данных из SOAPMessage (заголовка и тела).
 * @author dmk.dp.ua 2014-04-16 */
public class SOAPMsgHandler {
    
    public static SOAPMessage crMsg() throws SOAPException {
        MessageFactory mf = MessageFactory.newInstance();
        SOAPMessage msg = mf.createMessage();
        return msg;
    }
    
    public static SOAPMessage crMsg(String sHeader) throws SOAPException {
        SOAPMessage msg = SOAPMsgHandler.crMsg();
        SOAPPart soapPart = msg.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        SOAPHeader header = envelope.getHeader();
        header.addTextNode(sHeader); //заголовок сообщения
        return msg;
    }
    
    public static SOAPMessage crMsg(String sHeader, String sBody) throws SOAPException {
        SOAPMessage msg = SOAPMsgHandler.crMsg(sHeader);
        SOAPBody body = msg.getSOAPBody();
        body.addTextNode(sBody);  
        return msg;
    }

    public static SOAPMessage crMsg(String sHeader, String sBody, String[]... sBodyCh) throws SOAPException {
        SOAPMessage msg = SOAPMsgHandler.crMsg(sHeader,sBody);
        SOAPBody body = msg.getSOAPBody();
        
        for (String[] sBCh: sBodyCh) {
            body.addChildElement(sBCh[0]).addTextNode(sBCh[1]); 
        }
        return msg;
    }
    
    public static String getMsgHeader(SOAPMessage msg) throws SOAPException {
        SOAPHeader header = msg.getSOAPHeader();
        String sHeaderVal = header.getTextContent();
        return sHeaderVal;
    }

    public static HashMap getMsgBody(SOAPMessage msg) throws SOAPException {
        HashMap hm = new HashMap();
        SOAPBody body = msg.getSOAPBody();
        hm.put("Body", body.getValue()); //Body
        for (int i=1; i<body.getChildNodes().getLength();i++) { //Body children
            //System.out.print(body.getChildNodes().item(i).getLocalName()+" "+body.getChildNodes().item(i).getTextContent());//!!!IT'S for testing!!!
            hm.put(body.getChildNodes().item(i).getLocalName(),body.getChildNodes().item(i).getTextContent());
        }
        return hm;
    }
}
