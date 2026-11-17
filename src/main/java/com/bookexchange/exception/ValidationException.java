package com.bookexchange.exception;

/** Thrown when user input is missing or malformed. */
public class ValidationException extends AppException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) { super(message); }
}
