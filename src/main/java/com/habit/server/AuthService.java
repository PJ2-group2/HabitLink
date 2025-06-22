package com.habit.server;

import com.habit.domain.User;

public class AuthService {
    private UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user != null && user.authenticate(password)) {
            return user;
        }
        return null;
    }

    public User register(String username, String password) {
        // ユーザーID生成は省略
        User user = new User(username, username, password);
        userRepository.save(user);
        return user;
    }

    public void logout(String userId) {
        // セッション管理等は省略
    }
}