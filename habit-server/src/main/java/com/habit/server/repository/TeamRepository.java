package com.habit.server.repository;

import com.habit.domain.Team;
import java.sql.*;
import java.util.List;

public class TeamRepository {
  private final String databaseUrl;

  public TeamRepository() { this("jdbc:sqlite:habit.db"); }

  public TeamRepository(String DB_URL) {
    this.databaseUrl = DB_URL;
    try (Connection conn = DriverManager.getConnection(this.databaseUrl);
         Statement stmt = conn.createStatement()) {
      // まずテーブルがなければ作成
      String sql = "CREATE TABLE IF NOT EXISTS teams ("
                   + "id TEXT PRIMARY KEY,"
                   + "teamName TEXT,"
                   + "passcode TEXT,"
                   + "maxMembers INTEGER,"
                   + "editPermission TEXT,"
                   + "creatorId TEXT"
                   + ")";
      stmt.execute(sql);
      String sql2 = "CREATE TABLE IF NOT EXISTS team_members ("
                    + "teamID TEXT,"
                    + "memberId TEXT)";
      stmt.execute(sql2);
      // teamsテーブルのカラム追加（既存DB用）
      ResultSet rs = stmt.executeQuery("PRAGMA table_info(teams)");
      boolean hasteamName = false;
      boolean hasPasscode = false;
      boolean hasMaxMembers = false;
      boolean hasEditPermission = false;
      boolean hasCreatorId = false;
      while (rs.next()) {
        String col = rs.getString("name");
        if ("teamName".equalsIgnoreCase(col)) {
          hasteamName = true;
        }
        if ("passcode".equalsIgnoreCase(col)) {
          hasPasscode = true;
        }
        if ("maxMembers".equalsIgnoreCase(col)) {
          hasMaxMembers = true;
        }
        if ("editPermission".equalsIgnoreCase(col)) {
          hasEditPermission = true;
        }
        if ("creatorId".equalsIgnoreCase(col)) {
          hasCreatorId = true;
        }
      }
      if (!hasteamName) {
        stmt.execute("ALTER TABLE teams ADD COLUMN teamName TEXT");
      }
      if (!hasPasscode) {
        stmt.execute("ALTER TABLE teams ADD COLUMN passcode TEXT");
      }
      if (!hasMaxMembers) {
        stmt.execute("ALTER TABLE teams ADD COLUMN maxMembers INTEGER");
      }
      if (!hasEditPermission) {
        stmt.execute("ALTER TABLE teams ADD COLUMN editPermission TEXT");
      }
      if (!hasCreatorId) {
        stmt.execute("ALTER TABLE teams ADD COLUMN creatorId TEXT");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  // teamNameからteamIDを取得
  public String findTeamIdByName(String teamName) {
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
      String sql = "SELECT id FROM teams WHERE teamName = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, teamName);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
          return rs.getString("id");
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  // 公開チーム名一覧(公開非公開の区別を廃止)
  public List<String> findAllPublicTeamNames() {
    List<String> names = new java.util.ArrayList<>();
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
      String sql = "SELECT teamName FROM teams";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
          names.add(rs.getString("teamName"));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return names;
  }

  // 合言葉でチーム名を検索
  public String findTeamNameByPasscode(String passcode) {
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
      String sql = "SELECT teamName FROM teams WHERE passcode = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, passcode);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
          return rs.getString("teamName");
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  // 合言葉でチームIDを検索
  public String findTeamIdByPasscode(String passcode) {
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
      String sql = "SELECT id FROM teams WHERE passcode = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, passcode);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
          return rs.getString("id");
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  // teamIDからチーム名を取得
  public String findTeamNameById(String teamID) {
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
      String sql = "SELECT teamName FROM teams WHERE id = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, teamID);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
          return rs.getString("teamName");
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  // チーム名でメンバー追加
  public int addMemberByTeamName(String teamName, String memberId) {
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
      // まずteamID取得
      String sql = "SELECT id, maxMembers FROM teams WHERE teamName = ?";
      String teamID = null;
      int maxMembers = 0;
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, teamName);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
          teamID = rs.getString("id");
          maxMembers = rs.getInt("maxMembers");
        }
      }
      if (teamID == null)
        return -1; // チームが存在しない

      // 現在のメンバー数を取得
      String countSql = "SELECT COUNT(memberId) FROM team_members WHERE teamID = ?";
      int currentMembers = 0;
      try (PreparedStatement pstmt = conn.prepareStatement(countSql)) {
          pstmt.setString(1, teamID);
          ResultSet rs = pstmt.executeQuery();
          if (rs.next()) {
              currentMembers = rs.getInt(1);
          }
      }

      // 既にメンバーかチェック
      String checkSql =
          "SELECT 1 FROM team_members WHERE teamID = ? AND memberId = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
        pstmt.setString(1, teamID);
        pstmt.setString(2, memberId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next())
          return 2; // 既にメンバー
      }

      // 上限人数チェック
      if (currentMembers >= maxMembers) {
          return 0; // 上限到達
      }

      // 追加
      String insSql =
          "INSERT INTO team_members (teamID, memberId) VALUES (?, ?)";
      try (PreparedStatement pstmt = conn.prepareStatement(insSql)) {
        pstmt.setString(1, teamID);
        pstmt.setString(2, memberId);
        pstmt.executeUpdate();
      }
      return 1; // 参加成功
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return -1; // エラー
  }

  // 新しいsave: 追加情報も保存
  public void save(Team team, String passcode, int maxMembers, String editPerm,
                   List<String> members) {
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
      String sql = "INSERT OR REPLACE INTO teams (id, teamName, passcode, "
                   + "maxMembers, editPermission, creatorId) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, team.getTeamID());
        pstmt.setString(2, team.getteamName());
        pstmt.setString(3, passcode);
        pstmt.setInt(4, maxMembers);
        pstmt.setString(5, editPerm);
        pstmt.setString(6, team.getCreatorId());
        pstmt.executeUpdate();
      }
      // メンバー保存
      String delSql = "DELETE FROM team_members WHERE teamID = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(delSql)) {
        pstmt.setString(1, team.getTeamID());
        pstmt.executeUpdate();
      }
      // creatorIdも必ずメンバーに含める
      String creatorId = team.getCreatorId();
      List<String> allMembers = new java.util.ArrayList<>();
      if (creatorId != null && !creatorId.isEmpty()) {
        allMembers.add(creatorId);
      }
      for (String member : members) {
        if (member != null && !member.isEmpty() &&
            !allMembers.contains(member)) {
          allMembers.add(member);
        }
      }
      String insSql =
          "INSERT INTO team_members (teamID, memberId) VALUES (?, ?)";
      try (PreparedStatement pstmt = conn.prepareStatement(insSql)) {
        for (String member : allMembers) {
          pstmt.setString(1, team.getTeamID());
          pstmt.setString(2, member);
          pstmt.executeUpdate();
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
 }
    
    /**
     * 全チームID一覧を取得（タスク自動再設定用）
     *
     * @return 全チームのID一覧
     *
     * 【用途】
     * TaskAutoResetServiceから呼び出され、
     * 全チームのタスクを自動再設定する際に使用される
     *
     * 【取得対象】
     * teamsテーブルの全レコードのidカラム
     */
    public List<String> findAllTeamIds() {
        List<String> teamIds = new java.util.ArrayList<>();
        try (Connection conn = DriverManager.getConnection(databaseUrl)) {
            String sql = "SELECT id FROM teams";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    teamIds.add(rs.getString("id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teamIds;
    }

  // 指定チームIDの全メンバーID一覧を返す
  public List<String> findMemberIdsByTeamId(String teamId) {
    List<String> memberIds = new java.util.ArrayList<>();
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
      // team_membersから取得
      String sql = "SELECT memberId FROM team_members WHERE teamID = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, teamId);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
          memberIds.add(rs.getString("memberId"));
        }
      }
      // teamsテーブルのcreatorIdも追加（重複しない場合のみ）
      String creatorSql = "SELECT creatorId FROM teams WHERE id = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(creatorSql)) {
        pstmt.setString(1, teamId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
          String creatorId = rs.getString("creatorId");
          if (creatorId != null && !creatorId.isEmpty() &&
              !memberIds.contains(creatorId)) {
            memberIds.add(0, creatorId); // 先頭に追加（任意）
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return memberIds;
  }

  // チームIDでチーム情報を取得
  public Team findById(String teamId) {
    try (Connection conn = DriverManager.getConnection(databaseUrl)) {
      String sql = "SELECT * FROM teams WHERE id = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, teamId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
          Team team = new Team(
            rs.getString("id"),
            rs.getString("teamName"),
            rs.getString("creatorId"),
            rs.getString("editPermission")
          );
          
          // メンバー一覧を取得して設定
          List<String> memberIds = findMemberIdsByTeamId(teamId);
          for (String memberId : memberIds) {
            team.addMember(memberId);
          }
          
          return team;
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  /*
   * 指定されたチームIDのチームを削除する。
   * 関連するデータも全て削除される。
   */
  public void delete(String teamId) {
    Connection conn = null;
    try {
        conn = DriverManager.getConnection(databaseUrl);
        conn.setAutoCommit(false);

        // 関連データの削除
        // user_task_status
        String delUserTaskStatusSql = "DELETE FROM user_task_statuses WHERE teamId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(delUserTaskStatusSql)) {
            pstmt.setString(1, teamId);
            pstmt.executeUpdate();
        }

        // tasks
        String delTasksSql = "DELETE FROM tasks WHERE teamID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(delTasksSql)) {
            pstmt.setString(1, teamId);
            pstmt.executeUpdate();
        }

        // messages
        String delMessagesSql = "DELETE FROM messages WHERE team_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(delMessagesSql)) {
            pstmt.setString(1, teamId);
            pstmt.executeUpdate();
        }

        // team_members
        String delMembersSql = "DELETE FROM team_members WHERE teamID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(delMembersSql)) {
            pstmt.setString(1, teamId);
            pstmt.executeUpdate();
        }

        // teams
        String delTeamSql = "DELETE FROM teams WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(delTeamSql)) {
            pstmt.setString(1, teamId);
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
