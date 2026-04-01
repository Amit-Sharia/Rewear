package com.rewear.models;

import java.time.LocalDateTime;

public class ChatMessage {
    private final int messageId;
    private final int exchangeId;
    private final int senderUserId;
    private final String senderUsername;
    private final String messageText;
    private final LocalDateTime sentAt;

    public ChatMessage(int messageId, int exchangeId, int senderUserId, String senderUsername,
                       String messageText, LocalDateTime sentAt) {
        this.messageId = messageId;
        this.exchangeId = exchangeId;
        this.senderUserId = senderUserId;
        this.senderUsername = senderUsername;
        this.messageText = messageText;
        this.sentAt = sentAt;
    }

    public int getMessageId() {
        return messageId;
    }

    public int getExchangeId() {
        return exchangeId;
    }

    public int getSenderUserId() {
        return senderUserId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getMessageText() {
        return messageText;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
