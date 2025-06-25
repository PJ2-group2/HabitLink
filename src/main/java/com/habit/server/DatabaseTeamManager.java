package com.habit.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Team manager that stores teams and tasks in SQLite.
 */
public class DatabaseTeamManager {
    private final Connection connection;

    public DatabaseTeamManager(String url) {
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
                    "CREATE TABLE IF NOT EXISTS teams (id TEXT PRIMARY KEY)");
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS tasks (team_id TEXT, task TEXT, " +
                            "FOREIGN KEY(team_id) REFERENCES teams(id))");
        }
    }

    public synchronized boolean createTeam(String teamID) {
        String sql = "INSERT INTO teams(id) VALUES(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamID);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            // duplicate
            return false;
        }
    }

    public synchronized boolean teamExists(String teamID) {
        String sql = "SELECT 1 FROM teams WHERE id=? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized TaskManager getTaskManager(String teamID) {
        return new DatabaseTaskManager(connection, teamID);
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            // ignore
        }
    }
}
