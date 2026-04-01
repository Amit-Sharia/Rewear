package com.rewear.dao;

import com.rewear.DBConnection;
import com.rewear.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Data access for {@code USERS} and {@code POINTS_LOG}.
 */
public class UserDAO {

    private static final String INSERT_USER =
            "INSERT INTO USERS (username, email, password_hash, points_balance) VALUES (?, ?, ?, ?)";

    private static final String SELECT_BY_CREDENTIALS =
            "SELECT user_id, username, email, password_hash, points_balance, created_at "
                    + "FROM USERS WHERE username = ? AND password_hash = ?";

    private static final String SELECT_BY_ID =
            "SELECT user_id, username, email, password_hash, points_balance, created_at "
                    + "FROM USERS WHERE user_id = ?";

    private static final String SELECT_BY_USERNAME =
            "SELECT user_id FROM USERS WHERE username = ?";

    private static final String SELECT_POINTS_FOR_UPDATE =
            "SELECT points_balance FROM USERS WHERE user_id = ? FOR UPDATE";

    private static final String UPDATE_POINTS =
            "UPDATE USERS SET points_balance = points_balance + ? WHERE user_id = ?";

    private static final String INSERT_POINTS_LOG =
            "INSERT INTO POINTS_LOG (user_id, delta, reason, related_exchange_id) VALUES (?, ?, ?, ?)";

    /**
     * Registers a new user with an initial points balance (default 100).
     */
    public int register(String username, String email, String passwordPlain) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, passwordPlain);
            ps.setInt(4, 100);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Could not obtain new user id.");
    }

    /**
     * Validates credentials; demo stores password in {@code password_hash} column as plain text.
     */
    public Optional<User> login(String username, String passwordPlain) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_CREDENTIALS)) {
            ps.setString(1, username);
            ps.setString(2, passwordPlain);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findById(int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Reads balance within an open transaction (locks the user row).
     */
    public int getPointsBalanceForUpdate(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_POINTS_FOR_UPDATE)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("User not found: " + userId);
    }

    public boolean usernameExists(String username) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_USERNAME)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Adjusts points inside an existing transaction (connection must not be auto-commit).
     */
    public void adjustPoints(Connection conn, int userId, int delta) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_POINTS)) {
            ps.setInt(1, delta);
            ps.setInt(2, userId);
            int n = ps.executeUpdate();
            if (n != 1) {
                throw new SQLException("Failed to update points for user " + userId);
            }
        }
    }

    public void insertPointsLog(Connection conn, int userId, int delta, String reason, Integer relatedExchangeId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_POINTS_LOG)) {
            ps.setInt(1, userId);
            ps.setInt(2, delta);
            ps.setString(3, reason);
            if (relatedExchangeId != null) {
                ps.setInt(4, relatedExchangeId);
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
        }
    }

    /**
     * Demo helper: add points directly and write an audit entry.
     */
    public void addPoints(int userId, int delta, String reason) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                adjustPoints(conn, userId, delta);
                insertPointsLog(conn, userId, delta, reason, null);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setPointsBalance(rs.getInt("points_balance"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            u.setCreatedAt(ts.toLocalDateTime());
        }
        return u;
    }
}
