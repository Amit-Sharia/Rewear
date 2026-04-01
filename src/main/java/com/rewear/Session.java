package com.rewear;

import com.rewear.models.User;

/**
 * Holds the logged-in user for the current desktop session (single-user client).
 */
public final class Session {

    private static User currentUser;

    private Session() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
