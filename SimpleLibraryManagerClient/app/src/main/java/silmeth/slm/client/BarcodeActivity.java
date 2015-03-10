package silmeth.slm.client;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class BarcodeActivity extends ActionBarActivity {
    public final int barScanId = 0;
    public final int localSearchId = 1;
    public final int addBookId = 2;
    public final int loginId = 3;
    public final int settingsId = 4;

    private SharedPreferences sharedPref;
    private String SLMHostName;
    private String SLMPort;
    private Boolean loggedIn;
    private String sessionCookie;

    private EditText titleEditText;
    private EditText authorEditText;
    private EditText publisherEditText;
    private EditText ISBN10EditText;
    private EditText ISBN13EditText;
    private EditText pubYearEditText;

    private String ISBN = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SLMHostName = sharedPref.getString("pref_slm_host", "");
        SLMPort = sharedPref.getString("pref_slm_host_port", "");

        loggedIn = sharedPref.getBoolean("logged_in", false);
        sessionCookie = sharedPref.getString("session_cookie", "sessionid=null");

        titleEditText = (EditText) findViewById(R.id.titleEditText);
        authorEditText = (EditText) findViewById(R.id.authorEditText);
        publisherEditText = (EditText) findViewById(R.id.publisherEditText);
        ISBN10EditText = (EditText) findViewById(R.id.ISBN10EditText);
        ISBN13EditText = (EditText) findViewById(R.id.ISBN13EditText);
        pubYearEditText = (EditText) findViewById(R.id.pubYearEditText);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_barcode, menu);
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
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == barScanId) { // ZXing Barcode Scanner Activity
            if (resultCode == RESULT_OK) {
                String isbn = data.getStringExtra("SCAN_RESULT");
                int isbnLen = isbn.length();

                if((isbnLen == 10 || isbnLen == 13)) {
                    switch(isbnLen) {
                        case 10:
                            ISBN10EditText.setText(isbn);
                            ISBN13EditText.setText("");
                            break;
                        case 13:
                            ISBN13EditText.setText(isbn);
                            ISBN10EditText.setText("");
                            break;
                    }
                    if(ISBN != null && !ISBN.equals(isbn)) { // new ISBN read, do empty book data
                        titleEditText.setText("");
                        authorEditText.setText("");
                        publisherEditText.setText("");
                        pubYearEditText.setText("");
                    }
                    ISBN = isbn;
                    try {
                        searchGoogleBooks(); // find book data in Google Books
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                } // TODO what if scanned non-ISBN code?

            } else if (resultCode == RESULT_CANCELED) {
            } // TODO Handle error in reasonable way
        }
    }

    public void btBarCode(View btView) {
        boolean bcScannerInstalled = true;
        PackageManager pkgMng = getPackageManager();
        Intent barScanInt = new Intent("com.google.zxing.client.android.SCAN");

        try { // check if ZXing Scanner installed
            pkgMng.getPackageInfo("com.google.zxing.client.android", PackageManager.GET_ACTIVITIES);
            barScanInt.setPackage("com.google.zxing.client.android");
        } catch(PackageManager.NameNotFoundException e) {
            // if not installed, check for other apps able to handle scanning
            List<ResolveInfo> activities = pkgMng.queryIntentActivities(barScanInt, 0);
            if(activities != null && activities.isEmpty()) {
                // if nothing installed, prompt to install
                bcScannerInstalled = false;
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle(getString(R.string.barcode_not_installed));
                alertDialogBuilder.setMessage(getString(R.string.barcode_not_installed_msg));
                alertDialogBuilder.setCancelable(false);
                alertDialogBuilder.setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(
                                Uri.parse("market://details?id=com.google.zxing.client.android")
                        );
                        startActivity(intent);
                    }
                });
                alertDialogBuilder.setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                alertDialogBuilder.create().show();
            }
        }

        // if everything OK, run scanner
        if(bcScannerInstalled) {
            barScanInt.putExtra("SCAN_FORMATS", "EAN_13");
            startActivityForResult(barScanInt, barScanId);
        }
    }

    private void searchGoogleBooks() throws Exception {
        if(ISBN == null || ISBN.isEmpty()) {
            throw new Exception("No ISBN to parse");
        }
        try {
            ISBN = URLEncoder.encode(ISBN, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }

        HttpGetRequestTask httpReqTask = new HttpGetRequestTask();

        Object[] result = {null, null};
        try {
            result = httpReqTask.execute(
                    "https://www.googleapis.com/books/v1/volumes?q=isbn:" + ISBN,
                    ""
            ).get(2, TimeUnit.SECONDS);
        } catch(InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch(TimeoutException e) {
            httpReqTask.cancel(true);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setTitle(getString(R.string.connection_timeout));
            alertDialogBuilder.setMessage(getString(R.string.google_timeout_msg));
            alertDialogBuilder.create().show();
            e.printStackTrace();
            return;
        }

        String page = (String) result[httpReqTask.respStr];

        if(page == null) return;
        try {
            JSONObject jsonObj = new JSONObject(page);
            JSONArray jsonArray = jsonObj.getJSONArray("items");

            // TODO someday in the future - let the user choose
            // if more than one book found (eg. in new activity with spinner)

            // parse only first found book
            if(jsonArray.length() > 0) {
                JSONObject book = jsonArray.getJSONObject(0).getJSONObject("volumeInfo");
                try {
                    titleEditText.setText(book.getString("title"));
                } catch(JSONException e) {
                    e.printStackTrace();
                }
                String author = "";
                try {
                    for(int j = 0; j < book.getJSONArray("authors").length(); ++j) {
                        author += book.getJSONArray("authors").getString(j);
                        if(j < book.getJSONArray("authors").length()-1)
                            author += ", ";
                    }

                } catch(JSONException e) {
                    e.printStackTrace();
                }
                if(!author.isEmpty()) {
                    authorEditText.setText(author);
                }

                try {
                    publisherEditText.setText(book.getString("publisher"));
                } catch(JSONException e) {
                    e.printStackTrace();
                }

                try {
                    JSONArray isbnsArray = book.getJSONArray("industryIdentifiers");
                    String isbn10 = "";
                    String isbn13 = "";
                    try {
                        for (int j = 0; j < isbnsArray.length(); j++) {
                            if (isbnsArray.getJSONObject(j).getString("type").equals("ISBN_10"))
                                isbn10 = isbnsArray.getJSONObject(j).getString("identifier");
                            else if (isbnsArray.getJSONObject(j).getString("type").equals("ISBN_13"))
                                isbn13 = isbnsArray.getJSONObject(j).getString("identifier");
                        }
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                    if (!isbn10.isEmpty()) {
                        ISBN10EditText.setText(isbn10);
                    }
                    if (!isbn13.isEmpty()) {
                        ISBN13EditText.setText(isbn13);
                    }
                } catch(JSONException e) {
                    e.printStackTrace();
                }

                try {
                    String[] dateArray = book.getString("publishedDate").split("-");
                    if (dateArray.length > 0) {
                        pubYearEditText.setText(dateArray[0]);
                    }
                } catch(JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch(JSONException e) {
            e.printStackTrace(); // TODO more reasonable exception handling
        }
    }

    public void btConfirm(View btView) {
        String title = titleEditText.getText().toString();
        String author = authorEditText.getText().toString();
        String publisher = publisherEditText.getText().toString();
        String pubYear = pubYearEditText.getText().toString();
        String ISBN10 = ISBN10EditText.getText().toString();
        String ISBN13 = ISBN13EditText.getText().toString();

        Intent i = new Intent(this, AddBook.class);

        i.putExtra("type", "add_new"); // to be used later to differentiate between add/edit book

        try {
            if(!title.isEmpty()) {
                i.putExtra("titleSearch", searchTitles());
                i.putExtra("title", title);
            }

            if(!author.isEmpty()) {
                i.putExtra("authorSearch", searchAuthors());
                i.putExtra("author", author);
            }

            i.putExtra("publisherSearch", searchPublishers()); // list all publishers always

            if(!publisher.isEmpty()) {

                i.putExtra("publisher", publisher);
            }

            if(!pubYear.isEmpty()) {
                i.putExtra("pubYear", pubYear);
            }

            if(!ISBN10.isEmpty()) {
                i.putExtra("ISBN10", ISBN10);
            }

            if(!ISBN13.isEmpty()) {
                i.putExtra("ISBN13", ISBN13);
            }

            startActivityForResult(i, addBookId);
        } catch(Exception e) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setTitle(getString(R.string.connection_timeout));
            alertDialogBuilder.setMessage(getString(R.string.slm_timeout_msg));
            alertDialogBuilder.create().show();
            e.printStackTrace();
        }
    }

    // Using first found book if found by Google Books
    public String searchAuthors() throws Exception {
        if(!authorEditText.getText().toString().isEmpty()) {
            return search(authorEditText.getText().toString(), "_authors/");
        } else {
            return "[ ]"; // empty JSONArray
        }
    }

    // Using first found book if found by Google Books
    public String searchPublishers() throws Exception {
        if (!publisherEditText.getText().toString().isEmpty()) {
            return search(publisherEditText.getText().toString(), "_publishers/");
        } else {
            return search("+", "_publishers/"); // list all publishers anyway
        }
    }

    // Using first found book if found by Google Books
    public String searchTitles() throws Exception {
        if(!titleEditText.getText().toString().isEmpty()) {
            return search(titleEditText.getText().toString(), "/title=");
        } else {
            return "[ ]"; // empty JSONArray
        }
    }

    private String search(String query, String what) throws Exception {
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
                    "http://" + SLMHostName + ":" + SLMPort + "/webs/search" + what + query,
                    sessionCookie
            ).get(2, TimeUnit.SECONDS);
            if(result[httpReqTask.respStr] == null)
                throw new TimeoutException("Request returned null");
        } catch(InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } catch(TimeoutException e) {
            httpReqTask.cancel(true);

            e.printStackTrace();
            throw new Exception(e.getMessage());
        }

        return (String) result[httpReqTask.respStr];
    }
}
