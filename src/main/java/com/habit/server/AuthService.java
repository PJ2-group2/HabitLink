package com.habit.server;

import com.habit.domain.User;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private UserRepository userRepository;
    // セッションIDとユーザーIDのマッピング
    private final Map<String, String> sessionMap = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * ログイン認証し、成功時はセッションIDを発行して返す
     */
    public String loginAndCreateSession(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user != null && user.authenticate(password)) {
            String sessionId = UUID.randomUUID().toString();
            sessionMap.put(sessionId, user.getUserId());
            return sessionId;
        }
        return null;
    }

    /**
     * セッションIDからユーザーを取得
     */
    public User getUserBySession(String sessionId) {
        String userId = sessionMap.get(sessionId);
        if (userId == null) return null;
        return userRepository.findById(userId);
    }

    /**
     * 新規登録
     */
    public User register(String username, String password) {
        // ユーザーID生成は省略
        User user = new User(username, username, password);
        userRepository.save(user);
        return user;
    }

    /**
     * ログアウト（セッション破棄）
     */
    public void logout(String sessionId) {
        sessionMap.remove(sessionId);
    }
}