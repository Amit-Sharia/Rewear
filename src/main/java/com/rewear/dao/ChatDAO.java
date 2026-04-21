package com.rewear.dao;

import com.rewear.DBConnection;
import com.rewear.models.ChatMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChatDAO implements IDataAccessObject<ChatMessage, Integer> {
    private static final String SELECT_MESSAGES =
            "SELECT m.message_id, m.exchange_id, m.sender_user_id, u.username, m.message_text, m.sent_at "
                    + "FROM EXCHANGE_MESSAGES m "
                    + "JOIN USERS u ON u.user_id = m.sender_user_id "
                    + "JOIN EXCHANGES e ON e.exchange_id = m.exchange_id "
                    + "WHERE m.exchange_id = ? "
                    + "AND (e.requester_user_id = ? OR e.owner_user_id = ?) "
                    + "AND e.status IN ('ACCEPTED', 'COMPLETED') "
                    + "ORDER BY m.sent_at ASC";

    private static final String INSERT_MESSAGE =
            "INSERT INTO EXCHANGE_MESSAGES (exchange_id, sender_user_id, message_text) "
                    + "SELECT ?, ?, ? FROM EXCHANGES e "
                    + "WHERE e.exchange_id = ? "
                    + "AND (e.requester_user_id = ? OR e.owner_user_id = ?) "
                    + "AND e.status IN ('ACCEPTED', 'COMPLETED')";

    private static final String SELECT_BY_ID =
            "SELECT m.message_id, m.exchange_id, m.sender_user_id, u.username, m.message_text, m.sent_at "
                    + "FROM EXCHANGE_MESSAGES m "
                    + "JOIN USERS u ON u.user_id = m.sender_user_id "
                    + "WHERE m.message_id = ?";

    private static final String SELECT_ALL_MESSAGES =
            "SELECT m.message_id, m.exchange_id, m.sender_user_id, u.username, m.message_text, m.sent_at "
                    + "FROM EXCHANGE_MESSAGES m "
                    + "JOIN USERS u ON u.user_id = m.sender_user_id "
                    + "ORDER BY m.sent_at DESC";

    private static final String UPDATE_MESSAGE =
            "UPDATE EXCHANGE_MESSAGES SET message_text = ? WHERE message_id = ?";

    private static final String DELETE_MESSAGE =
            "DELETE FROM EXCHANGE_MESSAGES WHERE message_id = ?";

    // Interface implementation methods
    @Override
    public Integer insert(ChatMessage entity) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_MESSAGE, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.getExchangeId());
            ps.setInt(2, entity.getSenderUserId());
            ps.setString(3, entity.getMessageText());
            ps.setInt(4, entity.getExchangeId());
            ps.setInt(5, entity.getSenderUserId());
            ps.setInt(6, entity.getSenderUserId());

            int rows = ps.executeUpdate();
            if (rows == 1) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            throw new SQLException("Failed to insert chat message");
        }
    }

    @Override
    public Optional<ChatMessage> findById(Integer id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("sent_at");
                    ChatMessage message = new ChatMessage(
                            rs.getInt("message_id"),
                            rs.getInt("exchange_id"),
                            rs.getInt("sender_user_id"),
                            rs.getString("username"),
                            rs.getString("message_text"),
                            ts != null ? ts.toLocalDateTime() : null
                    );
                    return Optional.of(message);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<ChatMessage> findAll() throws SQLException {
        List<ChatMessage> messages = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_MESSAGES);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("sent_at");
                messages.add(new ChatMessage(
                        rs.getInt("message_id"),
                        rs.getInt("exchange_id"),
                        rs.getInt("sender_user_id"),
                        rs.getString("username"),
                        rs.getString("message_text"),
                        ts != null ? ts.toLocalDateTime() : null
                ));
            }
        }
        return messages;
    }

    @Override
    public void update(ChatMessage entity) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_MESSAGE)) {
            ps.setString(1, entity.getMessageText());
            ps.setInt(2, entity.getMessageId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new SQLException("Failed to update chat message with id: " + entity.getMessageId());
            }
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_MESSAGE)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new SQLException("Failed to delete chat message with id: " + id);
            }
        }
    }

    // Existing methods (kept for backward compatibility and specific use cases)
    // These methods provide additional functionality specific to chat messaging

    public List<ChatMessage> listMessages(int exchangeId, int currentUserId) throws SQLException {
        List<ChatMessage> out = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_MESSAGES)) {
            ps.setInt(1, exchangeId);
            ps.setInt(2, currentUserId);
            ps.setInt(3, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("sent_at");
                    out.add(new ChatMessage(
                            rs.getInt("message_id"),
                            rs.getInt("exchange_id"),
                            rs.getInt("sender_user_id"),
                            rs.getString("username"),
                            rs.getString("message_text"),
                            ts != null ? ts.toLocalDateTime() : null
                    ));
                }
            }
        }
        return out;
    }

    public void sendMessage(int exchangeId, int senderUserId, String messageText) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_MESSAGE)) {
            ps.setInt(1, exchangeId);
            ps.setInt(2, senderUserId);
            ps.setString(3, messageText);
            ps.setInt(4, exchangeId);
            ps.setInt(5, senderUserId);
            ps.setInt(6, senderUserId);
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new SQLException("Unable to send message for this exchange.");
            }
        }
    }
}
