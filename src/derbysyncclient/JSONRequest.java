package derbysyncclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by dmkits on 23.10.17.
 */
public class JSONRequest {

    private static final Logger logger = Logger.getLogger("derbysyncclient.JSONRequest");
    CloseableHttpClient httpClient=null;
    String url;
    HttpPost request= null;
    Gson gson= null;

    public JSONRequest(String url) {
        httpClient = HttpClientBuilder.create().build();
        this.url= url;
        request = new HttpPost(url);
        request.addHeader("Content-Type", "application/json;charset=UTF-8");
        request.addHeader("Accept","application/json");
        request.addHeader("Accept-Charset", "UTF-8"); //
        gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
    }

    public HashMap<String,Object> post(HashMap<String,Object> data) throws Exception {
        CloseableHttpResponse response=null;
        try {
            String sEntity=gson.toJson(data); logger.log(Level.FINE, "Request entity: {0}", sEntity);
            StringEntity reqEntity=new StringEntity(sEntity, ContentType.APPLICATION_JSON);
            request.setEntity(reqEntity);
            response= httpClient.execute(request);
            Header[] respHeaders= response.getAllHeaders();
            String sContentType=null;
            boolean hasJSONHeader= false;
            for(int i=0; i<respHeaders.length; i++){
                String sHeaderType= respHeaders[i].getName();
                String sHeader= respHeaders[i].getValue();
                if("Content-Type".equals(sHeaderType)) sContentType= sHeader;
                else if("content-type".equals(sHeaderType)) sContentType= sHeader;
                if(sContentType!=null&&sContentType.contains("application/json")) {
                    hasJSONHeader=true; break;
                }
            }
            String sRespData= EntityUtils.toString(response.getEntity(), "UTF-8");
            logger.log(Level.FINE, "Getted response from server url"+url+": \n body={0}", sRespData);
            if(!hasJSONHeader&&sRespData!=null) sRespData= sRespData.substring(0,1020)+" ...";
            if(!hasJSONHeader)
                throw new Exception("Response no JSON content! \n Content-type:"+sContentType+" Response content:"+sRespData);
            try {
                Result respData = gson.fromJson(sRespData, Result.class);
                if(respData==null)
                    throw new Exception("No JSON data!");
                return respData.getResult();
            } catch (Exception e){
                throw new Exception("Failed get data from JSON content! Reason:"+e.getLocalizedMessage());
            }
        } catch (Exception ex) {
            throw new Exception("Failed JSON request! Reason:"+ex.getLocalizedMessage());
        } finally {
            response.close();
        }
    }

    public void close(){
        try {
            httpClient.close();
        } catch (IOException e) {}
    }

    private class Result {
        public String error;
        public HashMap<String,Object> resultItem;
        public HashMap<String,Object> getResult(){
            HashMap<String,Object> result= new HashMap<>();
            result.put("error",error);
            result.put("resultItem",resultItem);
            return result;
        }
    }
}
