package com.bookexchange.model;

/** One audit-trail entry written on every status change (table: exchange_history). */
public class ExchangeHistory extends BaseEntity {
    private int id;
    private int requestId;
    private int bookId;
    private int studentId;
    private RequestStatus status;
    private String details;

    // Filled only by JOIN queries
    private String bookTitle;
    private String studentName;

    public ExchangeHistory() { }

    public ExchangeHistory(int requestId, int bookId, int studentId, RequestStatus status, String details) {
        this.requestId = requestId;
        this.bookId = bookId;
        this.studentId = studentId;
        this.status = status;
        this.details = details;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }
    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    /** The exchange date is the creation timestamp inherited from BaseEntity. */
    public java.sql.Timestamp getExchangeDate() { return getCreatedAt(); }

    @Override
    public String getSummary() { return "History#" + id + " request#" + requestId + " -> " + status; }
}
