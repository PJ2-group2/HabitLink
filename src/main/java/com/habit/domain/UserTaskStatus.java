package com.habit.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ユーザーごとのタスク達成状況を管理するクラス。
 */
public class UserTaskStatus {
    private String userId;
    private String taskId;
    private String originalTaskId; // 元のTaskID（自動再設定時の関連性を管理）
    private String teamId; // チーム共通タスクの場合のチームID（個人タスクの場合はnull）
    private LocalDate date;
    private boolean isDone;
    private LocalDateTime completionTimestamp;
    private int progress; // 進捗率（0〜100）
    private String comment; // 任意コメント

    public UserTaskStatus(String userId, String taskId, LocalDate date, boolean isDone) {
        this.userId = userId;
        this.taskId = taskId;
        this.originalTaskId = extractOriginalTaskId(taskId); // TaskIDから元のIDを抽出
        this.teamId = null; // 個人タスクの場合はnull
        this.date = date;
        this.isDone = isDone;
        this.progress = 0;
        this.comment = "";
    }

    // チーム共通タスク用コンストラクタ
    public UserTaskStatus(String userId, String taskId, String teamId, LocalDate date, boolean isDone) {
        this.userId = userId;
        this.taskId = taskId;
        this.originalTaskId = extractOriginalTaskId(taskId); // TaskIDから元のIDを抽出
        this.teamId = teamId;
        this.date = date;
        this.isDone = isDone;
        this.progress = 0;
        this.comment = "";
    }
    
    /**
     * TaskIDから元のTaskIDを抽出
     *
     * @param taskId 現在のTaskID
     * @return 元のTaskID
     */
    private String extractOriginalTaskId(String taskId) {
        if (taskId.contains("_")) {
            // 自動生成されたTaskIDの場合（例: "dailyTask_20250630"）
            return taskId.substring(0, taskId.indexOf("_"));
        }
        // 元のTaskIDの場合はそのまま返す
        return taskId;
    }

    public void setDone(boolean done) {
        this.isDone = done;
        if (done) {
            this.completionTimestamp = LocalDateTime.now();
        } else {
            this.completionTimestamp = null;
        }
    }

    public boolean isDone() {
        return isDone;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        if (progress < 0) progress = 0;
        if (progress > 100) progress = 100;
        this.progress = progress;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getUserId() {
        return userId;
    }

    public String getTaskId() {
        return taskId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalDateTime getCompletionTimestamp() {
        return completionTimestamp;
    }
    
    public String getOriginalTaskId() {
        return originalTaskId;
    }
    
    public void setOriginalTaskId(String originalTaskId) {
        this.originalTaskId = originalTaskId;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }
}