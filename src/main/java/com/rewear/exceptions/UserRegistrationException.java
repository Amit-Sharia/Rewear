package com.rewear.exceptions;

/**
 * Thrown when user registration fails due to duplicate username or invalid data.
 */
public class UserRegistrationException extends Exception {
    public UserRegistrationException(String message) {
        super(message);
    }

    public UserRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
