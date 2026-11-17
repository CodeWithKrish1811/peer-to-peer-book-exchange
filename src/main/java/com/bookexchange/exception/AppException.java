package com.bookexchange.exception;

/**
 * Base class of all application-specific (checked) exceptions.
 * Its message is always safe to show to the user.
 */
public class AppException extends Exception {
    private static final long serialVersionUID = 1L;

    public AppException(String message) { super(message); }

    public AppException(String message, Throwable cause) { super(message, cause); }
}
