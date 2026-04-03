package com.rewear.exceptions;

/**
 * Thrown when user authentication fails.
 */
public class UserAuthenticationException extends Exception {
    public UserAuthenticationException(String message) {
        super(message);
    }

    public UserAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
