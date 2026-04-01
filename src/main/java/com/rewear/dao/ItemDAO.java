package com.rewear.dao;

import com.rewear.DBConnection;
import com.rewear.models.Item;
import com.rewear.models.ItemBrowseRow;
import com.rewear.models.Lookup;
import com.rewear.models.MyItemRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for {@code ITEMS}, {@code CATEGORY}, {@code SIZE}, {@code ITEM_CONDITION}, {@code ITEM_IMAGES}.
 */
public class ItemDAO {

    private static final String INSERT_ITEM =
            "INSERT INTO ITEMS (owner_user_id, category_id, size_id, condition_id, item_name, brand, "
                    + "description, points_value, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'AVAILABLE')";

    private static final String INSERT_IMAGE =
            "INSERT INTO ITEM_IMAGES (item_id, image_url) VALUES (?, ?)";

    private static final String SELECT_AVAILABLE_OTHERS =
            "SELECT i.item_id, i.item_name, i.brand, i.points_value, u.username AS owner_username, "
                    + "c.name AS category_name, s.label AS size_label, ic.label AS condition_label, "
                    + "i.description, "
                    + "(SELECT im.image_url FROM ITEM_IMAGES im WHERE im.item_id = i.item_id "
                    + "ORDER BY im.image_id LIMIT 1) AS image_url "
                    + "FROM ITEMS i "
                    + "JOIN USERS u ON u.user_id = i.owner_user_id "
                    + "JOIN CATEGORY c ON c.category_id = i.category_id "
                    + "JOIN SIZE s ON s.size_id = i.size_id "
                    + "JOIN ITEM_CONDITION ic ON ic.condition_id = i.condition_id "
                    + "WHERE i.status = 'AVAILABLE' AND i.owner_user_id <> ? "
                    + "ORDER BY i.created_at DESC";

    private static final String SELECT_BY_OWNER =
            "SELECT i.item_id, i.item_name, i.brand, i.points_value, i.status, "
                    + "c.name AS category_name, s.label AS size_label, ic.label AS condition_label, "
                    + "i.description, "
                    + "(SELECT im.image_url FROM ITEM_IMAGES im WHERE im.item_id = i.item_id "
                    + "ORDER BY im.image_id LIMIT 1) AS image_url "
                    + "FROM ITEMS i "
                    + "JOIN CATEGORY c ON c.category_id = i.category_id "
                    + "JOIN SIZE s ON s.size_id = i.size_id "
                    + "JOIN ITEM_CONDITION ic ON ic.condition_id = i.condition_id "
                    + "WHERE i.owner_user_id = ? ORDER BY i.created_at DESC";

    private static final String SELECT_BY_ID_FOR_UPDATE =
            "SELECT item_id, owner_user_id, category_id, size_id, condition_id, item_name, brand, description, "
                    + "points_value, status FROM ITEMS WHERE item_id = ? FOR UPDATE";

    private static final String UPDATE_STATUS =
            "UPDATE ITEMS SET status = ? WHERE item_id = ?";

    private static final String LIST_CATEGORIES = "SELECT category_id, name FROM CATEGORY ORDER BY name";
    private static final String LIST_SIZES = "SELECT size_id, label FROM SIZE ORDER BY size_id";
    private static final String LIST_CONDITIONS = "SELECT condition_id, label FROM ITEM_CONDITION ORDER BY condition_id";

    public int insertItem(int ownerUserId, int categoryId, int sizeId, int conditionId,
                          String itemName, String brand, String description, int pointsValue,
                          String optionalImageUrl) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int itemId;
                try (PreparedStatement ps = conn.prepareStatement(INSERT_ITEM, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, ownerUserId);
                    ps.setInt(2, categoryId);
                    ps.setInt(3, sizeId);
                    ps.setInt(4, conditionId);
                    ps.setString(5, itemName);
                    ps.setString(6, brand);
                    ps.setString(7, description);
                    ps.setInt(8, pointsValue);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("No item id generated.");
                        }
                        itemId = keys.getInt(1);
                    }
                }
                if (optionalImageUrl != null && !optionalImageUrl.isBlank()) {
                    try (PreparedStatement ps = conn.prepareStatement(INSERT_IMAGE)) {
                        ps.setInt(1, itemId);
                        ps.setString(2, optionalImageUrl.trim());
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                return itemId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<ItemBrowseRow> listAvailableExcludingOwner(int ownerUserIdToExclude) throws SQLException {
        List<ItemBrowseRow> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_AVAILABLE_OTHERS)) {
            ps.setInt(1, ownerUserIdToExclude);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ItemBrowseRow(
                            rs.getInt("item_id"),
                            rs.getString("item_name"),
                            rs.getString("brand"),
                            rs.getString("owner_username"),
                            rs.getInt("points_value"),
                            rs.getString("category_name"),
                            rs.getString("size_label"),
                            rs.getString("condition_label"),
                            rs.getString("description"),
                            rs.getString("image_url")
                    ));
                }
            }
        }
        return rows;
    }

    public List<MyItemRow> listMyItems(int ownerUserId) throws SQLException {
        List<MyItemRow> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_OWNER)) {
            ps.setInt(1, ownerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new MyItemRow(
                            rs.getInt("item_id"),
                            rs.getString("item_name"),
                            rs.getString("brand"),
                            rs.getInt("points_value"),
                            rs.getString("category_name"),
                            rs.getString("size_label"),
                            rs.getString("condition_label"),
                            rs.getString("status"),
                            rs.getString("description"),
                            rs.getString("image_url")
                    ));
                }
            }
        }
        return rows;
    }

    /**
     * Loads item for update within an open transaction (row lock).
     */
    public Item loadByIdForUpdate(Connection conn, int itemId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID_FOR_UPDATE)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Item item = new Item();
                item.setItemId(rs.getInt("item_id"));
                item.setOwnerUserId(rs.getInt("owner_user_id"));
                item.setCategoryId(rs.getInt("category_id"));
                item.setSizeId(rs.getInt("size_id"));
                item.setConditionId(rs.getInt("condition_id"));
                item.setItemName(rs.getString("item_name"));
                item.setBrand(rs.getString("brand"));
                item.setDescription(rs.getString("description"));
                item.setPointsValue(rs.getInt("points_value"));
                item.setStatus(rs.getString("status"));
                return item;
            }
        }
    }

    public void updateStatus(Connection conn, int itemId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, status);
            ps.setInt(2, itemId);
            int n = ps.executeUpdate();
            if (n != 1) {
                throw new SQLException("Failed to update item status.");
            }
        }
    }

    public List<Lookup> listCategories() throws SQLException {
        return loadLookups(LIST_CATEGORIES, "category_id", "name");
    }

    public List<Lookup> listSizes() throws SQLException {
        return loadLookups(LIST_SIZES, "size_id", "label");
    }

    public List<Lookup> listConditions() throws SQLException {
        return loadLookups(LIST_CONDITIONS, "condition_id", "label");
    }

    private List<Lookup> loadLookups(String sql, String idCol, String labelCol) throws SQLException {
        List<Lookup> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Lookup(rs.getInt(idCol), rs.getString(labelCol)));
            }
        }
        return list;
    }
}
