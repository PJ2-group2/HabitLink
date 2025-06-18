package com.habit.client;

public class ServerApiClient {
    private String serverBaseUrl;
    private Object httpClient;
    private Object objectMapper;
    private String authToken;

    public Object login(String username, String password) {
        // ログインリクエスト送信
        return null;
    }

    public void updateTaskCompletion(String userId, String taskId, java.time.LocalDate date, boolean isDone) {
        // タスク完了状態送信
    }

    public Object getPublicRooms() {
        // 公開ルーム取得
        return null;
    }

    public Object createRoom(String roomName, String roomMode, String creatorId) {
        // ルーム作成
        return null;
    }

    public Object joinRoom(String userId, String roomId) {
        // ルーム参加
        return null;
    }

    public Object getRoomTaskProgress(String roomId) {
        // ルームタスク進捗取得
        return null;
    }

    public Object getSabotageRanking(String roomId) {
        // サボりランキング取得
        return null;
    }

    public void sendMessage(String roomId, String senderId, String content) {
        // チャットメッセージ送信
    }

    public Object getChatHistory(String roomId) {
        // チャット履歴取得
        return null;
    }

    public void triggerPenaltyPopup(String userId) {
        // ペナルティポップアップトリガー
    }
}