package com.habit.server;

import com.habit.domain.User;

public class UserRepository {
    // 実際のDB接続は省略
    public User findById(String userId) {
        // 実装省略
        return null;
    }

    public User findByUsername(String username) {
        // 実装省略
        return null;
    }

    public void save(User user) {
        // 実装省略
    }

    public void updateSabotagePoints(String userId, int points) {
        // 実装省略
    }
}