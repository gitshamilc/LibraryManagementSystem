package models;

public class BookCopy {
    private String id;
    private String bookId;
    private String barcode;
    private String status; // AVAILABLE, BORROWED, LOST, DAMAGED
    private String condition;
    private String shelfLocation;

    public BookCopy(String id, String bookId, String barcode, String status, String condition, String shelfLocation) {
        this.id = id;
        this.bookId = bookId;
        this.barcode = barcode;
        this.status = status;
        this.condition = condition;
        this.shelfLocation = shelfLocation;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getShelfLocation() { return shelfLocation; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }
}
