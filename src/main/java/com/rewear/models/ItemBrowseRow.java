package com.rewear.models;

/**
 * Row for {@link javax.swing.JTable} when browsing items (denormalized for display).
 */
public class ItemBrowseRow {

    private final int itemId;
    private final String itemName;
    private final String brand;
    private final String ownerUsername;
    private final int pointsValue;
    private final String categoryName;
    private final String sizeLabel;
    private final String conditionLabel;
    private final String description;
    private final String imageUrl;

    public ItemBrowseRow(int itemId, String itemName, String brand, String ownerUsername, int pointsValue,
                         String categoryName, String sizeLabel, String conditionLabel, String description,
                         String imageUrl) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.brand = brand;
        this.ownerUsername = ownerUsername;
        this.pointsValue = pointsValue;
        this.categoryName = categoryName;
        this.sizeLabel = sizeLabel;
        this.conditionLabel = conditionLabel;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public int getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getBrand() {
        return brand;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public int getPointsValue() {
        return pointsValue;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getSizeLabel() {
        return sizeLabel;
    }

    public String getConditionLabel() {
        return conditionLabel;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
