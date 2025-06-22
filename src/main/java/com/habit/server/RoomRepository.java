package com.habit.server;

import com.habit.domain.Room;
import java.util.List;

public class RoomRepository {
    // 実際のDB接続は省略
    public Room findById(String roomId) {
        // 実装省略
        return null;
    }

    public List<Room> findAllPublicRooms() {
        // 実装省略
        return null;
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