package com.rewear.dao;

import com.rewear.DBConnection;
import com.rewear.models.ExchangeRecord;
import com.rewear.models.Item;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for {@code EXCHANGES}, {@code EXCHANGE_ITEMS}, {@code REVIEWS}, and optional {@code SHIPPING}.
 */
public class ExchangeDAO {

    private static final String INSERT_EXCHANGE =
            "INSERT INTO EXCHANGES (requester_user_id, owner_user_id, status) VALUES (?, ?, 'PENDING')";

    private static final String INSERT_EXCHANGE_ITEM =
            "INSERT INTO EXCHANGE_ITEMS (exchange_id, item_id) VALUES (?, ?)";

    private static final String INSERT_SHIPPING =
            "INSERT INTO SHIPPING (exchange_id, address_line, status) VALUES (?, ?, 'PENDING')";

    private static final String INSERT_REVIEW =
            "INSERT INTO REVIEWS (exchange_id, reviewer_user_id, rating, comment) VALUES (?, ?, ?, ?)";

    private static final String SELECT_PENDING_REVIEWS =
            "SELECT e.exchange_id, e.requester_user_id, e.owner_user_id, e.status, e.created_at, "
                    + "ei.item_id, i.item_name, "
                    + "CASE WHEN e.requester_user_id = ? THEN uo.username ELSE ur.username END AS other_party "
                    + "FROM EXCHANGES e "
                    + "JOIN EXCHANGE_ITEMS ei ON ei.exchange_id = e.exchange_id "
                    + "JOIN ITEMS i ON i.item_id = ei.item_id "
                    + "JOIN USERS ur ON ur.user_id = e.requester_user_id "
                    + "JOIN USERS uo ON uo.user_id = e.owner_user_id "
                    + "WHERE e.status = 'COMPLETED' "
                    + "AND (e.requester_user_id = ? OR e.owner_user_id = ?) "
                    + "AND NOT EXISTS (SELECT 1 FROM REVIEWS r WHERE r.exchange_id = e.exchange_id "
                    + "AND r.reviewer_user_id = ?) "
                    + "ORDER BY e.created_at DESC";

    private static final String SELECT_OWNER_PENDING =
            "SELECT e.exchange_id, e.requester_user_id, e.owner_user_id, e.status, e.created_at, "
                    + "ei.item_id, i.item_name, ur.username AS other_party "
                    + "FROM EXCHANGES e "
                    + "JOIN EXCHANGE_ITEMS ei ON ei.exchange_id = e.exchange_id "
                    + "JOIN ITEMS i ON i.item_id = ei.item_id "
                    + "JOIN USERS ur ON ur.user_id = e.requester_user_id "
                    + "WHERE e.owner_user_id = ? AND e.status IN ('PENDING', 'ACCEPTED') "
                    + "ORDER BY e.created_at DESC";

    private static final String SELECT_CHAT_ELIGIBLE =
            "SELECT e.exchange_id, e.requester_user_id, e.owner_user_id, e.status, e.created_at, "
                    + "ei.item_id, i.item_name, "
                    + "CASE WHEN e.requester_user_id = ? THEN uo.username ELSE ur.username END AS other_party "
                    + "FROM EXCHANGES e "
                    + "JOIN EXCHANGE_ITEMS ei ON ei.exchange_id = e.exchange_id "
                    + "JOIN ITEMS i ON i.item_id = ei.item_id "
                    + "JOIN USERS ur ON ur.user_id = e.requester_user_id "
                    + "JOIN USERS uo ON uo.user_id = e.owner_user_id "
                    + "WHERE (e.requester_user_id = ? OR e.owner_user_id = ?) "
                    + "AND e.status IN ('ACCEPTED', 'COMPLETED') "
                    + "ORDER BY e.created_at DESC";

    private static final String SELECT_EXCHANGE_FOR_OWNER_UPDATE =
            "SELECT e.exchange_id, e.requester_user_id, e.owner_user_id, e.status, ei.item_id "
                    + "FROM EXCHANGES e "
                    + "JOIN EXCHANGE_ITEMS ei ON ei.exchange_id = e.exchange_id "
                    + "WHERE e.exchange_id = ? FOR UPDATE";

    private static final String UPDATE_EXCHANGE_STATUS =
            "UPDATE EXCHANGES SET status = ? WHERE exchange_id = ?";

    private static final String CALL_COMPLETE_EXCHANGE =
            "{CALL sp_complete_exchange(?)}";

    private final ItemDAO itemDAO = new ItemDAO();

