package com.rewear.models;

import java.time.LocalDateTime;

/**
 * Represents a row in {@code EXCHANGES} joined with item info for UI lists.
 */
public class ExchangeRecord {

    private final int exchangeId;
    private final int requesterUserId;
    private final int ownerUserId;
    private final String status;
    private final LocalDateTime createdAt;
    private final int itemId;
    private final String itemName;
    private final String otherPartyUsername;

    public ExchangeRecord(int exchangeId, int requesterUserId, int ownerUserId, String status,
                          LocalDateTime createdAt, int itemId, String itemName, String otherPartyUsername) {
        this.exchangeId = exchangeId;
        this.requesterUserId = requesterUserId;
        this.ownerUserId = ownerUserId;
        this.status = status;
        this.createdAt = createdAt;
        this.itemId = itemId;
        this.itemName = itemName;
        this.otherPartyUsername = otherPartyUsername;
    }

    public int getExchangeId() {
        return exchangeId;
    }

    public int getRequesterUserId() {
        return requesterUserId;
    }

    public int getOwnerUserId() {
        return ownerUserId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getOtherPartyUsername() {
        return otherPartyUsername;
    }
}
