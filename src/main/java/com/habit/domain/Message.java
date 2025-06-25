package com.habit.domain;

import java.time.LocalDateTime;

/**
 * チャットや通知などのメッセージ情報を表すクラス。
 */
public class Message {
    private String messageId;
    private String senderId;
    private String teamID;
    private String content;
    private LocalDateTime timestamp;
    private MessageType type;

    public Message(String messageId, String senderId, String teamID, String content, MessageType type) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.teamID = teamID;
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

    public String getTeamID() {
        return teamID;
    }

    public MessageType getType() {
        return type;
    }
}