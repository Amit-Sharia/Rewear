package com.rewear;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = System.getProperty("rewear.jdbc.url", DEFAULT_URL);
        String user = System.getProperty("rewear.jdbc.user", "root");
        String password = System.getProperty("rewear.jdbc.password", "AmitAmit");
        return DriverManager.getConnection(url, user, password);
    }
}
