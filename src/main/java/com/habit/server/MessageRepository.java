package com.habit.server;

import com.habit.domain.Message;
import java.util.List;

public class MessageRepository {
    // 実際のDB接続は省略
    public void save(Message message) {
        // 実装省略
    }

    public List<Message> findMessagesByRoomId(String roomId, int limit) {
        // 実装省略
        return null;
    }
}