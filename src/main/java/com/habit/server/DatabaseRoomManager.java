package com.habit.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Room manager that stores rooms and tasks in SQLite.
 */
public class DatabaseRoomManager {
    private final Connection connection;

    public DatabaseRoomManager(String url) {
        try {
            connection = DriverManager.getConnection(url);
            initSchema();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void initSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS rooms (id TEXT PRIMARY KEY)");
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS tasks (room_id TEXT, task TEXT, " +
                            "FOREIGN KEY(room_id) REFERENCES rooms(id))");
        }
    }

    public synchronized boolean createRoom(String roomId) {
        String sql = "INSERT INTO rooms(id) VALUES(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, roomId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            // duplicate
            return false;
        }
    }

    public synchronized boolean roomExists(String roomId) {
        String sql = "SELECT 1 FROM rooms WHERE id=? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized TaskManager getTaskManager(String roomId) {
        return new DatabaseTaskManager(connection, roomId);
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            // ignore
        }
    }
}
