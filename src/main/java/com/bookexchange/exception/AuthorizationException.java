package com.bookexchange.exception;

/** Thrown for failed logins and for attempts to act on another student's data. */
public class AuthorizationException extends AppException {
    private static final long serialVersionUID = 1L;

    public AuthorizationException(String message) { super(message); }
}
