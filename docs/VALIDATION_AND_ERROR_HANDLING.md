# ReWear Project - Input Validation & Error Handling Guide

## Overview

The ReWear application now includes comprehensive input validation and exception handling to ensure data safety and improve user experience. This document details all validation rules and error handling mechanisms.

---

## 1. Validation Features

### 1.1 Email Validation

**Location:** `InputValidator.validateEmail()`

**Validation Rules:**
- ✅ Cannot be empty or blank
- ✅ Maximum 255 characters
- ✅ Must match RFC 5322 simplified format: `user@example.com`
- ✅ Pattern: `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`

**Error Messages:**
- "Email cannot be empty."
- "Email is too long (max 255 characters)."
- "Invalid email format. Expected format: user@example.com"

**Usage Location:** RegisterFrame during registration

**Example Valid Emails:**
```
john.doe@example.com
alice+test@gmail.co.uk
user_name@company.org
```

**Example Invalid Emails:**
```
john@          (incomplete)
john.com       (missing @)
@example.com   (missing username)
john@.com      (missing domain)
```

---

### 1.2 Password Validation

**Location:** `InputValidator.validatePassword()`

**Password Strength Requirements:**

| Requirement | Rule | Regex |
|-------------|------|-------|
| Length | Minimum 8 characters | `.{8,}` |
| Uppercase | At least 1 letter (A-Z) | `.*[A-Z].*` |
| Lowercase | At least 1 letter (a-z) | `.*[a-z].*` |
| Digit | At least 1 number (0-9) | `.*[0-9].*` |
| Special Char | At least 1 special char | `.*[!@#$%^&*...].*` |

**Allowed Special Characters:**
```
! @ # $ % ^ & * ( ) _ + - = [ ] { } ; : ' " , . < > ? / \ | ` ~
```

**Error Messages:**
- "Password cannot be empty."
- "Password must be at least 8 characters long."
- "Password must contain at least 1 uppercase letter (A-Z)."
- "Password must contain at least 1 lowercase letter (a-z)."
- "Password must contain at least 1 digit (0-9)."
- "Password must contain at least 1 special character (!@#$%^&*)."

**Usage Location:** RegisterFrame during registration

**Example Valid Passwords:**
```
MyPassword123!      (8+ chars, upper, lower, digit, special)
Secure@Pass01       (meets all requirements)
Test#1234Demo       (meets all requirements)
```

**Example Invalid Passwords:**
```
password123        (no uppercase)
PASSWORD123!       (no lowercase)
Mypassword!        (no digit)
MyPass123          (no special character)
Short1!            (less than 8 characters)
```

---

### 1.3 Username Validation

**Location:** `InputValidator.validateUsername()`

**Validation Rules:**
- ✅ Cannot be empty or blank
- ✅ Minimum 3 characters
- ✅ Maximum 64 characters
- ✅ Only alphanumeric, underscores, dots, and hyphens allowed
- ✅ Pattern: `^[A-Za-z0-9_.-]+$`

**Error Messages:**
- "Username cannot be empty."
- "Username must be at least 3 characters long."
- "Username must not exceed 64 characters."
- "Username can only contain letters, numbers, underscores, dots, and hyphens."

**Usage Locations:**
- RegisterFrame during registration
- LoginFrame during login

**Example Valid Usernames:**
```
john_doe
alice.smith
user123
test-user
demo_2026
```

**Example Invalid Usernames:**
```
ab              (too short)
john@doe        (contains @)
user name       (contains space)
alice#smith     (contains #)
```

---

### 1.4 Password Confirmation

**Location:** `InputValidator.validatePasswordMatch()`

**Validation Rule:**
- ✅ Password and confirmation must be identical

**Error Message:**
- "Passwords do not match."

**Usage Location:** RegisterFrame during registration

---

## 2. Exception Handling

### 2.1 Exception Hierarchy

```
Exception
├── ValidationException
│   └── Thrown by InputValidator for invalid input
├── DatabaseConnectionException
│   └── Thrown when database connection fails
├── UserAuthenticationException
│   └── Thrown when login credentials are invalid
├── UserRegistrationException
│   └── Thrown when registration fails (duplicate username, etc.)
├── ItemNotFoundException
│   └── Thrown when item is not found
└── ExchangeException
    └── Thrown when exchange operation fails
