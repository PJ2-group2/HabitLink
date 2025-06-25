package com.habit.server;

import com.habit.domain.Team;
import java.sql.*;
import java.util.List;

public class TeamRepository {
    private static final String DB_URL = "jdbc:sqlite:habit.db";

    public TeamRepository() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            // まずテーブルがなければ作成
            String sql = "CREATE TABLE IF NOT EXISTS teams (" +
                    "id TEXT PRIMARY KEY," +
                    "teamName TEXT," +
                    "passcode TEXT," +
                    "maxMembers INTEGER," +
                    "editPermission TEXT," +
                    "category TEXT," +
                    "scope TEXT," +
                    "creatorId TEXT" +
                    ")";
            stmt.execute(sql);
            String sql2 = "CREATE TABLE IF NOT EXISTS team_members (" +
                    "teamID TEXT," +
                    "memberId TEXT)";
            stmt.execute(sql2);
            // teamsテーブルのカラム追加（既存DB用）
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(teams)");
            boolean hasteamName = false;
            boolean hasScope = false;
            boolean hasPasscode = false;
            boolean hasMaxMembers = false;
            boolean hasEditPermission = false;
            boolean hasCategory = false;
            boolean hasCreatorId = false;
            while (rs.next()) {
                String col = rs.getString("name");
                if ("teamName".equalsIgnoreCase(col)) {
                    hasteamName = true;
                }
                if ("scope".equalsIgnoreCase(col)) {
                    hasScope = true;
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
                if ("category".equalsIgnoreCase(col)) {
                    hasCategory = true;
                }
                if ("creatorId".equalsIgnoreCase(col)) {
                    hasCreatorId = true;
                }
            }
            if (!hasteamName) {
                stmt.execute("ALTER TABLE teams ADD COLUMN teamName TEXT");
            }
            if (!hasScope) {
                stmt.execute("ALTER TABLE teams ADD COLUMN scope TEXT");
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
            if (!hasCategory) {
                stmt.execute("ALTER TABLE teams ADD COLUMN category TEXT");
            }
            if (!hasCreatorId) {
                stmt.execute("ALTER TABLE teams ADD COLUMN creatorId TEXT");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Team findById(String teamID) {
        // 実装省略
        return null;
    }

    public List<Team> findAllPublicTeams() {
        // 実装省略
        return null;
    }

    // 公開チーム名一覧
    public List<String> findAllPublicTeamNames() {
        List<String> names = new java.util.ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT teamName FROM teams WHERE scope = 'public'";
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
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
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

    // teamIDからチーム名を取得
    public String findTeamNameById(String teamID) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
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
    public boolean addMemberByTeamName(String teamName, String memberId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // まずteamID取得
            String sql = "SELECT id FROM teams WHERE teamName = ?";
            String teamID = null;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, teamName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    teamID = rs.getString("id");
                }
            }
            if (teamID == null) return false;
            // 既にメンバーかチェック
            String checkSql = "SELECT 1 FROM team_members WHERE teamID = ? AND memberId = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setString(1, teamID);
                pstmt.setString(2, memberId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) return true; // 既にメンバー
            }
            // 追加
            String insSql = "INSERT INTO team_members (teamID, memberId) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insSql)) {
                pstmt.setString(1, teamID);
                pstmt.setString(2, memberId);
                pstmt.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 新しいsave: 追加情報も保存
    public void save(Team team, String passcode, int maxMembers, String editPerm, String category, String scope, List<String> members) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "INSERT OR REPLACE INTO teams (id, teamName, passcode, maxMembers, editPermission, category, scope, creatorId) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, team.getTeamID());
                pstmt.setString(2, team.getteamName());
                pstmt.setString(3, passcode);
                pstmt.setInt(4, maxMembers);
                pstmt.setString(5, editPerm);
                pstmt.setString(6, category);
                pstmt.setString(7, scope);
                pstmt.setString(8, team.getCreatorId());
                pstmt.executeUpdate();
            }
            // メンバー保存
            String delSql = "DELETE FROM team_members WHERE teamID = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(delSql)) {
                pstmt.setString(1, team.getTeamID());
                pstmt.executeUpdate();
            }
            String insSql = "INSERT INTO team_members (teamID, memberId) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insSql)) {
                for (String member : members) {
                    pstmt.setString(1, team.getTeamID());
                    pstmt.setString(2, member);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void save(Team team) {
        // 実装省略
    }

    public void addMember(String teamID, String userId) {
        // 実装省略
    }

    public void removeMember(String teamID, String userId) {
        // 実装省略
    }
}