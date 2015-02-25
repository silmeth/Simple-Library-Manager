package silmeth.slm.client;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;


public class DBSearch extends ActionBarActivity {
    private SharedPreferences sharedPref;
    private String localHostName;

    private TextView ISBNView;
    private EditText queryView;

    private String dlBookInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dbsearch);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        localHostName = sharedPref.getString("pref_slm_host", "");

        ISBNView = (TextView) findViewById(R.id.ISBNcontent);
        ISBNView.setText(getIntent().getExtras().getString("ISBN"));
        queryView = (EditText) findViewById(R.id.query);
        queryView.setText(ISBNView.getText());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_dbsearch, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // TODO: redundant search functions
    public void btSearchByISBN(View btView) {
        BookInfo[] results = null;
        String ISBN = null;
        try {
            ISBN = URLEncoder.encode(queryView.getText().toString(), "UTF-8");
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        assert(ISBN != null);
        HttpRequestTask httpReqTask = new HttpRequestTask();
        httpReqTask.execute("http://" + localHostName + ":8000/webs/get_books/isbn/" + ISBN);
        while(!httpReqTask.complete) {

        }
        String page = dlBookInfo;
        if(page == null) return;
        try {
            JSONArray jsonArray = new JSONArray(page);
            results = new BookInfo[jsonArray.length()];
            for(int i = 0; i < jsonArray.length(); ++i) {
                JSONObject book = jsonArray.getJSONObject(i);
                results[i] = new BookInfo();
                results[i].title = book.getString("title");
                results[i].author = book.getString("author");
                results[i].isbn10 = book.getString("isbn10");
                results[i].isbn13 = book.getString("isbn13");
            }
        } catch(JSONException e) {
            e.printStackTrace(); // TODO more reasonable exception handling
        }

        MainActivity.booksArray = results;
        Intent i = new Intent(this, MainActivity.class);
        setResult(RESULT_OK, i);
        finish();
    }

    public void btSearchByTitle(View btView) {
        BookInfo[] results = null;
        String query = null;
        try {
            query = URLEncoder.encode(queryView.getText().toString(), "UTF-8");
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace(); // TODO more reasonable exception handling
        }
        assert(query != null);
        HttpRequestTask httpReqTask = new HttpRequestTask();
        httpReqTask.execute("http://" + localHostName + ":8000/webs/search/title=" + query);
        while(!httpReqTask.complete) {

        }
        String page = dlBookInfo;
        if(page == null) return;
        try {
            JSONArray jsonArray = new JSONArray(page);
            results = new BookInfo[jsonArray.length()];
            for(int i = 0; i < jsonArray.length(); ++i) {
                JSONObject book = jsonArray.getJSONObject(i);
                results[i] = new BookInfo();
                results[i].title = book.getString("title");
                results[i].author = book.getString("author");
                results[i].isbn10 = book.getString("isbn10");
                results[i].isbn13 = book.getString("isbn13");
                results[i].similarity = new Float(book.getDouble("similarity"));
            }
        } catch(JSONException e) {
            e.printStackTrace(); // TODO more reasonable exception handling
        }

        MainActivity.booksArray = results;
        Intent i = new Intent(this, MainActivity.class);
        setResult(RESULT_OK, i);
        finish();
    }

    public void btSearchByAuthor(View btView) {
        BookInfo[] results = null;
        String query = null;
        try {
            query = URLEncoder.encode(queryView.getText().toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        assert(query != null);
        HttpRequestTask httpReqTask = new HttpRequestTask();
        httpReqTask.execute("http://" + localHostName + ":8000/webs/search/author=" + query);
        while(!httpReqTask.complete) {

        }
        String page = dlBookInfo;
        if(page == null) return;
        try {
            JSONArray jsonArray = new JSONArray(page);
            results = new BookInfo[jsonArray.length()];
            for(int i = 0; i < jsonArray.length(); ++i) {
                JSONObject book = jsonArray.getJSONObject(i);
                results[i] = new BookInfo();
                results[i].title = book.getString("title");
                results[i].author = book.getString("author");
                results[i].isbn10 = book.getString("isbn10");
                results[i].isbn13 = book.getString("isbn13");
                results[i].similarity = new Float(book.getDouble("similarity"));
            }
        } catch(JSONException e) {
            e.printStackTrace(); // TODO more reasonable exception handling
        }

        MainActivity.booksArray = results;
        Intent i = new Intent(this, MainActivity.class);
        setResult(RESULT_OK, i);
        finish();
    }

    private class HttpRequestTask extends AsyncTask<String, Void, String> {
        public boolean complete = false;

        @Override
        protected String doInBackground(String... urls) {
            complete = false;
            String url = null;
            if(urls.length > 0) url = urls[0];
            String result = null;
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet request = new HttpGet();
                request.setURI(new URI(url));
                HttpResponse response = httpClient.execute(request);

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
            }
            dlBookInfo = result;
            complete = true;
            return dlBookInfo;
        }
    }

    private String localSearch(String url) {
        String result;
        HttpRequestTask httpReqTask = new HttpRequestTask();
        httpReqTask.execute(url);
        while (!httpReqTask.complete) {

        }
        result = dlBookInfo;
        MainActivity.dlBookInfo = dlBookInfo;
        return result;
    }
}
