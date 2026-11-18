package com.bookexchange.model;

import com.bookexchange.exception.ValidationException;

/** Physical condition of a book. The enum name is what is stored in MySQL. */
public enum BookCondition {
    NEW("New"), GOOD("Good"), AVERAGE("Average");

    private final String label;

    BookCondition(String label) { this.label = label; }

    public String getLabel() { return label; }

    public static BookCondition fromString(String value) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Book condition is required.");
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid book condition.");
        }
    }
}
