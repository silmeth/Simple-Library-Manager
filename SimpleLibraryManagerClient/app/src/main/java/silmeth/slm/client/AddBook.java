package silmeth.slm.client;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by silmeth on 09.02.15.
 */
public class AddBook extends ActionBarActivity implements AdapterView.OnItemSelectedListener {
    int defColor;

    public final int newItem = -2;
    public final int unknownItem = -1;

    private SharedPreferences sharedPref;
    private String sessionCookie;
    private Boolean canManageBooks;
    private String SLMHostName;
    private String SLMPort;
    private String httpReqRes;

    Spinner titleSpinner;
    ArrayAdapter<String> titleAdapter;
    EditText titleEditText;
    List<String> titlesList;
    List<Integer> titlesIdList;

    Spinner authorSpinner;
    ArrayAdapter<String> authorAdapter;
    EditText authorEditText;
    List<String> authorsList;
    List<Integer> authorsIdList;

    Spinner publisherSpinner;
    ArrayAdapter<String> publisherAdapter;
    EditText publisherEditText;
    List<String> publishersList;
    List<Integer> publishersIdList;

    EditText ISBN10EditText;

    EditText ISBN13EditText;

    EditText pubyearEditText;

    String title = null;
    String author = null;
    int authorId = newItem;
    String publisher = null;
    int publisherId = newItem;

    JSONObject newBook;

    private void disableEditText(EditText et) {
        et.setEnabled(false);
        et.setClickable(false);
        et.setFocusable(false);
    }

    private void enableEditText(EditText et) {
        et.setFocusableInTouchMode(true);
        et.setFocusable(true);
        et.setClickable(true);
        et.setEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addbook);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sessionCookie = sharedPref.getString("session_cookie", "sessionid=null");
        SLMHostName = sharedPref.getString("pref_slm_host", "");
        SLMPort = sharedPref.getString("pref_slm_host_port", "");
        canManageBooks = sharedPref.getBoolean("can_manage_books", false);

        titleSpinner = (Spinner) findViewById(R.id.titleSpinner);
        titleEditText = (EditText) findViewById(R.id.titleEditText);
        enableEditText(titleEditText);

        authorSpinner = (Spinner) findViewById(R.id.authorSpinner);
        authorEditText = (EditText) findViewById(R.id.authorEditText);
        disableEditText(authorEditText);
        defColor = authorEditText.getCurrentTextColor();

        publisherSpinner = (Spinner) findViewById(R.id.publisherSpinner);
        publisherEditText = (EditText) findViewById(R.id.publisherEditText);
        disableEditText(publisherEditText);

        ISBN10EditText = (EditText) findViewById(R.id.ISBN10EditText);
        ISBN13EditText = (EditText) findViewById(R.id.ISBN13EditText);
        pubyearEditText = (EditText) findViewById(R.id.pubyearEditText);

        // Initiate Lists for spinners
        titlesList = new ArrayList<>();
        titlesIdList = new ArrayList<>();

        authorsList = new ArrayList<>();
        authorsIdList = new ArrayList<>();

        publishersList = new ArrayList<>();
        publishersIdList = new ArrayList<>();

        // Populate the lists
        Bundle extras = getIntent().getExtras();
        String titleSearchJSONStr = extras.getString("titleSearch");
        String titleStr = extras.getString("title");
        String authorSearchJSONStr = extras.getString("authorSearch");
        String authorStr = extras.getString("author");
        String publisherSearchJSONStr = extras.getString("publisherSearch");
        String publisherStr = extras.getString("publisher");

        if(titleSearchJSONStr != null) {
            try {
                JSONArray titleJSONArray = new JSONArray(titleSearchJSONStr);

                for (int i = 0; i < titleJSONArray.length(); ++i) {
                    JSONObject titleJSON = titleJSONArray.getJSONObject(i);
                    if (!titlesList.contains(titleJSON.getString("title"))) {
                        titlesList.add(titleJSON.getString("title"));
                        titlesIdList.add(titleJSON.getInt("book_id"));
                    }

                    if (!authorsIdList.contains(titleJSON.getInt("author_id"))) {
                        authorsList.add(titleJSON.getString("author"));
                        authorsIdList.add(titleJSON.getInt("author_id"));
                    }

                    if (!publishersIdList.contains(titleJSON.getInt("publisher_id"))) {
                        publishersList.add(titleJSON.getString("publisher"));
                        publishersIdList.add(titleJSON.getInt("publisher_id"));
                    }

                    // TODO add pubYear and ISBNs (?)
                }
            } catch(JSONException e) {
                e.printStackTrace(); // TODO add better exception handling
            }
        }

