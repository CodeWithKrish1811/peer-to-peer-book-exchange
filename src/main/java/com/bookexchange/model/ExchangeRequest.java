package com.bookexchange.model;

import java.sql.Timestamp;

/** An exchange request between two students (table: exchange_requests). */
public class ExchangeRequest extends BaseEntity {
    private int id;
    private int requesterId;
    private int ownerId;
    private int requestedBookId;
    private Integer offeredBookId;          // optional
    private String message;
    private RequestStatus status = RequestStatus.PENDING;
    private Timestamp updatedAt;

    // Filled only by JOIN queries
    private String requestedBookTitle;
    private String offeredBookTitle;
    private String requesterName;
    private String requesterEmail;
    private String requesterCollegeId;
    private String ownerName;
    private String ownerEmail;

    public ExchangeRequest() { }

    public ExchangeRequest(int requesterId, int ownerId, int requestedBookId, Integer offeredBookId, String message) {
        this.requesterId = requesterId;
        this.ownerId = ownerId;
        this.requestedBookId = requestedBookId;
        this.offeredBookId = offeredBookId;
        this.message = message;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getRequesterId() { return requesterId; }
    public void setRequesterId(int requesterId) { this.requesterId = requesterId; }
    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public int getRequestedBookId() { return requestedBookId; }
    public void setRequestedBookId(int requestedBookId) { this.requestedBookId = requestedBookId; }
    public Integer getOfferedBookId() { return offeredBookId; }
    public void setOfferedBookId(Integer offeredBookId) { this.offeredBookId = offeredBookId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public String getRequestedBookTitle() { return requestedBookTitle; }
    public void setRequestedBookTitle(String t) { this.requestedBookTitle = t; }
    public String getOfferedBookTitle() { return offeredBookTitle; }
    public void setOfferedBookTitle(String t) { this.offeredBookTitle = t; }
    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String n) { this.requesterName = n; }
    public String getRequesterEmail() { return requesterEmail; }
    public void setRequesterEmail(String e) { this.requesterEmail = e; }
    public String getRequesterCollegeId() { return requesterCollegeId; }
    public void setRequesterCollegeId(String c) { this.requesterCollegeId = c; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String n) { this.ownerName = n; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String e) { this.ownerEmail = e; }

    /** The request date is the creation timestamp inherited from BaseEntity. */
    public Timestamp getRequestDate() { return getCreatedAt(); }

    @Override
    public String getSummary() { return "Request#" + id + " [" + status + "] for book#" + requestedBookId; }
}
