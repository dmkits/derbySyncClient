package derbysyncclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by dmkits on 23.10.17.
 */
public class JSONRequest {

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
        request.addHeader("Accept-Charset", "UTF-8");
        gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZ").create();
    }

    public HashMap<String,Object> post(HashMap<String,Object> data) throws Exception {
        try {
            request.setEntity(new StringEntity(gson.toJson(data)));
            CloseableHttpResponse response= httpClient.execute(request);
            Header[] respHeaders= response.getAllHeaders();
            boolean hasJSONHeader= false;
            for(int i=0; i<respHeaders.length; i++){
                String sHeaderType= respHeaders[i].getName();
                String sHeader= respHeaders[i].getValue();
                if(("Content-Type".equals(sHeaderType)||"content-type".equals(sHeaderType))
                        && sHeader.contains("application/json")){
                    hasJSONHeader=true; break;
                }
            }
            String sRespData=null;
            if(hasJSONHeader) sRespData= EntityUtils.toString(response.getEntity(), "UTF-8");
            response.close();
            if(!hasJSONHeader)
                throw new Exception("Failed JSON request! Reason: response no JSON content!");
            Result respData = gson.fromJson(sRespData, Result.class);
            if(respData==null)
                throw new Exception("Failed JSON request! Reason: no JSON data!");
            return respData.getResult();
        } catch (Exception ex) {
            throw new Exception("Failed JSON request! Reason:"+ex.getLocalizedMessage());
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