        if(titleStr != null && !titlesList.contains(titleStr)) {
            titlesList.add(1, titleStr);
            titlesIdList.add(1, unknownItem);
        }

        titlesList.add(getString(R.string.entNewTitle));
        titlesIdList.add(newItem);

        if(authorSearchJSONStr != null) {
            try {
                JSONArray authorJSONArray = new JSONArray(authorSearchJSONStr);
                for (int i = 0; i < authorJSONArray.length(); ++i) {
                    JSONObject authorJSON = authorJSONArray.getJSONObject(i);

                    if (!authorsIdList.contains(authorJSON.getInt("author_id"))) {
                        authorsList.add(authorJSON.getString("name"));
                        authorsIdList.add(authorJSON.getInt("author_id"));
                    }
                }
            } catch(JSONException e) {
                e.printStackTrace(); // TODO add better exception handling
            }
        }

        if(authorStr != null && !authorsList.contains(authorStr)) {
            authorsList.add(1, authorStr);
            authorsIdList.add(1, unknownItem);
        }

        authorsList.add(getString(R.string.entNewAuthor));
        authorsIdList.add(newItem);
        authorId = authorsIdList.get(0);

        if(publisherSearchJSONStr != null) {
            try {
                JSONArray publisherJSONArray = new JSONArray(publisherSearchJSONStr);
                for (int i = 0; i < publisherJSONArray.length(); ++i) {
                    JSONObject publisherJSON = publisherJSONArray.getJSONObject(i);

                    if (!publishersIdList.contains(publisherJSON.getInt("publisher_id"))) {
                        publishersList.add(publisherJSON.getString("name"));
                        publishersIdList.add(publisherJSON.getInt("publisher_id"));
                    }
                }
            } catch(JSONException e) {
                e.printStackTrace(); // TODO add better exception handling
            }
        }

        if(publisherStr != null && !publishersList.contains(publisherStr)) {
            publishersList.add(1, publisherStr);
            publishersIdList.add(1, unknownItem);
        }

        publishersList.add(getString(R.string.entNewPublisher));
        publishersIdList.add(newItem);
        publisherId = publishersIdList.get(0);

        if(extras.getString("ISBN10") != null) {
            ISBN10EditText.setText(extras.getString("ISBN10"));
        }
        if(extras.getString("ISBN13") != null) {
            ISBN13EditText.setText(extras.getString("ISBN13"));
        }
        if(extras.getString("pubYear") != null) {
            pubyearEditText.setText(extras.getString("pubYear"));
        }

