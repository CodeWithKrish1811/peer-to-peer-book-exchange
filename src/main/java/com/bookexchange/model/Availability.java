package com.bookexchange.model;

import com.bookexchange.exception.ValidationException;

/** Availability of a listed book. */
public enum Availability {
    AVAILABLE("Available"), EXCHANGE_PENDING("Exchange Pending"), EXCHANGED("Exchanged");

    private final String label;

    Availability(String label) { this.label = label; }

    public String getLabel() { return label; }

    public static Availability fromString(String value) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Availability is required.");
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid availability value.");
        }
    }
}
