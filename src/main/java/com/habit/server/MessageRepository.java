package com.habit.server;

import com.habit.domain.Message;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private static final String DB_URL = "jdbc:sqlite:habit.db";

    public MessageRepository() {
        initDatabase();
    }

    private void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS messages (" +
                         "message_id TEXT PRIMARY KEY," +
                         "sender_id TEXT NOT NULL," +
                         "team_id TEXT NOT NULL," +
                         "content TEXT NOT NULL," +
                         "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error initializing messages table: " + e.getMessage());
        }
    }
    public void save(Message message) {
        String sql = "INSERT INTO messages (message_id, sender_id, team_id, content) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, java.util.UUID.randomUUID().toString()); // メッセージIDをUUIDで生成
            pstmt.setString(2, message.getSenderId());
            pstmt.setString(3, message.getTeamID());
            pstmt.setString(4, message.getContent());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving message: " + e.getMessage());
        }
    }

    public List<Message> findMessagesByteamID(String teamID, int limit) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT message_id, sender_id, team_id, content, timestamp FROM messages WHERE team_id = ? ORDER BY timestamp DESC LIMIT ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, teamID);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // MessageTypeはNORMAL固定でインスタンス化
                Message message = new Message(
                    rs.getString("content"),
                    rs.getString("sender_id"),
                    rs.getString("team_id"),
                    rs.getString("content"),
                    com.habit.domain.MessageType.NORMAL // MessageTypeはNORMAL固定
                );
                messages.add(message);
            }
        } catch (SQLException e) {
            System.err.println("Error finding messages by team ID: " + e.getMessage());
        }
        return messages;
    }
}