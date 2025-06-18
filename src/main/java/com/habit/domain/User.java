package com.habit.domain;

import java.util.Objects;

public class User {
    private String userId;
    private String username;
    private String hashedPassword;
    private int sabotagePoints;
    private String profileIconPath;
    private String bio;

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

    public String getProfileIconPath() {
        return profileIconPath;
    }

    public void setProfileIconPath(String profileIconPath) {
        this.profileIconPath = profileIconPath;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}