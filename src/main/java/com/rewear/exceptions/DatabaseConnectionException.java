package com.rewear.exceptions;

/**
 * Thrown when database connection cannot be established.
 */
public class DatabaseConnectionException extends Exception {
    public DatabaseConnectionException(String message) {
        super(message);
    }

    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
