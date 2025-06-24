package com.habit.domain;

import java.util.Objects;

/**
 * ユーザー情報を管理するクラス。
 */
public class User {
    private String userId;
    private String username;
    private String hashedPassword;
    private int sabotagePoints;

    public User(String userId, String username, String hashedPassword) {
        this.userId = userId;
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.sabotagePoints = 0;
    }

    public boolean authenticate(String password) {
        // ハッシュ化ロジックは省略（実装時に追加）
        return Objects.equals(this.hashedPassword, password);
    }

    public void addSabotagePoints(int points) {
        this.sabotagePoints += points;
    }

    public int getSabotagePoints() {
        return sabotagePoints;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(String newPassword) {
        this.hashedPassword = newPassword;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }
}