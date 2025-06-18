package com.habit.server;

import com.habit.domain.Room;
import com.habit.domain.RoomMode;
import com.habit.domain.User;
import java.util.List;

public class RoomService {
    private RoomRepository roomRepository;
    private UserRepository userRepository;

    public RoomService(RoomRepository roomRepository, UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public Room createRoom(String creatorId, String roomName, RoomMode mode) {
        // ルームID生成は省略
        Room room = new Room(roomName, creatorId, mode);
        room.setRoomName(roomName);
        roomRepository.save(room);
        return room;
    }

    public Room joinRoom(String userId, String roomId) {
        Room room = roomRepository.findById(roomId);
        if (room != null) {
            room.addMember(userId);
            roomRepository.addMember(roomId, userId);
        }
        return room;
    }

    public List<Room> getPublicRooms() {
        return roomRepository.findAllPublicRooms();
    }

    public Room getRoomById(String roomId) {
        return roomRepository.findById(roomId);
    }

    public void updateRoomMembers(String roomId, List<User> members) {
        // メンバー更新ロジックは省略
    }
}