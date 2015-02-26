package silmeth.slm.client;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class DBSearch extends ActionBarActivity {
    public final int SLMBookId = 0;

    private SharedPreferences sharedPref;
    private String SLMHostName;
    private String SLMPort;
    private String sessionCookie;

    private TextView ISBNView;
    private EditText queryView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dbsearch);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SLMHostName = sharedPref.getString("pref_slm_host", "");
        SLMPort = sharedPref.getString("pref_slm_host_port", "");
        sessionCookie = sharedPref.getString("session_cookie", "sessionid=null");

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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Intent i = new Intent(this, MainActivity.class);
        setResult(RESULT_OK, i);
        finish();
    }

    public void btSearchByISBN(View btView) {
        String page = search(queryView.getText().toString(), "get_books/isbn/");
        if(page == null) return;

        Intent i = new Intent(this, SLMBookActivity.class);
        i.putExtra("books", page);
        startActivityForResult(i, SLMBookId);
    }

    public void btSearchByTitle(View btView) {
        String page = search(queryView.getText().toString(), "search/title=");
        if(page == null) return;

        Intent i = new Intent(this, SLMBookActivity.class);
        i.putExtra("books", page);
        startActivityForResult(i, SLMBookId);
    }

    public void btSearchByAuthor(View btView) {
        String page = search(queryView.getText().toString(), "search/author=");
        if(page == null) return;

        Intent i = new Intent(this, SLMBookActivity.class);
        i.putExtra("books", page);
        startActivityForResult(i, SLMBookId);
    }

    private String search(String query, String what) {
        try {
            query = URLEncoder.encode(query, "UTF-8");
            what = URLEncoder.encode(what, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        assert(query != null);
        HttpGetRequestTask httpReqTask = new HttpGetRequestTask();

        Object[] result = {null, null};
        try {
            result = httpReqTask.execute(
                    "http://" + SLMHostName + ":" + SLMPort + "/webs/" + what + query,
                    sessionCookie
            ).get(2, TimeUnit.SECONDS);
        } catch(InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch(TimeoutException e) {
            httpReqTask.cancel(true);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setTitle(getString(R.string.connection_timeout));
            alertDialogBuilder.setMessage(getString(R.string.generic_timeout_msg));
            alertDialogBuilder.create().show();
            e.printStackTrace();
            return null;
        }

        return (String) result[httpReqTask.respStr];
    }
}
