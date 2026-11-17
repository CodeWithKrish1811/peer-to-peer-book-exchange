package com.bookexchange.model;

/** A book listed by a student (table: books). */
public class Book extends BaseEntity {
    private int id;
    private int ownerId;
    private String title;
    private String author;
    private String subject;
    private String isbn;
    private String edition;
    private BookCondition condition = BookCondition.GOOD;
    private String description;
    private String exchangePreference;
    private Availability availability = Availability.AVAILABLE;

    // Filled only by JOIN queries
    private String ownerName;
    private String ownerEmail;
    private String ownerCollegeId;

    public Book() { }

    public Book(int ownerId, String title, String author, String subject, String isbn, String edition,
                BookCondition condition, String description, String exchangePreference) {
        this.ownerId = ownerId;
        this.title = title;
        this.author = author;
        this.subject = subject;
        this.isbn = isbn;
        this.edition = edition;
        this.condition = condition;
        this.description = description;
        this.exchangePreference = exchangePreference;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getEdition() { return edition; }
    public void setEdition(String edition) { this.edition = edition; }
    public BookCondition getCondition() { return condition; }
    public void setCondition(BookCondition condition) { this.condition = condition; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExchangePreference() { return exchangePreference; }
    public void setExchangePreference(String exchangePreference) { this.exchangePreference = exchangePreference; }
    public Availability getAvailability() { return availability; }
    public void setAvailability(Availability availability) { this.availability = availability; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
    public String getOwnerCollegeId() { return ownerCollegeId; }
    public void setOwnerCollegeId(String ownerCollegeId) { this.ownerCollegeId = ownerCollegeId; }

    public boolean isOwnedBy(int studentId) { return ownerId == studentId; }

    @Override
    public String getSummary() { return "Book#" + id + " '" + title + "' by " + author; }
}
