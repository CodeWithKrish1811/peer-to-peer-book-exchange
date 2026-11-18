package com.bookexchange.exception;

/** Thrown when an action breaks a business rule (duplicate request, unavailable book, ...). */
public class BusinessRuleException extends AppException {
    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) { super(message); }
}
