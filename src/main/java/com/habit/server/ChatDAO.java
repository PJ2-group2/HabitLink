package com.habit.server;

import java.util.List;
import com.habit.client.model.ChatMessage;

public class ChatDAO {
    // DB接続などは省略
    public List<ChatMessage> getAllMessages() {
        // DBから全チャットメッセージを取得
        // ...実装...
        return null; // 仮実装
    }
    public void saveMessage(ChatMessage msg) {
        // DBにチャットメッセージを保存
        // ...実装...
    }
}