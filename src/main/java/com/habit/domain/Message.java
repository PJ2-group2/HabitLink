package com.habit.domain;

import java.time.LocalDateTime;
import org.json.JSONObject;

/**
 * チャットや通知などのメッセージ情報を表すクラス。
 */
public class Message {
  private final String messageId;
  private final User sender;
  private final String teamID;
  private final String content;
  private LocalDateTime timestamp;
  private final MessageType type;

  public Message(String messageId, User sender, String teamID, String content,
                 MessageType type) {
    this.messageId = messageId;
    this.sender = sender;
    this.teamID = teamID;
    this.content = content;
    this.type = type;
    this.timestamp = LocalDateTime.now();
  }

  public String getMessageId() { return messageId; }

  public User getSender() { return sender; }

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
    json.put("sender", sender.toJson());
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
    User sender = User.fromJson(json.getJSONObject("sender"));
    String teamID = json.getString("teamID");
    String content = json.getString("content");
    LocalDateTime timestamp = LocalDateTime.parse(json.getString("timestamp"));
    MessageType type = MessageType.valueOf(json.getString("type"));
    // Create instance and override timestamp
    Message message = new Message(messageId, sender, teamID, content, type);
    message.timestamp = timestamp;
    return message;
  }
}
