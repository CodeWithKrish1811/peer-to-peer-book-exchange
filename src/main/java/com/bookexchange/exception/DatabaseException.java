package com.bookexchange.exception;

/** Wraps SQLExceptions so that technical details never reach the user interface. */
public class DatabaseException extends AppException {
    private static final long serialVersionUID = 1L;

    public DatabaseException(String message, Throwable cause) { super(message, cause); }
}
