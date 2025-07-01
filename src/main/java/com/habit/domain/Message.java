package com.habit.domain;

import java.time.LocalDateTime;
import org.json.JSONObject;

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

  public Message(String messageId, String senderId, String teamID,
                 String content, MessageType type) {
    this.messageId = messageId;
    this.senderId = senderId;
    this.teamID = teamID;
    this.content = content;
    this.type = type;
    this.timestamp = LocalDateTime.now();
  }

  public String getMessageId() { return messageId; }

  public String getSenderId() { return senderId; }

  public String getTeamID() { return teamID; }

  public String getContent() { return content; }

  public LocalDateTime getTimestamp() { return timestamp; }

  public MessageType getType() { return type; }

  /**
   * Serialize this Message to a JSONObject.
   */
  public JSONObject toJson() {
    JSONObject json = new JSONObject();
    json.put("messageId", messageId);
    json.put("senderId", senderId);
    json.put("teamID", teamID);
    json.put("content", content);
    json.put("timestamp", timestamp.toString());
    json.put("type", type.name());
    return json;
  }

  /**
   * Deserialize a Message from a JSONObject.
   */
  public static Message fromJson(JSONObject json) {
    String messageId = json.getString("messageId");
    String senderId = json.getString("senderId");
    String teamID = json.getString("teamID");
    String content = json.getString("content");
    LocalDateTime timestamp = LocalDateTime.parse(json.getString("timestamp"));
    MessageType type = MessageType.valueOf(json.getString("type"));
    // Create instance and override timestamp
    Message message = new Message(messageId, senderId, teamID, content, type);
    message.timestamp = timestamp;
    return message;
  }
}
