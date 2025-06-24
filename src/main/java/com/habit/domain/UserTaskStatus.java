package com.habit.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ユーザーごとのタスク達成状況を管理するクラス。
 */
public class UserTaskStatus {
    private String userId;
    private String taskId;
    private LocalDate date;
    private boolean isDone;
    private LocalDateTime completionTimestamp;

    public UserTaskStatus(String userId, String taskId, LocalDate date, boolean isDone) {
        this.userId = userId;
        this.taskId = taskId;
        this.date = date;
        this.isDone = isDone;
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
}