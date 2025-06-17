package com.habit.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Task manager backed by SQLite database.
 */
public class DatabaseTaskManager extends TaskManager {
    private final Connection connection;
    private final String roomId;

    public DatabaseTaskManager(Connection connection, String roomId) {
        this.connection = connection;
        this.roomId = roomId;
    }

    @Override
    public synchronized void addTask(String task) {
        String sql = "INSERT INTO tasks(room_id, task) VALUES(?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, roomId);
            ps.setString(2, task);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized List<String> getTasks() {
        String sql = "SELECT task FROM tasks WHERE room_id=?";
        List<String> tasks = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return tasks;
    }

    @Override
    public synchronized boolean taskExists(String task) {
        String sql = "SELECT 1 FROM tasks WHERE room_id=? AND task=? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, roomId);
            ps.setString(2, task);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
