package silmeth.slm.client;

import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by silmeth on 26.02.15.
 */
public class HttpPostRequestTask extends AsyncTask<Object, Void, Object[]> {
    public final int respObj = 0;
    public final int respStr = 1;

    @Override
    protected Object[] doInBackground(Object... objs) {
        String url = null;
        String cookie = "sessionid=null";
        String content = "";
        String conType = "text/plain";
        if(objs.length == 4) {
            url = (String) objs[0];
            content = (String) objs[1];
            cookie = (String) objs[2];
            conType = (String) objs[3];
        }
        String result = null;
        HttpResponse response = null;
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost request = new HttpPost();

            request.setURI(new URI(url));
            request.setEntity(new StringEntity(content, "UTF-8"));
            request.setHeader("Cookie", cookie);
            request.setHeader("Content-type", conType);

            response = httpClient.execute(request);

            BufferedReader buff = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent())
            );
            StringBuffer sbuff = new StringBuffer("");

            String line;

            String NL = System.getProperty("line.separator");

            while ((line = buff.readLine()) != null) {
                sbuff.append(line); sbuff.append(NL);
            }

            buff.close();

            result = sbuff.toString();

        } catch (URISyntaxException | IOException e) {
            e.printStackTrace(); // TODO more reasonable exception handling
                                 // TODO especially ConnectException errors
        }

        Object[] returnVal = new Object[Math.max(respObj, respStr)+1];
        returnVal[respObj] = response;
        returnVal[respStr] = result;
        return returnVal;
    }
}
