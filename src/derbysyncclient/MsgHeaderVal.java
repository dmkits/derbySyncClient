/* derbysyncclient
 * @autor dmk.dp.ua 2014-04-15
 */
package derbysyncclient;

/** MsgHeaderVal
 * Значения заголовка SOAP-сообщений.
 * @author dmk.dp.ua 2014-04-15
 */
public class MsgHeaderVal {
    
    public static final String REQUEST_FROM_CLIENT_WITH_CLIENT_DATA_MSG_NAME = ClientInstanceInfo.CLIENT_NAME+"_"+ClientInstanceInfo.CLIENT_VERSION+"_Data_From_Client";
    public static final String REQUEST_FROM_CLIENT_TO_UPD_STATUS_DATA_MSG_NAME = ClientInstanceInfo.CLIENT_NAME+"_"+ClientInstanceInfo.CLIENT_VERSION+"_Data_To_Upd_Status";
    
    public static final String REQUEST_FROM_CLIENT_TO_GET_SERVER_DATA_MSG_NAME = ClientInstanceInfo.CLIENT_NAME+"_"+ClientInstanceInfo.CLIENT_VERSION+"_To_Get_Server_Data";
    public static final String REQUEST_FROM_CLIENT_TO_UPD_SATATUS_SERVER_DATA_MSG_NAME = ClientInstanceInfo.CLIENT_NAME+"_"+ClientInstanceInfo.CLIENT_VERSION+"_To_Upd_Status_Server_Data";
    
    public static final String RESPONSE_FROM_SERVICE_MSG_NAME = ClientInstanceInfo.SERVICE_NAME+"_"+ClientInstanceInfo.SERVICE_VERSION+" Data";
    
}