        // Initiate spinners and their adapters
        titleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, titlesList);
        titleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        titleSpinner.setAdapter(titleAdapter);
        titleSpinner.setOnItemSelectedListener(this);

        authorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, authorsList);
        authorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        authorSpinner.setAdapter(authorAdapter);
        authorSpinner.setOnItemSelectedListener(this);

        publisherAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                publishersList
        );
        publisherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        publisherSpinner.setAdapter(publisherAdapter);
        publisherSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_addbook, menu);
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

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner = (Spinner) parent;
        if(spinner.getId() == titleSpinner.getId()) {
            if(titlesIdList.get(pos) == newItem) {
//                titleEditText.setText("");
                titleEditText.requestFocus();
            } else {
                titleEditText.setText(titlesList.get(pos));
            }
        } else if(spinner.getId() == authorSpinner.getId()) {
            authorId = authorsIdList.get(pos);
            if (authorId == newItem) {
                enableEditText(authorEditText);
                authorEditText.setTextColor(Color.RED);
                authorEditText.requestFocus();
            } else if(authorId == unknownItem) {
                disableEditText(authorEditText);
                authorEditText.setTextColor(Color.RED);
                authorEditText.setText("(NEW) " + authorsList.get(pos));
                author = authorsList.get(pos);
            } else {
                disableEditText(authorEditText);
                authorEditText.setTextColor(defColor);
                authorEditText.setText(authorsList.get(pos));
                author = authorsList.get(pos);
            }
        } else if(spinner.getId() == publisherSpinner.getId()) {
            publisherId = publishersIdList.get(pos);
            if (publisherId == newItem) {
                enableEditText(publisherEditText);
                publisherEditText.setTextColor(Color.RED);
                publisherEditText.requestFocus();
            } else if(publisherId == unknownItem) {
                disableEditText(publisherEditText);
                publisherEditText.setTextColor(Color.RED);
                publisherEditText.setText("(NEW) " + publishersList.get(pos));
                publisher = publishersList.get(pos);
            } else {
                disableEditText(publisherEditText);
                publisherEditText.setTextColor(defColor);
                publisherEditText.setText(publishersList.get(pos));
                publisher = publishersList.get(pos);
            }
        }

    }

    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void btConfirmAddBook(View btView) {

        newBook = new JSONObject();
        try {
            title = titleEditText.getText().toString();
            newBook.put("title", title);

            newBook.put("author", true);
            if(authorId == newItem) {
                newBook.put("author_new", true);
                newBook.put("author_name", authorEditText.getText());
            } else if(authorId == unknownItem) {
                newBook.put("author_new", true);
                newBook.put("author_name", author);
            } else {
                newBook.put("author_new", false);
                newBook.put("author_id", authorId);
            }

            newBook.put("publisher", true);
            if(publisherId == newItem) {
                newBook.put("publisher_new", true);
                newBook.put("publisher_name", publisherEditText.getText());
            } else if(publisherId == unknownItem) {
                newBook.put("publisher_new", true);
                newBook.put("publisher_name", publisher);
            } else {
                newBook.put("publisher_new", false);
                newBook.put("publisher_id", publisherId);
            }

            String ISBN13String = ISBN13EditText.getText().toString().replace("-", "");
            String ISBN10String = ISBN10EditText.getText().toString().replace("-", "");

            if(ISBN13String.length() == 13) {
                newBook.put("isbn13", ISBN13String);
            }

            if(ISBN10String.length() == 10) {
                newBook.put("isbn10", ISBN10String);
            }

            if(pubyearEditText.getText().toString().length() > 0) {
                Integer pubYear = Integer.valueOf(pubyearEditText.getText().toString());
                newBook.put("pub_date", pubYear.intValue());
            }

            HttpRequestTask httpReqTask = new HttpRequestTask();
            httpReqTask.execute("http://" + SLMHostName + ":" + SLMPort + "/webs/add_book");

            while(!httpReqTask.complete) {

            }
        } catch(JSONException e) {
            e.printStackTrace(); // TODO better exception handling
        }

        JSONArray respArray;
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setCancelable(false);
        final Intent i = new Intent(this, MainActivity.class);
        try {
            respArray = new JSONArray(httpReqRes);
            JSONObject respObj = respArray.getJSONObject(0);
            if(respObj.getString("title").equals(title)) {
                alertDialogBuilder.setTitle("Success!");
                alertDialogBuilder.setMessage("Book " + title + " added successfully.");
                alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setResult(RESULT_OK, i);
                        finish();
                    }
                });
            }
            else throw new JSONException("other");
        } catch(JSONException e) {
            alertDialogBuilder.setTitle("There was problem");
            alertDialogBuilder.setMessage(httpReqRes);

            alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setResult(RESULT_CANCELED, i);
                    finish();
                }
            });
            e.printStackTrace();
        }
        alertDialogBuilder.create().show();

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
                HttpPost request = new HttpPost();

                request.setURI(new URI(url));
                request.setEntity(new StringEntity(newBook.toString(), "UTF-8"));
                request.setHeader("Cookie", sessionCookie);
                request.setHeader("Content-type", "application/json; charset=utf-8");

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

            httpReqRes = result;
            complete = true;
            return result;
        }
    }
}
