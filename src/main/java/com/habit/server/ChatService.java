package com.habit.server;

import com.habit.domain.Message;
import com.habit.domain.MessageType;
import java.util.List;

public class ChatService {
    private MessageRepository messageRepository;
    private RoomRepository roomRepository;

    public ChatService(MessageRepository messageRepository, RoomRepository roomRepository) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
    }

    public void sendMessage(String senderId, String roomId, String content) {
        // メッセージID生成は省略
        Message message = new Message(content, senderId, roomId, content, MessageType.NORMAL);
        messageRepository.save(message);
    }

    public List<Message> getChatHistory(String roomId, int limit) {
        return messageRepository.findMessagesByRoomId(roomId, limit);
    }

    public void postPenaltyReport(String userId, String roomId) {
        // メッセージID生成は省略
        Message message = new Message(userId, userId, roomId, "ペナルティ報告", MessageType.PENALTY_REPORT);
        messageRepository.save(message);
    }
}