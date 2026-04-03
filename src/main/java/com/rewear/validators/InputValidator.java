package com.rewear.validators;

import com.rewear.exceptions.ValidationException;

import java.util.regex.Pattern;

/**
 * Input validation utility for email and password fields.
 */
public final class InputValidator {

    private InputValidator() {
    }

    /**
     * Email regex pattern: valid RFC 5322 simplified.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /**
     * Validates email format.
     *
     * @param email the email to validate
     * @throws ValidationException if email is invalid
     */
    public static void validateEmail(String email) throws ValidationException {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email cannot be empty.");
        }

        if (email.length() > 255) {
            throw new ValidationException("Email is too long (max 255 characters).");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException(
                    "Invalid email format. Expected format: user@example.com"
            );
        }
    }

    /**
     * Validates password strength.
     * Requirements:
     * - At least 8 characters
     * - At least 1 uppercase letter (A-Z)
     * - At least 1 lowercase letter (a-z)
     * - At least 1 digit (0-9)
     * - At least 1 special character (!@#$%^&*)
     *
     * @param password the password to validate
     * @throws ValidationException if password is weak
     */
    public static void validatePassword(String password) throws ValidationException {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password cannot be empty.");
        }

        if (password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long.");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new ValidationException("Password must contain at least 1 uppercase letter (A-Z).");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new ValidationException("Password must contain at least 1 lowercase letter (a-z).");
        }

        if (!password.matches(".*[0-9].*")) {
            throw new ValidationException("Password must contain at least 1 digit (0-9).");
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};:'\",.<>?/\\\\|`~].*")) {
            throw new ValidationException(
                    "Password must contain at least 1 special character (!@#$%^&*)."
            );
        }
    }

    /**
     * Validates username format.
     *
     * @param username the username to validate
     * @throws ValidationException if username is invalid
     */
    public static void validateUsername(String username) throws ValidationException {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username cannot be empty.");
        }

        if (username.length() < 3) {
            throw new ValidationException("Username must be at least 3 characters long.");
        }

        if (username.length() > 64) {
            throw new ValidationException("Username must not exceed 64 characters.");
        }

        if (!username.matches("^[A-Za-z0-9_.-]+$")) {
            throw new ValidationException(
                    "Username can only contain letters, numbers, underscores, dots, and hyphens."
            );
        }
    }

    /**
     * Validates password confirmation match.
     *
     * @param password the password
     * @param confirmation the confirmation password
     * @throws ValidationException if passwords don't match
     */
    public static void validatePasswordMatch(String password, String confirmation) 
            throws ValidationException {
        if (!password.equals(confirmation)) {
            throw new ValidationException("Passwords do not match.");
        }
    }
}
