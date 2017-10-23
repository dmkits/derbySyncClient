package derbysyncclient;

import java.util.HashMap;

/**
 * Created by dmkits on 23.10.17.
 */
public class RequestToSyncService extends JSONRequest{

    static String SYNC_SERVICE_URI = "syncService";
    static String SYNC_SERVICE_CLIENT_REQUEST_HEADER = "sync-service-client";
    static String SYNC_SERVICE_CLIENT_REQUEST_TYPE = "syncServiceClientRequestType";
    static String REQUEST_TYPE_GET_INC_DATA = "getSyncIncData";
    static String REQUEST_TYPE_SEND_OUT_DATA = "storeSyncOutData";
    static String REQUEST_TYPE_APPLY_OUT_DATA = "applySyncOutData";

    public static String POS_NAME = "POSName";

    public static String SYNC_DATA_ID = "SyncDataID";
    public static String TABLE_NAME = "TableName";
    public static String TABLE_KEY1_NAME = "TableKey1Name";
    public static String TABLE_KEY1_VALUE = "TableKey1Value";
    public static String TABLE_KEY2_NAME = "TableKey2Name";
    public static String TABLE_KEY2_VALUE = "TableKey2Value";
    public static String OPERATION_TYPE = "OType";

    public static String SERVER_SYNC_DATA_ID = "ServerSyncDataID";
    public static String SERVER_CREATE_DATE = "ServerCreateDate";

    public RequestToSyncService(String url) {
        super(url+"/"+SYNC_SERVICE_URI);
        request.addHeader(SYNC_SERVICE_CLIENT_REQUEST_HEADER, ClientInstanceInfo.CLIENT_ID+"_"+ClientInstanceInfo.CLIENT_VERSION);
    }

    private HashMap<String,Object> postToSyncService(HashMap<String,String> data,
                                                     HashMap<String,String> dataItem,
                                                     HashMap<String,String> dataItemValues) throws Exception {
        HashMap<String,Object> dataToSend= new HashMap<>();
        dataToSend.put(POS_NAME,data.get("POS.clientSyncName"));
        dataToSend.put(SYNC_SERVICE_CLIENT_REQUEST_TYPE,data.get(SYNC_SERVICE_CLIENT_REQUEST_TYPE));
        if(dataItem!=null) dataToSend.put("dataItem",dataItem);
        if(dataItem!=null) dataToSend.put("dataItemValues",dataItemValues);
        HashMap<String,Object> syncServiceDataFromService= post(dataToSend);
        String sError=null;
        if(syncServiceDataFromService.get("error")!=null) sError=syncServiceDataFromService.get("error").toString();
        if(sError!=null)
            throw new Exception("Failed post to sync service! Reason:"+sError);
        return (HashMap)syncServiceDataFromService.get("resultItem");
    }
    private HashMap<String,Object> postToSyncService(HashMap<String,String> data) throws Exception {
        return postToSyncService(data,null,null);
    }

    public boolean getSyncIncData(HashMap<String, String> posClientInformation) throws Exception {
        try {
            posClientInformation.put(SYNC_SERVICE_CLIENT_REQUEST_TYPE,REQUEST_TYPE_GET_INC_DATA);
            HashMap<String,Object> incSyncData= postToSyncService(posClientInformation);
            if(incSyncData==null) return false;

            return true;
        } catch (Exception e){
            throw new Exception("Failed request to get sync inc data! Reason:"+e.getLocalizedMessage());
        }
    }
    public boolean storeSyncOutData(HashMap<String, String> posClientInformation,
                                    HashMap<String, String> posClientOutData,
                                    HashMap<String, String> posClientOutDataValues) throws Exception {
        try {
            posClientInformation.put(SYNC_SERVICE_CLIENT_REQUEST_TYPE, REQUEST_TYPE_SEND_OUT_DATA);
            HashMap<String,Object> outSyncDataResult=
                    postToSyncService(posClientInformation,posClientOutData,posClientOutDataValues);

            return true;
        } catch (Exception e){
            throw new Exception("Failed request to store sync out data! Reason:"+e.getLocalizedMessage());
        }
    }
    public boolean applySyncOutData(HashMap<String, String> posClientInformation,
                                    HashMap<String, Object> posClientOutData) throws Exception {
        try {
            posClientInformation.put(SYNC_SERVICE_CLIENT_REQUEST_TYPE,REQUEST_TYPE_APPLY_OUT_DATA);
            postToSyncService(posClientInformation);
            return true;
        } catch (Exception e){
            throw new Exception("Failed request to apply sync out data! Reason:"+e.getLocalizedMessage());
        }
    }
}
