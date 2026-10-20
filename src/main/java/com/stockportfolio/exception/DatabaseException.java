package com.stockportfolio.exception;

/**
 * Wraps database problems (SQLException) in one of our own exceptions.
 * It is unchecked, so the UI can catch it in one place and show a friendly message.
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}