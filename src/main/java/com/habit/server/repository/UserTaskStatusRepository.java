package com.habit.server.repository;

import com.habit.domain.UserTaskStatus;
import java.util.*;
import java.time.LocalDate;
import java.sql.*;

/**
 * UserTaskStatusのDB連携用リポジトリ
 */
public class UserTaskStatusRepository {
    private final String dbUrl;

    public UserTaskStatusRepository() {
        this("jdbc:sqlite:habit.db");
    }

    public UserTaskStatusRepository(String dbUrl) {
        this.dbUrl = dbUrl;
        // テーブル作成（なければ）
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS user_task_statuses (" +
                    "userId TEXT," +
                    "taskId TEXT," +
                    "teamId TEXT," +
                    "date TEXT," +
                    "isDone INTEGER," +
                    "completionTimestamp TEXT," +
                    "PRIMARY KEY(userId, taskId, date)" +
                    ")";
            stmt.execute(sql);
            
            // 既存テーブルにteamIdカラムを追加（存在しない場合）
            ResultSet teamIdCheck = stmt.executeQuery("PRAGMA table_info(user_task_statuses)");
            boolean hasTeamId = false;
            while (teamIdCheck.next()) {
                String col = teamIdCheck.getString("name");
                if ("teamId".equalsIgnoreCase(col))
                    hasTeamId = true;
            }
            if (!hasTeamId) {
                stmt.execute("ALTER TABLE user_task_statuses ADD COLUMN teamId TEXT");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ユーザIDで検索
    public List<UserTaskStatus> findByUserId(String userId) {
        List<UserTaskStatus> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "SELECT * FROM user_task_statuses WHERE userId = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    UserTaskStatus status = mapRowToStatus(rs);
                    result.add(status);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
    // ユーザID・チームIDで検索
    public List<UserTaskStatus> findByUserIdAndTeamId(String userId, String teamId) {
        List<UserTaskStatus> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "SELECT uts.* FROM user_task_statuses uts " +
                         "JOIN tasks t ON uts.taskId = t.taskId " +
                         "WHERE uts.userId = ? AND t.teamID = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, teamId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    UserTaskStatus status = mapRowToStatus(rs);
                    result.add(status);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // userIdとteamIdで、そのユーザーが担当するチーム内タスクID一覧を取得
    public List<String> findTaskIdsByUserIdAndTeamId(String userId, String teamId) {
        List<String> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "SELECT uts.taskId FROM user_task_statuses uts " +
                         "JOIN tasks t ON uts.taskId = t.taskId " +
                         "WHERE uts.userId = ? AND t.teamID = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, teamId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    result.add(rs.getString("taskId"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // タスクIDで検索
    public List<UserTaskStatus> findByTaskId(String taskId) {
        List<UserTaskStatus> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "SELECT * FROM user_task_statuses WHERE taskId = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, taskId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    UserTaskStatus status = mapRowToStatus(rs);
                    result.add(status);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // ユーザID・タスクIDで検索
    public Optional<UserTaskStatus> findByUserIdAndTaskId(String userId, String taskId) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "SELECT * FROM user_task_statuses WHERE userId = ? AND taskId = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, taskId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return Optional.of(mapRowToStatus(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // タスクID・日付で検索
    public List<UserTaskStatus> findByTaskIdAndDate(String taskId, LocalDate date) {
        List<UserTaskStatus> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "SELECT * FROM user_task_statuses WHERE taskId = ? AND date = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, taskId);
                pstmt.setString(2, date.toString());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    result.add(mapRowToStatus(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }


    // ユーザID・タスクID・日付で検索
    public Optional<UserTaskStatus> findByUserIdAndTaskIdAndDate(String userId, String taskId, LocalDate date) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "SELECT * FROM user_task_statuses WHERE userId = ? AND taskId = ? AND date = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, taskId);
                pstmt.setString(3, date.toString());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return Optional.of(mapRowToStatus(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // ユーザー・チーム・日付で一括取得
    public List<UserTaskStatus> findByUserIdAndTeamIdAndDate(String userId, String teamId, LocalDate date) {
        List<UserTaskStatus> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "SELECT uts.* FROM user_task_statuses uts " +
                         "JOIN tasks t ON uts.taskId = t.taskId " +
                         "WHERE uts.userId = ? AND t.teamID = ? AND uts.date = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, teamId);
                pstmt.setString(3, date.toString());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    UserTaskStatus status = mapRowToStatus(rs);
                    result.add(status);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // チームID・日付で全メンバー分の進捗を取得
    public List<UserTaskStatus> findByTeamIdAndDate(String teamId, LocalDate date) {
        List<UserTaskStatus> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "SELECT uts.* FROM user_task_statuses uts " +
                         "JOIN tasks t ON uts.taskId = t.taskId " +
                         "WHERE t.teamID = ? AND uts.date = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, teamId);
                pstmt.setString(2, date.toString());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    UserTaskStatus status = mapRowToStatus(rs);
                    result.add(status);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // チームID・日付範囲で全メンバー分の進捗を取得
    public List<UserTaskStatus> findByTeamIdAndDateRange(String teamId, LocalDate from, LocalDate to) {
        List<UserTaskStatus> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "SELECT uts.* FROM user_task_statuses uts " +
                         "JOIN tasks t ON uts.taskId = t.taskId " +
                         "WHERE t.teamID = ? AND uts.date >= ? AND uts.date <= ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, teamId);
                pstmt.setString(2, from.toString());
                pstmt.setString(3, to.toString());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    UserTaskStatus status = mapRowToStatus(rs);
                    result.add(status);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // 保存・更新
    public void save(UserTaskStatus status) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "INSERT OR REPLACE INTO user_task_statuses (userId, taskId, teamId, date, isDone, completionTimestamp) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, status.getUserId());
                pstmt.setString(2, status.getTaskId());
                pstmt.setString(3, status.getTeamId());
                pstmt.setString(4, status.getDate().toString());
                pstmt.setInt(5, status.isDone() ? 1 : 0);
                pstmt.setString(6, status.getCompletionTimestamp() != null ? status.getCompletionTimestamp().toString() : null);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 全件取得
    public List<UserTaskStatus> findAll() {
        List<UserTaskStatus> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "SELECT * FROM user_task_statuses";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    UserTaskStatus status = mapRowToStatus(rs);
                    result.add(status);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // ResultSetからUserTaskStatusを生成
    private UserTaskStatus mapRowToStatus(ResultSet rs) throws SQLException {
        String teamId = rs.getString("teamId");
        UserTaskStatus status;
        
        if (teamId != null) {
            // チーム共通タスクの場合
            status = new UserTaskStatus(
                rs.getString("userId"),
                rs.getString("taskId"),
                teamId,
                LocalDate.parse(rs.getString("date")),
                rs.getInt("isDone") == 1
            );
        } else {
            // 個人タスクの場合
            status = new UserTaskStatus(
                rs.getString("userId"),
                rs.getString("taskId"),
                LocalDate.parse(rs.getString("date")),
                rs.getInt("isDone") == 1
            );
        }
        
        String ts = rs.getString("completionTimestamp");
        if (ts != null) {
            status.setDone(true); // completionTimestampはsetDoneで自動設定
        }
        return status;
    }

    // teamIdがnullでないユーザーのタスク状況を取得（チーム共通タスクのみ）
    public List<UserTaskStatus> findByUserIdAndDateAndTeamIdNotNull(String userId, LocalDate date) {
        List<UserTaskStatus> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "SELECT uts.* FROM user_task_statuses uts " +
                         "JOIN tasks t ON uts.taskId = t.taskId " +
                         "WHERE uts.userId = ? AND uts.date = ? AND t.teamID IS NOT NULL";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, date.toString());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    UserTaskStatus status = mapRowToStatus(rs);
                    result.add(status);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
}