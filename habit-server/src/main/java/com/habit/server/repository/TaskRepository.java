package com.habit.server.repository;

import com.habit.domain.Task;
import com.habit.domain.UserTaskStatus;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskRepository {
  private static final Logger logger = LoggerFactory.getLogger(TaskRepository.class);
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
                   + "teamID TEXT,"
                   + "dueDate TEXT,"
                   + "cycleType TEXT"
                   + ")";
      stmt.execute(sql);

      // 既存DB用: カラム追加
      ResultSet rs = stmt.executeQuery("PRAGMA table_info(tasks)");
      boolean hasTaskId = false;
      boolean hasDueDate = false;
      boolean hasCycleType = false;
      while (rs.next()) {
        String col = rs.getString("name");
        if ("taskId".equalsIgnoreCase(col))
          hasTaskId = true;
        if ("dueDate".equalsIgnoreCase(col))
          hasDueDate = true;
        if ("cycleType".equalsIgnoreCase(col))
          hasCycleType = true;
      }
      if (!hasTaskId) {
        stmt.execute("ALTER TABLE tasks ADD COLUMN taskId TEXT");
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
        logger.warn("注意: tasksテーブルのカラム 'task' を 'taskName' にリネームしてください。");
      }

      // ユーザーごとのタスク達成状況
      String sql2 = "CREATE TABLE IF NOT EXISTS user_task_statuses ("
                    + "userId TEXT,"
                    + "taskId TEXT,"
                    + "date TEXT,"
                    + "isDone INTEGER,"
                    + "completionTimestamp TEXT,"
                    + "PRIMARY KEY(userId, taskId, date)"
                    + ")";
      stmt.execute(sql2);
      
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
      String sql = "SELECT * FROM tasks WHERE teamID = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, teamID);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
          Task task = new Task(
              rs.getString("taskId"), rs.getString("taskName"),
              rs.getString("description"),
              rs.getString("teamID"),
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
      String sql = "INSERT OR REPLACE INTO tasks (taskId, taskName, description, teamID, dueDate, cycleType) VALUES  (?, ?, ?, ?, ?, ?)";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, task.getTaskId());
        pstmt.setString(2, task.getTaskName());
        pstmt.setString(3, task.getDescription());
        pstmt.setString(4, teamID);
        pstmt.setString(5, task.getDueDate() != null ? task.getDueDate().toString() : null);
        pstmt.setString(6, task.getCycleType());
        pstmt.executeUpdate();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  // Task保存（簡単版）
  public Task save(Task task) {
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
      String sql = "INSERT OR REPLACE INTO tasks (taskId, taskName, description, teamID, dueDate, cycleType) VALUES  (?, ?, ?, ?, ?, ?)";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, task.getTaskId());
        pstmt.setString(2, task.getTaskName());
        pstmt.setString(3, task.getDescription());
        pstmt.setString(4, task.getTeamId());
        pstmt.setString(5, task.getDueDate() != null ? task.getDueDate().toString() : null);
        pstmt.setString(6, task.getCycleType());
        pstmt.executeUpdate();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return task;
  }

  // チームIDでタスク一覧を取得
  public List<Task> findByTeamId(String teamId) {
    List<Task> list = new java.util.ArrayList<>();
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
      String sql = "SELECT * FROM tasks WHERE teamID = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, teamId);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
          Task task = new Task(
              rs.getString("taskId"), rs.getString("taskName"),
              rs.getString("description"), 
              rs.getString("teamID"),
              rs.getString("cycleType"));
          
          // dueDateがある場合は設定
          if (rs.getString("dueDate") != null) {
            task.setDueDate(java.time.LocalDate.parse(rs.getString("dueDate")));
          }
          
          list.add(task);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return list;
  }

  /*
   * タスクIDでタスクを取得
   */
  public Task findById(String taskId) {
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
      String sql = "SELECT * FROM tasks WHERE taskId = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, taskId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
          Task task = new Task(
              rs.getString("taskId"), rs.getString("taskName"),
              rs.getString("description"), 
              rs.getString("teamID"),
              rs.getString("dueDate") != null
                  ? java.time.LocalDate.parse(rs.getString("dueDate"))
                  : null,
              rs.getString("cycleType"));
          return task;
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * タスクIDでタスクを削除する
   * @param taskId
   */
  public void deleteById(String taskId) {
    Connection conn = null;
    try {
        conn = DriverManager.getConnection(databaseUrl);
        conn.setAutoCommit(false);

        // user_task_statusesテーブルから関連レコードを削除
        String delUserTaskStatusSql = "DELETE FROM user_task_statuses WHERE taskId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(delUserTaskStatusSql)) {
            pstmt.setString(1, taskId);
            pstmt.executeUpdate();
        }

        // tasksテーブルからタスクを削除
        String delTaskSql = "DELETE FROM tasks WHERE taskId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(delTaskSql)) {
            pstmt.setString(1, taskId);
            pstmt.executeUpdate();
        }

        conn.commit();
    } catch (SQLException e) {
        e.printStackTrace();
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    } finally {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
  }
}
