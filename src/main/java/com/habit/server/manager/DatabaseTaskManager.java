package com.habit.server.manager;

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
    private final String teamID;

    public DatabaseTaskManager(Connection connection, String teamID) {
        this.connection = connection;
        this.teamID = teamID;
    }

    @Override
    public synchronized void addTask(String task) {
        String sql = "INSERT INTO tasks(teamId, taskId) VALUES(?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamID);
            ps.setString(2, task);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized List<String> getTasks() {
        String sql = "SELECT taskId FROM tasks WHERE teamId=?";
        List<String> tasks = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamID);
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
        String sql = "SELECT 1 FROM tasks WHERE teamId=? AND taskId=? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamID);
            ps.setString(2, task);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
