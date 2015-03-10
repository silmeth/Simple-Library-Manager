package silmeth.slm.client;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class LoginActivity extends ActionBarActivity {
    public final int settingsId = 0;

    private SharedPreferences sharedPref;
    private String SLMHostName;
    private String SLMPort;
    private String sessionCookie;

    private EditText usernameEditText;
    private EditText passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SLMHostName = sharedPref.getString("pref_slm_host", "");
        SLMPort = sharedPref.getString("pref_slm_host_port", "");

        usernameEditText = (EditText) findViewById(R.id.usernameEditText);
        passwordEditText = (EditText) findViewById(R.id.passwordEditText);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
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
            Intent i = new Intent(this, SettingsActivity.class);
            startActivityForResult(i, settingsId);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == settingsId) {
            SLMHostName = sharedPref.getString("pref_slm_host", "");
            SLMPort = sharedPref.getString("pref_slm_host_port", "");
        }
    }

    public void btLoginConfirm(View btView) {
        SharedPreferences.Editor edit = sharedPref.edit();
        JSONObject credentials = new JSONObject();
        try {
            credentials.put("username", usernameEditText.getText());
            credentials.put("password", passwordEditText.getText());
        } catch(JSONException e) {
            e.printStackTrace();
        }

        HttpRequestTask httpReqTask = new HttpRequestTask();
        String response = null;
        try {
            response = httpReqTask.
                    execute("http://" + SLMHostName + ":" + SLMPort + "/webs/login", credentials).
                    get(2, TimeUnit.SECONDS);
        } catch(InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch(TimeoutException e) {
            httpReqTask.cancel(true);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setTitle(getString(R.string.connection_timeout));
            alertDialogBuilder.setMessage(getString(R.string.login_timeout));
            alertDialogBuilder.create().show();
            e.printStackTrace();
        }
        if(response != null) {
            if (!response.contains("error") && response.contains("logged_in")) {
                try {
                    JSONObject responseJson = new JSONObject(response);
                    if (responseJson.getBoolean("logged_in")) {
                        Boolean canManageBooks;
                        try {
                            canManageBooks = responseJson.getBoolean("can_manage_books");
                        } catch(JSONException e) {
                            canManageBooks = false;
                            e.printStackTrace();
                        }
                        Boolean canLend;
                        try {
                            canLend = responseJson.getBoolean("can_lend");
                        } catch(JSONException e) {
                            canLend = false;
                            e.printStackTrace();
                        }
                        Boolean canBorrow;
                        try {
                            canBorrow = responseJson.getBoolean("can_borrow");
                        } catch(JSONException e) {
                            canBorrow = false;
                            e.printStackTrace();
                        }
                        edit.putBoolean("logged_in", responseJson.getBoolean("logged_in"));
                        edit.putString("session_cookie", sessionCookie);

                        edit.putBoolean("can_manage_books", canManageBooks);
                        edit.putBoolean("can_lend", canLend);
                        edit.putBoolean("can_borrow", canBorrow);

                        edit.apply();

                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                        alertDialogBuilder.setCancelable(false);
                        alertDialogBuilder.setMessage(getString(R.string.logged_as) +
                                " " + responseJson.getString("username"));
                        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setResult(RESULT_CANCELED);
                                finish();
                            }
                        });
                        alertDialogBuilder.create().show();

                    } else {
                        System.out.println("Something strange happened!");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                edit.putBoolean("logged_in", false);
                edit.apply();
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setCancelable(true);
                alertDialogBuilder.setTitle(getString(R.string.error));
                if (response.contains("wrong credentials")) {
                    alertDialogBuilder.setMessage(getString(R.string.wrong_cred));
                } else if (response.contains("user inactive")) {
                    alertDialogBuilder.setMessage(getString(R.string.user_inactive));
                } else if (response.contains("not a valid json")) {
                    alertDialogBuilder.setMessage(getString(R.string.invalid_json));
                } else {
                    alertDialogBuilder.setMessage(response);
                }

                alertDialogBuilder.create().show();
            }
        }
    }

    private class HttpRequestTask extends AsyncTask<Object, Void, String> {
        @Override
        protected String doInBackground(Object... objs) {
            String url = null;
            JSONObject credentials = null;
            if(objs.length == 2) {
                url = (String) objs[0];
                credentials = (JSONObject) objs[1];
            } else {
                throw new IllegalArgumentException("Wrong arg number");
            }
            String result = null;
            try {
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpPost request = new HttpPost();

                request.setURI(new URI(url));
                request.setEntity(new StringEntity(credentials.toString()));
                request.setHeader("Cookie", sessionCookie);
                request.setHeader("Content-type", "application/json; charset=utf-8");

                HttpResponse response = httpClient.execute(request);

                List<Cookie> cookieList = httpClient.getCookieStore().getCookies();

                for(Cookie c: cookieList) {
                    if(c.getName().equals("sessionid")) {
                        sessionCookie = "sessionid=" + c.getValue();
                    }
                }

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

            } catch (URISyntaxException | IOException | IllegalArgumentException e) {
                e.printStackTrace(); // TODO more reasonable exception handling
            }

            return result;
        }
    }
}
