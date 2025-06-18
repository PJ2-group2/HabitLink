package com.habit.domain;

import java.time.LocalDateTime;

public class Message {
    private String messageId;
    private String senderId;
    private String roomId;
    private String content;
    private LocalDateTime timestamp;
    private MessageType type;

    public Message(String messageId, String senderId, String roomId, String content, MessageType type) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.roomId = roomId;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public String getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getRoomId() {
        return roomId;
    }

    public MessageType getType() {
        return type;
    }
}