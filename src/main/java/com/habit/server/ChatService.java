package com.habit.server;

import com.habit.domain.Message;
import com.habit.domain.MessageType;
import java.util.List;

public class ChatService {
    private MessageRepository messageRepository;
    private TeamRepository teamRepository;

    public ChatService(MessageRepository messageRepository, TeamRepository teamRepository) {
        this.messageRepository = messageRepository;
        this.teamRepository = teamRepository;
    }

    public void sendMessage(String senderId, String teamID, String content) {
        // メッセージID生成は省略
        Message message = new Message(content, senderId, teamID, content, MessageType.NORMAL);
        messageRepository.save(message);
    }

    public List<Message> getChatHistory(String teamID, int limit) {
        return messageRepository.findMessagesByteamID(teamID, limit);
    }

    public void postPenaltyReport(String userId, String teamID) {
        // メッセージID生成は省略
        Message message = new Message(userId, userId, teamID, "ペナルティ報告", MessageType.PENALTY_REPORT);
        messageRepository.save(message);
    }
}