```

### 2.2 Exception Handling in UI

**LoginFrame Exception Handling:**
```java
try {
    InputValidator.validateUsername(username);
    userDAO.login(username, password);
} catch (ValidationException vex) {
    // Show as WARNING (yellow icon)
    JOptionPane.showMessageDialog(this, vex.getMessage(), 
        "Validation Error", JOptionPane.WARNING_MESSAGE);
} catch (SQLException ex) {
    // Show as ERROR (red icon)
    JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), 
        "Error", JOptionPane.ERROR_MESSAGE);
}
```

**RegisterFrame Exception Handling:**
```java
try {
    InputValidator.validateUsername(username);
    InputValidator.validateEmail(email);
    InputValidator.validatePassword(password);
    InputValidator.validatePasswordMatch(password, confirmPassword);
    userDAO.register(username, email, password);
    // Clear password fields on success
} catch (ValidationException vex) {
    // Show warning and clear password fields
    JOptionPane.showMessageDialog(this, vex.getMessage(), 
        "Validation Error", JOptionPane.WARNING_MESSAGE);
    passwordField.setText("");
    confirmPasswordField.setText("");
}
```

### 2.3 Exception Handling in DAO

**UserDAO Transaction Handling:**
```java
try (Connection conn = DBConnection.getConnection()) {
    conn.setAutoCommit(false);  // Start transaction
    try {
        // Perform operations
        conn.commit();  // Commit if successful
    } catch (SQLException e) {
        conn.rollback();  // Rollback on error
        throw new UserRegistrationException("Registration failed", e);
    }
}
```

---

## 3. User Experience Flow

### 3.1 Registration Flow

```
User enters credentials
         ↓
InputValidator.validateUsername()
         ↓
InputValidator.validateEmail()
         ↓
InputValidator.validatePassword()
         ↓
InputValidator.validatePasswordMatch()
         ↓
userDAO.usernameExists() → Check database
         ↓
userDAO.register() → Insert if all valid
         ↓
Success message + Auto-login
```

### 3.2 Login Flow

```
User enters credentials
         ↓
InputValidator.validateUsername()
         ↓
userDAO.login() → Query database
         ↓
Success: Move to Dashboard
         ↓
Failure: Show error message
```

---

## 4. Error Message Guidelines

### Error Categories

**Validation Errors (WARNING)** 🟡
- Empty fields
- Invalid email format
- Weak password
- Username too short/long
- Special character issues

**Database Errors (ERROR)** 🔴
- Connection failures
- Duplicate username
- Database constraint violations
- Transaction failures

**Success Messages (INFO)** 🟢
- Account created successfully
- Login successful
- Operation completed

---

## 5. Security Considerations

### Password Security
- ✅ Strong password requirements enforce 4 character types
- ✅ Minimum 8 characters prevents short/weak passwords
- ⚠️ **TODO:** Hash passwords using bcrypt instead of plaintext storage

### Input Validation
- ✅ Server-side validation prevents SQL injection
- ✅ Regular expressions sanitize input
- ✅ Parameterized queries in DAO prevent SQL injection

### Email Validation
- ✅ RFC 5322 format prevents invalid addresses
- ✅ Avoids email enumeration attacks
- ⚠️ **TODO:** Send confirmation email to verify ownership

---

## 6. Testing Scenarios

### Test Case 1: Valid Registration

```
Username: john_doe
Email: john@example.com
Password: SecurePass123!
Confirm: SecurePass123!

Expected: ✅ Account created successfully
```

### Test Case 2: Weak Password

```
Username: alice
Email: alice@test.com
Password: weak123
Confirm: weak123

Expected: ⛔ "Password must contain at least 1 special character"
```

### Test Case 3: Invalid Email

```
Username: bob_smith
Email: bob@invalid
Password: ValidPass123!
Confirm: ValidPass123!

Expected: ⛔ "Invalid email format. Expected format: user@example.com"
```

### Test Case 4: Password Mismatch

```
Username: carol
Email: carol@test.com
Password: ValidPass123!
Confirm: Different456!

Expected: ⛔ "Passwords do not match."
```

### Test Case 5: Duplicate Username

```
Username: john_doe (already registered)
Email: john2@example.com
Password: AnotherPass123!
Confirm: AnotherPass123!

Expected: ⛔ "Username already taken."
```

---

## 7. Files Created/Modified

### New Files
- `src/main/java/com/rewear/exceptions/ValidationException.java` – Validation error
- `src/main/java/com/rewear/exceptions/DatabaseConnectionException.java` – Connection error
- `src/main/java/com/rewear/exceptions/UserAuthenticationException.java` – Auth error
- `src/main/java/com/rewear/exceptions/UserRegistrationException.java` – Registration error
- `src/main/java/com/rewear/exceptions/ItemNotFoundException.java` – Item not found error
- `src/main/java/com/rewear/exceptions/ExchangeException.java` – Exchange error
- `src/main/java/com/rewear/validators/InputValidator.java` – Validation utility

### Modified Files
- `src/main/java/com/rewear/ui/LoginFrame.java` – Added username validation
- `src/main/java/com/rewear/ui/RegisterFrame.java` – Added comprehensive validation

---

## 8. Future Enhancements

- [ ] Add email confirmation verification
- [ ] Implement password hashing (bcrypt/Argon2)
- [ ] Add user-friendly password strength indicator UI
- [ ] Rate limiting for login attempts
- [ ] Add Java logging framework (java.util.logging or Log4j)
- [ ] Create audit log for security events
- [ ] Implement CAPTCHA for bot prevention
- [ ] Add two-factor authentication (2FA)

