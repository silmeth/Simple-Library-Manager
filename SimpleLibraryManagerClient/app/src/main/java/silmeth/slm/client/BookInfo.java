package silmeth.slm.client;

/**
 * Created by silmeth on 16.12.14.
 */
public class BookInfo {
    public String title;
    public String author;
    public String isbn10;
    public String isbn13;
    public Float similarity;

    BookInfo(String titleStr, String authorStr, String isbn10Str, String isbn13Str) {
        title = new String(titleStr);
        isbn10 = new String(isbn10Str);
        isbn13 = new String(isbn13Str);
        author = new String(authorStr);
        similarity = null;
    }

    BookInfo() {
        title = new String();
        isbn10 = new String();
        isbn13 = new String();
        author = new String();
        similarity = null;
    }
}
