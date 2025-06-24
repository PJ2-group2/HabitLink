package com.habit.server;

import com.habit.domain.User;

import java.sql.*;

public class UserRepository {
    private static final String DB_URL = "jdbc:sqlite:habit.db";

    public UserRepository() {
        // usersテーブルがなければ作成
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "userId TEXT PRIMARY KEY," +
                    "username TEXT UNIQUE," +
                    "hashedPassword TEXT," +
                    "sabotagePoints INTEGER," +
                    "joinedTeamIds TEXT," + // 追加: チームIDをカンマ区切りで保存
                    "profileIconPath TEXT," +
                    "bio TEXT)";
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User findById(String userId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM users WHERE userId = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User findByUsername(String username) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM users WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void save(User user) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "INSERT OR REPLACE INTO users (userId, username, hashedPassword, sabotagePoints, joinedTeamIds, profileIconPath, bio) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, user.getUserId());
                pstmt.setString(2, user.getUsername());
                pstmt.setString(3, user.getHashedPassword());
                pstmt.setInt(4, user.getSabotagePoints());
                // joinedTeamIdsをカンマ区切りで保存
                pstmt.setString(5, String.join(",", user.getJoinedTeamIds()));
                pstmt.setString(6, null); // profileIconPath未使用
                pstmt.setString(7, null); // bio未使用
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateSabotagePoints(String userId, int points) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "UPDATE users SET sabotagePoints = ? WHERE userId = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, points);
                pstmt.setString(2, userId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getString("userId"),
                rs.getString("username"),
                rs.getString("hashedPassword")
        );
        user.addSabotagePoints(rs.getInt("sabotagePoints") - user.getSabotagePoints());
        // joinedTeamIdsを復元
        String joined = rs.getString("joinedTeamIds");
        if (joined != null && !joined.isEmpty()) {
            for (String tid : joined.split(",")) {
                user.addJoinedTeamId(tid);
            }
        }
        return user;
    }
}