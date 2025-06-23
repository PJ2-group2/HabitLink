package com.habit.server;

import com.habit.domain.Room;
import java.sql.*;
import java.util.List;

public class RoomRepository {
    private static final String DB_URL = "jdbc:sqlite:habit.db";

    public RoomRepository() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            // まずテーブルがなければ作成
            String sql = "CREATE TABLE IF NOT EXISTS rooms (" +
                    "roomId TEXT PRIMARY KEY," +
                    "roomName TEXT," +
                    "passcode TEXT," +
                    "maxMembers INTEGER," +
                    "editPermission TEXT," +
                    "category TEXT," +
                    "scope TEXT," +
                    "creatorId TEXT" +
                    ")";
            stmt.execute(sql);
            String sql2 = "CREATE TABLE IF NOT EXISTS room_members (" +
                    "roomId TEXT," +
                    "memberId TEXT)";
            stmt.execute(sql2);
            // roomsテーブルのカラム追加（既存DB用）
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(rooms)");
            boolean hasRoomName = false;
            boolean hasScope = false;
            while (rs.next()) {
                String col = rs.getString("name");
                if ("roomName".equalsIgnoreCase(col)) {
                    hasRoomName = true;
                }
                if ("scope".equalsIgnoreCase(col)) {
                    hasScope = true;
                }
            }
            if (!hasRoomName) {
                stmt.execute("ALTER TABLE rooms ADD COLUMN roomName TEXT");
            }
            if (!hasScope) {
                stmt.execute("ALTER TABLE rooms ADD COLUMN scope TEXT");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Room findById(String roomId) {
        // 実装省略
        return null;
    }

    public List<Room> findAllPublicRooms() {
        // 実装省略
        return null;
    }

    // 公開チーム名一覧
    public List<String> findAllPublicTeamNames() {
        List<String> names = new java.util.ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT roomName FROM rooms WHERE scope = 'public'";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    names.add(rs.getString("roomName"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return names;
    }

    // 新しいsave: 追加情報も保存
    public void save(Room room, String passcode, int maxMembers, String editPerm, String category, String scope, List<String> members) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "INSERT OR REPLACE INTO rooms (roomId, roomName, passcode, maxMembers, editPermission, category, scope, creatorId) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, room.getRoomId());
                pstmt.setString(2, room.getRoomName());
                pstmt.setString(3, passcode);
                pstmt.setInt(4, maxMembers);
                pstmt.setString(5, editPerm);
                pstmt.setString(6, category);
                pstmt.setString(7, scope);
                pstmt.setString(8, room.getCreatorId());
                pstmt.executeUpdate();
            }
            // メンバー保存
            String delSql = "DELETE FROM room_members WHERE roomId = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(delSql)) {
                pstmt.setString(1, room.getRoomId());
                pstmt.executeUpdate();
            }
            String insSql = "INSERT INTO room_members (roomId, memberId) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insSql)) {
                for (String member : members) {
                    pstmt.setString(1, room.getRoomId());
                    pstmt.setString(2, member);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void save(Room room) {
        // 実装省略
    }

    public void addMember(String roomId, String userId) {
        // 実装省略
    }

    public void removeMember(String roomId, String userId) {
        // 実装省略
    }
}