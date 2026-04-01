package com.rewear.models;

/**
 * Row for the "My Items" table.
 */
public class MyItemRow {

    private final int itemId;
    private final String itemName;
    private final String brand;
    private final int pointsValue;
    private final String categoryName;
    private final String sizeLabel;
    private final String conditionLabel;
    private final String status;
    private final String description;
    private final String imageUrl;

    public MyItemRow(int itemId, String itemName, String brand, int pointsValue,
                     String categoryName, String sizeLabel, String conditionLabel, String status,
                     String description, String imageUrl) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.brand = brand;
        this.pointsValue = pointsValue;
        this.categoryName = categoryName;
        this.sizeLabel = sizeLabel;
        this.conditionLabel = conditionLabel;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
