package com.habit.server;

import com.habit.domain.UserTaskStatus;
import com.habit.domain.Task;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class TaskRepository {
    private static final String DB_URL = "jdbc:sqlite:habit.db";

    public TaskRepository() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            // タスクテーブル
            String sql = "CREATE TABLE IF NOT EXISTS tasks (" +
                    "taskId TEXT PRIMARY KEY," +
                    "taskName TEXT," +
                    "description TEXT," +
                    "estimatedMinutes INTEGER," +
                    "repeatDays TEXT," + // カンマ区切り
                    "isTeamTask INTEGER," +
                    "teamID TEXT" +
                    ")";
            stmt.execute(sql);
            // ユーザーごとのタスク達成状況
            String sql2 = "CREATE TABLE IF NOT EXISTS user_task_statuses (" +
                    "userId TEXT," +
                    "taskId TEXT," +
                    "date TEXT," +
                    "isDone INTEGER," +
                    "completionTimestamp TEXT," +
                    "PRIMARY KEY(userId, taskId, date)" +
                    ")";
            stmt.execute(sql2);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public UserTaskStatus findUserTaskStatus(String userId, String taskId, LocalDate date) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM user_task_statuses WHERE userId = ? AND taskId = ? AND date = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, taskId);
                pstmt.setString(3, date.toString());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    UserTaskStatus status = new UserTaskStatus(
                            rs.getString("userId"),
                            rs.getString("taskId"),
                            LocalDate.parse(rs.getString("date")),
                            rs.getInt("isDone") == 1
                    );
                    String ts = rs.getString("completionTimestamp");
                    if (ts != null) {
                        status.setDone(true); // completionTimestampはsetDoneで自動設定
                    }
                    return status;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveUserTaskStatus(UserTaskStatus status) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "INSERT OR REPLACE INTO user_task_statuses (userId, taskId, date, isDone, completionTimestamp) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, status.getUserId());
                pstmt.setString(2, status.getTaskId());
                pstmt.setString(3, status.getDate().toString());
                pstmt.setInt(4, status.isDone() ? 1 : 0);
                pstmt.setString(5, status.getCompletionTimestamp() != null ? status.getCompletionTimestamp().toString() : null);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<UserTaskStatus> findUserTaskStatusesForPeriod(String userId, LocalDate startDate, LocalDate endDate) {
        List<UserTaskStatus> list = new java.util.ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM user_task_statuses WHERE userId = ? AND date BETWEEN ? AND ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, startDate.toString());
                pstmt.setString(3, endDate.toString());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    UserTaskStatus status = new UserTaskStatus(
                            rs.getString("userId"),
                            rs.getString("taskId"),
                            LocalDate.parse(rs.getString("date")),
                            rs.getInt("isDone") == 1
                    );
                    String ts = rs.getString("completionTimestamp");
                    if (ts != null) {
                        status.setDone(true);
                    }
                    list.add(status);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Task> findTeamTasksByteamID(String teamID) {
        List<Task> list = new java.util.ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT * FROM tasks WHERE teamID = ? AND isTeamTask = 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, teamID);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    List<java.time.DayOfWeek> repeatDays = new java.util.ArrayList<>();
                    String repeatDaysStr = rs.getString("repeatDays");
                    if (repeatDaysStr != null && !repeatDaysStr.isEmpty()) {
                        for (String day : repeatDaysStr.split(",")) {
                            repeatDays.add(java.time.DayOfWeek.valueOf(day));
                        }
                    }
                    Task task = new Task(
                            rs.getString("taskId"),
                            rs.getString("taskName"),
                            rs.getString("description"),
                            rs.getInt("estimatedMinutes"),
                            repeatDays,
                            rs.getInt("isTeamTask") == 1
                    );
                    list.add(task);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}