    /**
     * Creates a pending request. Owner must accept later.
     */
    public int requestExchange(int requesterUserId, int itemId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Item item = itemDAO.loadByIdForUpdate(conn, itemId);
                if (item == null) {
                    throw new SQLException("Item not found.");
                }
                if (!"AVAILABLE".equalsIgnoreCase(item.getStatus())) {
                    throw new SQLException("Item is not available for exchange.");
                }
                if (item.getOwnerUserId() == requesterUserId) {
                    throw new SQLException("You cannot request your own item.");
                }
                int exchangeId;
                try (PreparedStatement ps = conn.prepareStatement(INSERT_EXCHANGE, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, requesterUserId);
                    ps.setInt(2, item.getOwnerUserId());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Could not create exchange.");
                        }
                        exchangeId = keys.getInt(1);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(INSERT_EXCHANGE_ITEM)) {
                    ps.setInt(1, exchangeId);
                    ps.setInt(2, itemId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(INSERT_SHIPPING)) {
                    ps.setInt(1, exchangeId);
                    ps.setString(2, "Local pickup / TBD");
                    ps.executeUpdate();
                }

                conn.commit();
                return exchangeId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<ExchangeRecord> listOwnerPendingRequests(int ownerUserId) throws SQLException {
        List<ExchangeRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_OWNER_PENDING)) {
            ps.setInt(1, ownerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapExchangeRecord(rs));
                }
            }
        }
        return list;
    }

    public List<ExchangeRecord> listChatEligibleExchanges(int currentUserId) throws SQLException {
        List<ExchangeRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_CHAT_ELIGIBLE)) {
            ps.setInt(1, currentUserId);
            ps.setInt(2, currentUserId);
            ps.setInt(3, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapExchangeRecord(rs));
                }
            }
        }
        return list;
    }

    public void acceptRequest(int exchangeId, int ownerUserId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ExchangeLockRow ex = loadExchangeForOwnerUpdate(conn, exchangeId);
                if (ex == null) {
                    throw new SQLException("Exchange not found.");
                }
                if (ex.ownerUserId != ownerUserId) {
                    throw new SQLException("You are not allowed to accept this request.");
                }
                if (!"PENDING".equalsIgnoreCase(ex.status)) {
                    throw new SQLException("This request is no longer pending.");
                }

                Item item = itemDAO.loadByIdForUpdate(conn, ex.itemId);
                if (item == null || !"AVAILABLE".equalsIgnoreCase(item.getStatus())) {
                    throw new SQLException("Item is no longer available.");
                }
                updateExchangeStatus(conn, exchangeId, "ACCEPTED");
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void completeAcceptedRequest(int exchangeId, int ownerUserId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ExchangeLockRow ex = loadExchangeForOwnerUpdate(conn, exchangeId);
                if (ex == null) {
                    throw new SQLException("Exchange not found.");
                }
                if (ex.ownerUserId != ownerUserId) {
                    throw new SQLException("You are not allowed to complete this exchange.");
                }
                if (!"ACCEPTED".equalsIgnoreCase(ex.status)) {
                    throw new SQLException("Only ACCEPTED requests can be completed.");
                }
                try (CallableStatement cs = conn.prepareCall(CALL_COMPLETE_EXCHANGE)) {
                    cs.setInt(1, exchangeId);
                    cs.execute();
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void rejectRequest(int exchangeId, int ownerUserId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ExchangeLockRow ex = loadExchangeForOwnerUpdate(conn, exchangeId);
                if (ex == null) {
                    throw new SQLException("Exchange not found.");
                }
                if (ex.ownerUserId != ownerUserId) {
                    throw new SQLException("You are not allowed to reject this request.");
                }
                if (!"PENDING".equalsIgnoreCase(ex.status)) {
                    throw new SQLException("This request is no longer pending.");
                }
                updateExchangeStatus(conn, exchangeId, "REJECTED");
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<ExchangeRecord> listExchangesAwaitingReview(int currentUserId) throws SQLException {
        List<ExchangeRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_PENDING_REVIEWS)) {
            ps.setInt(1, currentUserId);
            ps.setInt(2, currentUserId);
            ps.setInt(3, currentUserId);
            ps.setInt(4, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime created = ts != null ? ts.toLocalDateTime() : null;
                    list.add(new ExchangeRecord(
                            rs.getInt("exchange_id"),
                            rs.getInt("requester_user_id"),
                            rs.getInt("owner_user_id"),
                            rs.getString("status"),
                            created,
                            rs.getInt("item_id"),
                            rs.getString("item_name"),
                            rs.getString("other_party")
                    ));
                }
            }
        }
        return list;
    }

    public void submitReview(int exchangeId, int reviewerUserId, int rating, String comment) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_REVIEW)) {
            ps.setInt(1, exchangeId);
            ps.setInt(2, reviewerUserId);
            ps.setInt(3, rating);
            ps.setString(4, comment);
            ps.executeUpdate();
        }
    }

    private ExchangeRecord mapExchangeRecord(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime created = ts != null ? ts.toLocalDateTime() : null;
        return new ExchangeRecord(
                rs.getInt("exchange_id"),
                rs.getInt("requester_user_id"),
                rs.getInt("owner_user_id"),
                rs.getString("status"),
                created,
                rs.getInt("item_id"),
                rs.getString("item_name"),
                rs.getString("other_party")
        );
    }

    private ExchangeLockRow loadExchangeForOwnerUpdate(Connection conn, int exchangeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_EXCHANGE_FOR_OWNER_UPDATE)) {
            ps.setInt(1, exchangeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new ExchangeLockRow(
                        rs.getInt("exchange_id"),
                        rs.getInt("requester_user_id"),
                        rs.getInt("owner_user_id"),
                        rs.getString("status"),
                        rs.getInt("item_id")
                );
            }
        }
    }

    private void updateExchangeStatus(Connection conn, int exchangeId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_EXCHANGE_STATUS)) {
            ps.setString(1, status);
            ps.setInt(2, exchangeId);
            int n = ps.executeUpdate();
            if (n != 1) {
                throw new SQLException("Could not update exchange status.");
            }
        }
    }

    private record ExchangeLockRow(int exchangeId, int requesterUserId, int ownerUserId, String status, int itemId) {
    }
}
