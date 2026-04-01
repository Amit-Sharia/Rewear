package com.rewear.dao;

import com.rewear.DBConnection;
import com.rewear.models.ChatMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ChatDAO {
    private static final String SELECT_MESSAGES =
            "SELECT m.message_id, m.exchange_id, m.sender_user_id, u.username, m.message_text, m.sent_at "
                    + "FROM EXCHANGE_MESSAGES m "
                    + "JOIN USERS u ON u.user_id = m.sender_user_id "
                    + "JOIN EXCHANGES e ON e.exchange_id = m.exchange_id "
                    + "WHERE m.exchange_id = ? "
                    + "AND (e.requester_user_id = ? OR e.owner_user_id = ?) "
                    + "AND e.status = 'COMPLETED' "
                    + "ORDER BY m.sent_at ASC";

    private static final String INSERT_MESSAGE =
            "INSERT INTO EXCHANGE_MESSAGES (exchange_id, sender_user_id, message_text) "
                    + "SELECT ?, ?, ? FROM EXCHANGES e "
                    + "WHERE e.exchange_id = ? "
                    + "AND (e.requester_user_id = ? OR e.owner_user_id = ?) "
                    + "AND e.status = 'COMPLETED'";

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
