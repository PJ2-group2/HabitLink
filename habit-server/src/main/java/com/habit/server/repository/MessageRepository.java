package com.habit.server.repository;

import com.habit.domain.Message;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageRepository {
  private static final Logger logger = LoggerFactory.getLogger(MessageRepository.class);
  public static class MessageEntry {
    public final String id, senderId, teamId, content;
    public final LocalDateTime time;
    public MessageEntry(String id, String senderId, String teamId,
                        String content, LocalDateTime time) {
      this.id = id;
      this.senderId = senderId;
      this.teamId = teamId;
      this.content = content;
      this.time = time;
    }
  };

  private final String databaseUrl;

  public MessageRepository() { this("jdbc:sqlite:habit.db"); }

  public MessageRepository(String databaseUrl) {
    this.databaseUrl = databaseUrl;
    try (Connection conn = DriverManager.getConnection(databaseUrl);
         Statement stmt = conn.createStatement()) {
      String sql = "CREATE TABLE IF NOT EXISTS messages ("
                   + "message_id TEXT PRIMARY KEY,"
                   + "sender_id TEXT NOT NULL,"
                   + "team_id TEXT NOT NULL,"
                   + "content TEXT NOT NULL,"
                   + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";
      stmt.execute(sql);
    } catch (SQLException e) {
      logger.error("Error initializing messages table: {}", e.getMessage(), e);
    }
  }

  public void save(Message message) {
    String sql = "INSERT INTO messages (message_id, sender_id, team_id, "
                 + "content, timestamp) VALUES (?, ?, ?, ?, ?)";
    try (Connection conn = DriverManager.getConnection(databaseUrl);
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(
          1,
          java.util.UUID.randomUUID().toString()); // メッセージIDをUUIDで生成
      pstmt.setString(2, message.getSender().getUserId());
      pstmt.setString(3, message.getTeamID());
      pstmt.setString(4, message.getContent());
      pstmt.setTimestamp(5, Timestamp.valueOf(message.getTimestamp()));
      pstmt.executeUpdate();
    } catch (SQLException e) {
      logger.error("Error saving message: {}", e.getMessage(), e);
    }
  }

  public List<MessageEntry> findMessagesByteamID(String teamID, int limit) {
    List<MessageEntry> messages = new ArrayList<>();
    String sql =
        "SELECT message_id, sender_id, team_id, content, timestamp FROM "
        + "messages WHERE team_id = ? LIMIT ?";
    try (Connection conn = DriverManager.getConnection(databaseUrl);
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, teamID);
      pstmt.setInt(2, limit);
      ResultSet rs = pstmt.executeQuery();
      while (rs.next()) {

        Timestamp ts = rs.getTimestamp("timestamp");
        LocalDateTime time = ts.toLocalDateTime();

        // MessageTypeはNORMAL固定でインスタンス化
        MessageEntry entries = new MessageEntry(
            rs.getString("message_id"), rs.getString("sender_id"),
            rs.getString("team_id"), rs.getString("content"), time);
        messages.add(entries);
      }
    } catch (SQLException e) {
      logger.error("Error finding messages by team ID: {}", e.getMessage(), e);
    }
    return messages;
  }

  /**
   * 削除メソッド。
   */
  public void delete(String messageId) {
    String sql = "DELETE FROM messages WHERE message_id = ?";
    try (Connection conn = DriverManager.getConnection(databaseUrl);
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, messageId);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      logger.error("Error deleting message: {}", e.getMessage(), e);
    }
  }
}
