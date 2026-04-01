package com.rewear.models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Maps to the {@code USERS} table.
 */
public class User {

    private int userId;
    private String username;
    private String email;
    private String passwordHash;
    private int pointsBalance;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(int userId, String username, String email, int pointsBalance) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.pointsBalance = pointsBalance;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getPointsBalance() {
        return pointsBalance;
    }

    public void setPointsBalance(int pointsBalance) {
        this.pointsBalance = pointsBalance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User user)) {
            return false;
        }
        return userId == user.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return username + " (" + pointsBalance + " pts)";
    }
}
