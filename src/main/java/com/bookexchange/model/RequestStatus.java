package com.bookexchange.model;

import com.bookexchange.exception.ValidationException;

/** Lifecycle of an exchange request. */
public enum RequestStatus {
    PENDING("Pending"), ACCEPTED("Accepted"), REJECTED("Rejected"),
    COMPLETED("Completed"), CANCELLED("Cancelled");

    private final String label;

    RequestStatus(String label) { this.label = label; }

    public String getLabel() { return label; }

    /** An active request still reserves the book (PENDING or ACCEPTED). */
    public boolean isActive() { return this == PENDING || this == ACCEPTED; }

    public static RequestStatus fromString(String value) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Request status is required.");
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid request status.");
        }
    }
}
