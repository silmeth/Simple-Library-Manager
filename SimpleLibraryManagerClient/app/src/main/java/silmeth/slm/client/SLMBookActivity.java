package silmeth.slm.client;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SLMBookActivity extends ActionBarActivity
        implements AdapterView.OnItemSelectedListener {
    public final int addBookId = 0;

    private SharedPreferences sharedPref;
    private String SLMHostName;
    private String SLMPort;
    private String sessionCookie;
    private Boolean canManageBooks;

    TextView bookDetailsTextView;

    private Spinner bookChooseSpinner;
    private ArrayAdapter<String> bookChooseAdapter;
    List<String> bookChooseList;
    List<String> titlesList;
    List<String> authorsList;
    List<Integer> authorIdsList;
    List<String> publishersList;
    List<Integer> publisherIdsList;
    List<Integer> bookIdsList;
    List<String> ISBN10List;
    List<String> ISBN13List;
    List<Integer> pubYearList;
    List<Float> similaritiesList;

    String title;
    String author;
    Integer authorId;
    String publisher;
    Integer publisherId;
    Integer bookId;
    String ISBN10;
    String ISBN13;
    Integer pubYear;
    Float similarity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slmbook);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SLMHostName = sharedPref.getString("pref_slm_host", "");
        SLMPort = sharedPref.getString("pref_slm_host_port", "");
        sessionCookie = sharedPref.getString("session_cookie", "sessionid=null");
        canManageBooks = sharedPref.getBoolean("can_manage_books", false);

        Bundle extras = getIntent().getExtras();

        bookDetailsTextView = (TextView) findViewById(R.id.bookDetailsTextView);

        bookChooseList = new ArrayList<>();
        titlesList = new ArrayList<>();
        authorsList = new ArrayList<>();
        authorIdsList = new ArrayList<>();
        publishersList = new ArrayList<>();
        publisherIdsList = new ArrayList<>();
        bookIdsList = new ArrayList<>();
        ISBN10List = new ArrayList<>();
        ISBN13List = new ArrayList<>();
        pubYearList = new ArrayList<>();
        similaritiesList = new ArrayList<>();

        String booksJsonStr = extras.getString("books");
        try {
            JSONArray booksJsonArray = new JSONArray(booksJsonStr);

            for(int i = 0; i < booksJsonArray.length(); ++i) {
                JSONObject bookJson = booksJsonArray.getJSONObject(i);
                titlesList.add(bookJson.getString("title"));
                bookIdsList.add(bookJson.getInt("book_id"));
                authorsList.add(bookJson.getString("author"));
                authorIdsList.add(bookJson.getInt("author_id"));
                publishersList.add(bookJson.getString("publisher"));
                publisherIdsList.add(bookJson.getInt("publisher_id"));
                ISBN10List.add(bookJson.getString("isbn10"));
                ISBN13List.add(bookJson.getString("isbn13"));
                try {
                    pubYearList.add(bookJson.getInt("pub_date"));
                } catch(JSONException e) {
                    pubYearList.add(null);
                    e.printStackTrace();
                }
                try {
                    similaritiesList.add(new Float(bookJson.getDouble("similarity")));
                } catch(JSONException e) {
                    similaritiesList.add(new Float(.0));
                    e.printStackTrace();
                }
                bookChooseList = titlesList;

            }
        } catch(JSONException e) {
            e.printStackTrace();
        }

        bookChooseSpinner = (Spinner) findViewById(R.id.bookChooseSpinner);
        bookChooseAdapter = new ArrayAdapter<>(
                this,
                R.layout.support_simple_spinner_dropdown_item,
                bookChooseList
        );
        bookChooseSpinner.setAdapter(bookChooseAdapter);
        bookChooseSpinner.setOnItemSelectedListener(this);

        if(titlesList.size() > 0) {
            title = titlesList.get(0);
            bookId = bookIdsList.get(0);
            author = authorsList.get(0);
            authorId = authorIdsList.get(0);
            publisher = publishersList.get(0);
            publisherId = publisherIdsList.get(0);
            ISBN10 = ISBN10List.get(0);
            ISBN13 = ISBN13List.get(0);
            pubYear = pubYearList.get(0);
            similarity = similaritiesList.get(0);
        } else {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setCancelable(false);
            alertDialogBuilder.setTitle(getString(R.string.error));
            alertDialogBuilder.setMessage(getString(R.string.no_book_found));
            alertDialogBuilder.setPositiveButton(
                    getString(R.string.OK),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    });
            alertDialogBuilder.create().show();
        }

        refreshBookDetails();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_slmbook, menu);
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
        if(requestCode == addBookId) {

        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner = (Spinner) parent;
        if(spinner.getId() == bookChooseSpinner.getId()) {
            title = titlesList.get(pos);
            bookId = bookIdsList.get(pos);
            author = authorsList.get(pos);
            authorId = authorIdsList.get(pos);
            publisher = publishersList.get(pos);
            publisherId = publisherIdsList.get(pos);
            ISBN10 = ISBN10List.get(pos);
            ISBN13 = ISBN13List.get(pos);
            pubYear = pubYearList.get(pos);
            similarity = similaritiesList.get(pos);

            refreshBookDetails();
        }

    }

    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void btAddNewCopy(View btView) {
        Intent i = new Intent(this, AddBook.class);
//        i.putExtra("title", title);
//        i.putExtra("author", author);
//        i.putExtra("publisher", publisher);
        if(pubYear != null) i.putExtra("pubYear", pubYear.toString());
        if(ISBN10 != null) i.putExtra("ISBN10", ISBN10);
        if(ISBN13 != null) i.putExtra("ISBN13", ISBN13);
        i.putExtra("titleSearch", searchTitles());
        i.putExtra("authorSearch", searchAuthors());
        i.putExtra("publisherSearch", searchPublishers());
        startActivityForResult(i, addBookId);
    }

    private String searchTitles() {
        return search(title, "/title=");
    }

    private String searchAuthors() {
        return search(author, "_authors/");
    }

    private String searchPublishers() {
        return search(publisher, "_publishers/");
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
                    "http://" + SLMHostName + ":" + SLMPort + "/webs/search" + what + query,
                    sessionCookie
            ).get(2, TimeUnit.SECONDS);
            if(result[httpReqTask.respStr] == null)
                throw new TimeoutException("Request returned null");
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

    private void refreshBookDetails() {
        String NL = System.getProperty("line.separator");
        String bookDetails =
                "title: " + title + NL +
                "author: " + author + NL +
                "publisher: " + publisher + NL +
                "id: " + bookId + NL +
                "similarity: " + similarity + NL;

        bookDetailsTextView.setText(bookDetails);
    }
}
