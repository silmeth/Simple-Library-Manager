package silmeth.slm.client;

/**
 * Created by silmeth on 16.12.14.
 */
public class BookInfo {
    public String title;
    public String author;
    public String isbn10;
    public String isbn13;
    public String publisher;
    public int id;
    public Float similarity;
    public String pubYear;

    BookInfo(String titleStr, String authorStr, String isbn10Str, String isbn13Str) {
        title = titleStr;
        isbn10 = isbn10Str;
        isbn13 = isbn13Str;
        author = authorStr;
        similarity = null;
    }

    BookInfo() {
        title = null;
        isbn10 = null;
        isbn13 = null;
        author = null;
        publisher = null;
        similarity = null;
        pubYear = null;
    }
}
