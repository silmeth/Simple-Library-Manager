package silmeth.slm.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpGet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {
    public final int barScanId = 0;
    public final int localSearchId = 1;
    public final int addBookId = 2;

    public static String ISBN;
    private EditText editISBN;
    public static BookInfo[] booksArray;
    public static String dlBookInfo; // downloaded info
    private SharedPreferences sharedPref;
    private String localHostName;
//    public static String page = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        localHostName = sharedPref.getString("pref_slm_host", "");

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        refreshBookView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == barScanId) { // ZXing Barcode Scanner Activity
            if(resultCode == RESULT_OK) {
                ISBN = data.getStringExtra("SCAN_RESULT");
                System.out.println(ISBN);
                refreshBookView();
            }
            else if(resultCode == RESULT_CANCELED) {} // TODO Handle error in reasonable way
        } else if(requestCode == localSearchId) {
            if(resultCode == RESULT_OK) {
                refreshBookView();
            }
        }
    }

    public void BtBarCode(View btView) {
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
                // TODO move strings to xml
                bcScannerInstalled = false;
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("Barcode Scanner not installed!");
                alertDialogBuilder.setMessage("You need to install barcode scanner application to" +
                        " use this feature. Do you want to install it now?");
                alertDialogBuilder.setCancelable(false);
                alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(
                                Uri.parse("market://details?id=com.google.zxing.client.android")
                        );
                        startActivity(intent);
                    }
                });
                alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
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

    public void btGoogleSearch(View btView) {
        booksArray = searchGoogleBooks();
        refreshBookView();
    }

    public void btLocalSearch(View btView) {
        Intent i = new Intent(this, DBSearch.class);
        i.putExtra("ISBN", ISBN);
        startActivityForResult(i, localSearchId);
    }

    public void btAddBook(View btView) {
        Intent i = new Intent(this, AddBook.class);
        if(booksArray.length > 0) {
            if(booksArray[0].title != null) {
                i.putExtra("titleSearch", searchTitles());
                i.putExtra("title", booksArray[0].title);
            }
            if(booksArray[0].author != null) {
                i.putExtra("authorSearch", searchAuthors());
                i.putExtra("author", booksArray[0].author);
            }
            if(booksArray[0].publisher != null) {
                i.putExtra("publisherSearch", searchPublishers());
                i.putExtra("publisher", booksArray[0].publisher);
            }
            if(booksArray[0].pubYear != null) {
                i.putExtra("pubYear", booksArray[0].pubYear);
            }

            if(booksArray[0].isbn10 != null) {
                i.putExtra("ISBN10", booksArray[0].isbn10);
            } else if(ISBN != null && ISBN.length() == 10) {
                i.putExtra("ISBN10", ISBN);
            }

            if(booksArray[0].isbn13 != null) {
                i.putExtra("ISBN13", booksArray[0].isbn13);
            } else if(ISBN != null && ISBN.length() == 13) {
                i.putExtra("ISBN13", ISBN);
            }
        }
        startActivityForResult(i, addBookId);
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

    // TODO: get rid of all the redundant search functions
    // This should (almost?) always return only one entry:
    // there should exist only one book with given ISBN in the world.
    private BookInfo[] searchGoogleBooks() {
        BookInfo[] results = null;
        try {
            ISBN = URLEncoder.encode(ISBN, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        assert(ISBN != null);
        HttpRequestTask httpReqTask = new HttpRequestTask();
        httpReqTask.execute("https://www.googleapis.com/books/v1/volumes?q=isbn:" + ISBN);
        while(!httpReqTask.complete) {

        }
        String page = dlBookInfo;
        if(page == null) return null;
        try {
            JSONObject jsonObj = new JSONObject(page);
            JSONArray jsonArray = jsonObj.getJSONArray("items");
            results = new BookInfo[jsonArray.length()];
            for(int i = 0; i < jsonArray.length(); ++i) {
                JSONObject book = jsonArray.getJSONObject(i).getJSONObject("volumeInfo");
                results[i] = new BookInfo();
                results[i].title = book.getString("title");
                String author = "";
                for(int j = 0; j < book.getJSONArray("authors").length(); ++j) {
                    author += book.getJSONArray("authors").getString(j);
                    if(j < book.getJSONArray("authors").length()-1)
                        author += ", ";
                }
                results[i].author = author;
                JSONArray isbnsArray = book.getJSONArray("industryIdentifiers");
                String isbn10 = "";
                String isbn13 = "";
                for(int j = 0; j < isbnsArray.length(); j++) {
                    if(isbnsArray.getJSONObject(j).getString("type").equals("ISBN_10"))
                        isbn10 = isbnsArray.getJSONObject(j).getString("identifier");
                    else if(isbnsArray.getJSONObject(j).getString("type").equals("ISBN_13"))
                        isbn13 = isbnsArray.getJSONObject(j).getString("identifier");
                }
                results[i].isbn10 = isbn10;
                results[i].isbn13 = isbn13;

                String[] dateArray = book.getString("publishedDate").split("-");
                if(dateArray.length > 0) {
                    results[i].pubYear = dateArray[0];
                }
            }
        } catch(JSONException e) {
            e.printStackTrace(); // TODO more reasonable exception handling
        }

        return results;
    }

    // TODO: redundant search
    // Using first found book if found by Google Books
    public String searchAuthors() {
        String query = null;
        if(booksArray.length > 0) {
            query = booksArray[0].author;
        }
        try {
            query = URLEncoder.encode(query, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        assert(query != null);
        HttpRequestTask httpReqTask = new HttpRequestTask();
        httpReqTask.execute("http://" + localHostName + ":8000/webs/search_authors/" + query);
        while(!httpReqTask.complete) {

        }
        return dlBookInfo; // return JSON string with listed authors, their ids and similarities
    }

    // TODO: redundant search
    // Using first found book if found by Google Books
    public String searchPublishers() {
        String query = null;
        if(booksArray.length > 0) {
            query = booksArray[0].publisher;
        }
        try {
            query = URLEncoder.encode(query, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        assert(query != null);
        HttpRequestTask httpReqTask = new HttpRequestTask();
        httpReqTask.execute("http://" + localHostName + ":8000/webs/search_publishers/" + query);
        while(!httpReqTask.complete) {

        }
        return dlBookInfo; // return JSON string with listed publishers, their ids and similarities
    }

    // TODO: redundant search
    // Using first found book if found by Google Books
    public String searchTitles() {
        String query = null;
        BookInfo[] results = null;
        if(booksArray.length > 0) {
            query = booksArray[0].title;
        }
        try {
            query = URLEncoder.encode(query, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        assert(query != null);
        HttpRequestTask httpReqTask = new HttpRequestTask();
        httpReqTask.execute("http://" + localHostName + ":8000/webs/search/title=" + query);
        while(!httpReqTask.complete) {

        }

        return dlBookInfo;
    }

    private void refreshBookView() {
        String NL = System.getProperty("line.separator");
        TextView bookListView = (TextView) findViewById(R.id.bookListView);
        String bookList = "ISBN: " + ISBN + NL + "______" + NL + NL;

        if(booksArray != null && booksArray.length > 0) {
            for (int i = 0; i < booksArray.length; ++i) {
                String bookDetails ="#" + Integer.toString(i + 1) + NL + "____" + NL;
                bookDetails += "title: " + booksArray[i].title + NL;
                bookDetails += "author: " + booksArray[i].author + NL;
                bookDetails += "ISBN10: " + booksArray[i].isbn10 + NL;
                bookDetails += "ISBN13: " + booksArray[i].isbn13 + NL;
                if(booksArray[i].similarity != null)
                    bookDetails += "similarity: " + booksArray[i].similarity.toString() + NL;

                bookList += bookDetails + NL;
            }
        } else {
            bookList += "No books found!";
        }

        bookListView.setText(bookList);
    }
}

