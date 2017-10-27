package derbysyncclient;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by dmkits on 23.10.17.
 */
public class RequestToSyncService extends JSONRequest{

    private static final Logger logger = Logger.getLogger("derbysyncclient.RequestToSyncService");
    static String SYNC_SERVICE_URI = "syncService";
    static String SYNC_SERVICE_CLIENT_REQUEST_HEADER = "sync-service-client";
    static String SYNC_SERVICE_CLIENT_REQUEST_TYPE = "syncServiceClientRequestType";
    static String REQUEST_TYPE_GET_INC_DATA = "getSyncIncData";
    static String REQUEST_TYPE_STORE_OUT_DATA = "storeSyncOutData";
    static String REQUEST_TYPE_APPLY_OUT_DATA = "applySyncOutData";
    public static String POS_NAME = "POSName";

    public RequestToSyncService(String url) {
        super(url+"/"+SYNC_SERVICE_URI);
        request.addHeader(SYNC_SERVICE_CLIENT_REQUEST_HEADER, ClientInstanceInfo.CLIENT_ID+"_"+ClientInstanceInfo.CLIENT_VERSION);
    }

    private HashMap<String,Object> postToSyncService(String sSyncServiceRequestType, String sPOSName,
                                                     HashMap<String,String> dataItem,
                                                     HashMap<String,String> dataItemValues) throws Exception {
        HashMap<String,Object> dataToSend= new HashMap<>();
        dataToSend.put(SYNC_SERVICE_CLIENT_REQUEST_TYPE,sSyncServiceRequestType);
        dataToSend.put(POS_NAME,sPOSName);
        if(dataItem!=null) dataToSend.put("dataItem",dataItem);
        if(dataItem!=null) dataToSend.put("dataItemValues",dataItemValues);
        HashMap<String,Object> syncServiceDataFromService= post(dataToSend);
        logger.log(Level.INFO, "Getted data from server /SyncService: \n data={0}", syncServiceDataFromService);
        return syncServiceDataFromService;
    }
    private HashMap<String,Object> postToSyncService(String sSyncServiceRequestType, String sPOSName) throws Exception {
        return postToSyncService(sSyncServiceRequestType,sPOSName, null,null);
    }
    private HashMap<String,Object> postToSyncService(String sSyncServiceRequestType, String sPOSName,
                                                     HashMap<String,String> dataItem) throws Exception {
        return postToSyncService(sSyncServiceRequestType,sPOSName, dataItem,null);
    }
    public boolean getSyncIncData(HashMap<String, String> posClientInformation) throws Exception {
        try {
            HashMap<String,Object> incSyncDataFromServer=
                    postToSyncService(REQUEST_TYPE_GET_INC_DATA, posClientInformation.get("POS.clientSyncName"));
            if(incSyncDataFromServer==null)
                throw new Exception("Failed get sync data inc from server! Reason: no server response data!");
            if(incSyncDataFromServer!=null&&incSyncDataFromServer.get("error")!=null)
                throw new Exception("Failed get sync data inc from server! Reason:"+incSyncDataFromServer.get("error").toString());
            if(incSyncDataFromServer!=null&&incSyncDataFromServer.get("item")==null) return false;
            return true;
        } catch (Exception e){
            throw new Exception("Failed request to get sync inc data! Reason:"+e.getLocalizedMessage());
        }
    }
    public HashMap<String,Object> storeSyncOutData(HashMap<String, String> posClientInformation,
                                    HashMap<String, String> posClientOutData,
                                    HashMap<String, String> posClientOutDataValues) throws Exception {
        try {
            HashMap<String,Object> outSyncDataResult=
                    postToSyncService(REQUEST_TYPE_STORE_OUT_DATA, posClientInformation.get("POS.clientSyncName"),
                            posClientOutData,posClientOutDataValues);
            return outSyncDataResult;
        } catch (Exception e){
            throw new Exception("Failed request to store sync out data! Reason:"+e.getLocalizedMessage());
        }
    }
    public HashMap<String,Object> applySyncOutData(HashMap<String, String> posClientInformation,
                                    HashMap<String, String> posClientOutData) throws Exception {
        try {
            HashMap<String,Object> outSyncDataResult=
                    postToSyncService(REQUEST_TYPE_APPLY_OUT_DATA, posClientInformation.get("POS.clientSyncName"),posClientOutData);
            return outSyncDataResult;
        } catch (Exception e){
            throw new Exception("Failed request to apply sync out data! Reason:"+e.getLocalizedMessage());
        }
    }
}
