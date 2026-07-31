package com.expensesplitter.exception;

/**
 * Thrown for authorization failures that are NOT "you're not logged in" (that's 401,
 * handled by Spring Security itself) but "you're logged in, just not allowed to do this"
 * — e.g. trying to add an expense to a group you're not a member of. Maps to HTTP 403.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
