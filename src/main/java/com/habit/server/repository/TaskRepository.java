package com.habit.server.repository;

import com.habit.domain.Task;
import com.habit.domain.UserTaskStatus;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class TaskRepository {
  private final String databaseUrl;
  private static final String DB_URL = "jdbc:sqlite:habit.db";

  public TaskRepository() {
    this(DB_URL);
  }

  public TaskRepository(String databaseUrl) {
    this.databaseUrl = databaseUrl;
    try (Connection conn = DriverManager.getConnection(databaseUrl);
         Statement stmt = conn.createStatement()) {
      // タスクテーブル
      String sql = "CREATE TABLE IF NOT EXISTS tasks ("
                   + "taskId TEXT PRIMARY KEY,"
                   + "taskName TEXT,"
                   + "description TEXT,"
                   + "estimatedMinutes INTEGER,"
                   + "repeatDays TEXT," // カンマ区切り
                   + "isTeamTask INTEGER,"
                   + "teamID TEXT,"
                   + "dueTime TEXT,"
                   + "dueDate TEXT,"
                   + "cycleType TEXT"
                   + ")";
      stmt.execute(sql);

      // 既存DB用: カラム追加
      ResultSet rs = stmt.executeQuery("PRAGMA table_info(tasks)");
      boolean hasTaskId = false;
      boolean hasDueTime = false;
      boolean hasDueDate = false;
      boolean hasCycleType = false;
      while (rs.next()) {
        String col = rs.getString("name");
        if ("taskId".equalsIgnoreCase(col))
          hasTaskId = true;
        if ("dueTime".equalsIgnoreCase(col))
          hasDueTime = true;
        if ("dueDate".equalsIgnoreCase(col))
          hasDueDate = true;
        if ("cycleType".equalsIgnoreCase(col))
          hasCycleType = true;
      }
      if (!hasTaskId) {
        stmt.execute("ALTER TABLE tasks ADD COLUMN taskId TEXT");
      }
      if (!hasDueTime) {
        stmt.execute("ALTER TABLE tasks ADD COLUMN dueTime TEXT");
      }
      if (!hasDueDate) {
        stmt.execute("ALTER TABLE tasks ADD COLUMN dueDate TEXT");
      }
      if (!hasCycleType) {
        stmt.execute("ALTER TABLE tasks ADD COLUMN cycleType TEXT");
      }
      // カラム名「task」が存在し「taskName」がない場合はリネーム
      ResultSet rs2 = stmt.executeQuery("PRAGMA table_info(tasks)");
      boolean hasTask = false;
      boolean hasTaskName = false;
      while (rs2.next()) {
        String col = rs2.getString("name");
        if ("task".equalsIgnoreCase(col))
          hasTask = true;
        if ("taskName".equalsIgnoreCase(col))
          hasTaskName = true;
      }
      if (hasTask && !hasTaskName) {
        // SQLiteは直接カラム名変更できないため、手動で対応が必要
        System.out.println("注意: tasksテーブルのカラム 'task' を 'taskName' "
                           + "にリネームしてください。");
      }

      // ユーザーごとのタスク達成状況
      String sql2 = "CREATE TABLE IF NOT EXISTS user_task_statuses ("
                    + "userId TEXT,"
                    + "taskId TEXT,"
                    + "originalTaskId TEXT,"
                    + "date TEXT,"
                    + "isDone INTEGER,"
                    + "completionTimestamp TEXT,"
                    + "PRIMARY KEY(userId, taskId, date)"
                    + ")";
      stmt.execute(sql2);
      
      // 既存テーブルにoriginalTaskIdカラムを追加（存在しない場合）
      ResultSet rs3 = stmt.executeQuery("PRAGMA table_info(user_task_statuses)");
      boolean hasOriginalTaskId = false;
      while (rs3.next()) {
        String col = rs3.getString("name");
        if ("originalTaskId".equalsIgnoreCase(col))
          hasOriginalTaskId = true;
      }
      if (!hasOriginalTaskId) {
        stmt.execute("ALTER TABLE user_task_statuses ADD COLUMN originalTaskId TEXT");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public UserTaskStatus findUserTaskStatus(String userId, String taskId,
                                           LocalDate date) {
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
      String sql = "SELECT * FROM user_task_statuses WHERE userId = ? AND "
                   + "taskId = ? AND date = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, userId);
        pstmt.setString(2, taskId);
        pstmt.setString(3, date.toString());
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
          UserTaskStatus status = new UserTaskStatus(
              rs.getString("userId"), rs.getString("taskId"),
              LocalDate.parse(rs.getString("date")), rs.getInt("isDone") == 1);
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
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
      String sql =
          "INSERT OR REPLACE INTO user_task_statuses (userId, taskId, date, "
          + "isDone, completionTimestamp) VALUES (?, ?, ?, ?, ?)";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, status.getUserId());
        pstmt.setString(2, status.getTaskId());
        pstmt.setString(3, status.getDate().toString());
        pstmt.setInt(4, status.isDone() ? 1 : 0);
        pstmt.setString(5, status.getCompletionTimestamp() != null
                               ? status.getCompletionTimestamp().toString()
                               : null);
        pstmt.executeUpdate();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public List<UserTaskStatus> findUserTaskStatusesForPeriod(String userId,
                                                            LocalDate startDate,
                                                            LocalDate endDate) {
    List<UserTaskStatus> list = new java.util.ArrayList<>();
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
      String sql = "SELECT * FROM user_task_statuses WHERE userId = ? AND "
                   + "date BETWEEN ? AND ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, userId);
        pstmt.setString(2, startDate.toString());
        pstmt.setString(3, endDate.toString());
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
          UserTaskStatus status = new UserTaskStatus(
              rs.getString("userId"), rs.getString("taskId"),
              LocalDate.parse(rs.getString("date")), rs.getInt("isDone") == 1);
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

  public List<Task> findTeamTasksByTeamID(String teamID) {
    List<Task> list = new java.util.ArrayList<>();
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
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
              rs.getString("taskId"), rs.getString("taskName"),
              rs.getString("description"), rs.getInt("estimatedMinutes"),
              repeatDays, rs.getInt("isTeamTask") == 1,
              rs.getString("dueTime") != null
                  ? java.time.LocalTime.parse(rs.getString("dueTime"))
                  : null,
              rs.getString("dueDate") != null
                  ? java.time.LocalDate.parse(rs.getString("dueDate"))
                  : null,
              rs.getString("cycleType"));
          list.add(task);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return list;
  }

  // タスク保存
  public void saveTask(Task task, String teamID) {
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
      String sql = "INSERT OR REPLACE INTO tasks (taskId, taskName, description, estimatedMinutes, repeatDays, isTeamTask, teamID, dueTime, dueDate, cycleType) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, task.getTaskId());
        pstmt.setString(2, task.getTaskName());
        pstmt.setString(3, task.getDescription());
        pstmt.setInt(4, task.getEstimatedMinutes());
        // repeatDaysはカンマ区切り
        String repeatDaysStr = "";
        if (task.getRepeatDays() != null && !task.getRepeatDays().isEmpty()) {
          repeatDaysStr = String.join(",",
            task.getRepeatDays().stream().map(java.time.DayOfWeek::name).toArray(String[]::new));
        }
        pstmt.setString(5, repeatDaysStr);
        pstmt.setInt(6, task.isTeamTask() ? 1 : 0);
        pstmt.setString(7, teamID);
        pstmt.setString(8, task.getDueTime() != null ? task.getDueTime().toString() : null);
        pstmt.setString(9, task.getDueDate() != null ? task.getDueDate().toString() : null);
        pstmt.setString(10, task.getCycleType());
        pstmt.executeUpdate();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
