package com.habit.server.repository;

import com.habit.domain.UserTaskStatus;
import java.util.*;
import java.time.LocalDate;
import java.sql.*;

/**
 * UserTaskStatusのDB連携用リポジトリ
 */
public class UserTaskStatusRepository {
    private static final String DB_URL = "jdbc:sqlite:habit.db";

    public UserTaskStatusRepository() {
        // テーブル作成（なければ）
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS user_task_statuses (" +
                    "userId TEXT," +
                    "taskId TEXT," +
                    "originalTaskId TEXT," +
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
            
            // 既存レコードのoriginalTaskIdを更新（nullの場合のみ）
            String updateSql = "UPDATE user_task_statuses SET originalTaskId = ? WHERE originalTaskId IS NULL AND taskId = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement("SELECT DISTINCT taskId FROM user_task_statuses WHERE originalTaskId IS NULL");
                 ResultSet rs = updateStmt.executeQuery()) {
                
                while (rs.next()) {
                    String taskId = rs.getString("taskId");
                    String originalTaskId = extractOriginalTaskId(taskId);
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                        pstmt.setString(1, originalTaskId);
                        pstmt.setString(2, taskId);
                        pstmt.executeUpdate();
                    }
                }
            } catch (SQLException ex) {
                System.err.println("originalTaskId更新エラー: " + ex.getMessage());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * TaskIDから元のTaskIDを抽出（UserTaskStatusクラスと同じロジック）
     */
    private String extractOriginalTaskId(String taskId) {
        if (taskId.contains("_")) {
            // 自動生成されたTaskIDの場合（例: "dailyTask_20250630"）
            return taskId.substring(0, taskId.indexOf("_"));
        }
        // 元のTaskIDの場合はそのまま返す
        return taskId;
    }

    // ユーザIDで検索
    public List<UserTaskStatus> findByUserId(String userId) {
        List<UserTaskStatus> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
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
    // userIdとteamIdで、そのユーザーが担当するチーム内タスクID一覧を取得
    public List<String> findTaskIdsByUserIdAndTeamId(String userId, String teamId) {
        List<String> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
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
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
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

    // ユーザID・タスクID・日付で検索
    public Optional<UserTaskStatus> findByUserIdAndTaskIdAndDate(String userId, String taskId, LocalDate date) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
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

    // ユーザID・元のTaskID・日付で検索（自動再設定の重複チェック用）
    public Optional<UserTaskStatus> findByUserIdAndOriginalTaskIdAndDate(String userId, String originalTaskId, LocalDate date) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM user_task_statuses WHERE userId = ? AND originalTaskId = ? AND date = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, originalTaskId);
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
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
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
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
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
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
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

    // originalTaskIdを使用してチーム・日付範囲で進捗を取得（重複排除用）
    public List<UserTaskStatus> findByTeamIdAndDateRangeGroupedByOriginalTaskId(String teamId, LocalDate from, LocalDate to) {
        List<UserTaskStatus> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // originalTaskIdごとにグループ化し、最新のタスクの進捗のみを取得
            String sql = "SELECT uts.* FROM user_task_statuses uts " +
                         "JOIN tasks t ON uts.originalTaskId = t.originalTaskId " +
                         "WHERE t.teamID = ? AND uts.date >= ? AND uts.date <= ? " +
                         "AND uts.taskId = (SELECT MAX(taskId) FROM user_task_statuses uts2 " +
                         "                  WHERE uts2.originalTaskId = uts.originalTaskId " +
                         "                  AND uts2.userId = uts.userId " +
                         "                  AND uts2.date = uts.date)";
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
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "INSERT OR REPLACE INTO user_task_statuses (userId, taskId, originalTaskId, teamId, date, isDone, completionTimestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, status.getUserId());
                pstmt.setString(2, status.getTaskId());
                pstmt.setString(3, status.getOriginalTaskId());
                pstmt.setString(4, status.getTeamId());
                pstmt.setString(5, status.getDate().toString());
                pstmt.setInt(6, status.isDone() ? 1 : 0);
                pstmt.setString(7, status.getCompletionTimestamp() != null ? status.getCompletionTimestamp().toString() : null);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 全件取得
    public List<UserTaskStatus> findAll() {
        List<UserTaskStatus> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
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
        
        // originalTaskIdを設定（既存データとの互換性のためnullチェック）
        String originalTaskId = rs.getString("originalTaskId");
        if (originalTaskId != null) {
            status.setOriginalTaskId(originalTaskId);
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
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
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