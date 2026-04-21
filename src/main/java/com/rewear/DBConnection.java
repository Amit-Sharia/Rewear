package com.rewear;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Central JDBC access for the ReWear desktop client.
 * Connection parameters can be overridden with JVM system properties:
 * <ul>
 *   <li>{@code rewear.jdbc.url}</li>
 *   <li>{@code rewear.jdbc.user}</li>
 *   <li>{@code rewear.jdbc.password}</li>
 * </ul>
 */
public final class DBConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/rewear?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "AmitAmit";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = firstNonBlank(
                System.getProperty("rewear.jdbc.url"),
                System.getenv("REWEAR_JDBC_URL"),
                DEFAULT_URL
        );
        String user = firstNonBlank(
                System.getProperty("rewear.jdbc.user"),
                System.getenv("REWEAR_JDBC_USER"),
                DEFAULT_USER
        );

        String explicitPassword = firstNonBlank(
                System.getProperty("rewear.jdbc.password"),
                System.getenv("REWEAR_JDBC_PASSWORD")
        );
        Set<String> passwordCandidates = new LinkedHashSet<>();
        if (explicitPassword != null) {
            passwordCandidates.add(explicitPassword);
        } else {
            // Common local defaults. Keeps old behavior and also supports blank root passwords.
            passwordCandidates.add(DEFAULT_PASSWORD);
            passwordCandidates.add("");
        }

        SQLException lastError = null;
        for (String password : passwordCandidates) {
            try {
                return DriverManager.getConnection(url, user, password);
            } catch (SQLException ex) {
                lastError = ex;
            }
        }

        String message = "Unable to connect to MySQL. "
                + "url=" + url + ", user=" + user + ", sqlState="
                + (lastError != null ? lastError.getSQLState() : "n/a")
                + ", errorCode=" + (lastError != null ? lastError.getErrorCode() : -1)
                + ". Ensure MySQL is running, database 'rewear' exists, and credentials are correct. "
                + "You can set rewear.jdbc.url/user/password (JVM props) or REWEAR_JDBC_URL/USER/PASSWORD (env vars).";
        throw new SQLException(message, lastError);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
