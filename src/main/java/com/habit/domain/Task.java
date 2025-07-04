package com.habit.domain;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * タスク情報を管理するクラス。
 */

public class Task {
    private String taskId;
    private String taskName;
    private String description;
    private boolean isTeamTask;
    private String teamId;          // チーム共通タスクの場合のチームID
    private LocalTime dueTime;      // 期限時刻
    private LocalDate dueDate;      // 期限日付
    private String cycleType;       // "daily" or "weekly"
    private String originalTaskId;  // 元のTaskID（自動再設定時の関連性を管理）

    public Task(String taskId, String taskName) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.originalTaskId = extractOriginalTaskId(taskId);
    }

    public Task(String taskId, String taskName, String description, boolean isTeamTask, LocalTime dueTime, String cycleType) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.description = description;
        this.isTeamTask = isTeamTask;
        this.teamId = null; // 個人タスクの場合はnull
        this.dueTime = dueTime;
        this.dueDate = null; // デフォルトはnull、後で設定可能
        this.cycleType = cycleType;
        this.originalTaskId = extractOriginalTaskId(taskId);
    }

    // チーム共通タスク用コンストラクタ
    public Task(String taskId, String taskName, String description, boolean isTeamTask, String teamId, LocalTime dueTime, String cycleType) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.description = description;
        this.isTeamTask = isTeamTask;
        this.teamId = teamId;
        this.dueTime = dueTime;
        this.dueDate = null; // デフォルトはnull、後で設定可能
        this.cycleType = cycleType;
        this.originalTaskId = extractOriginalTaskId(taskId);
    }

    // dueDateを含む完全版コンストラクタ
    public Task(String taskId, String taskName, String description, boolean isTeamTask, LocalTime dueTime, LocalDate dueDate, String cycleType) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.description = description;
        this.isTeamTask = isTeamTask;
        this.dueTime = dueTime;
        this.dueDate = dueDate;
        this.cycleType = cycleType;
        this.originalTaskId = extractOriginalTaskId(taskId);
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

    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void updateTaskDetails(String newName, String newDescription, LocalTime newDueTime, String newCycleType) {
        this.taskName = newName;
        this.description = newDescription;
        this.dueTime = newDueTime;
        this.cycleType = newCycleType;
    }

    public void updateTaskDetails(String newName, String newDescription, LocalTime newDueTime, LocalDate newDueDate, String newCycleType) {
        this.taskName = newName;
        this.description = newDescription;
        this.dueTime = newDueTime;
        this.dueDate = newDueDate;
        this.cycleType = newCycleType;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTeamTask() {
        return isTeamTask;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public LocalTime getDueTime() {
        return dueTime;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getCycleType() {
        return cycleType;
    }

    public String getOriginalTaskId() {
        return originalTaskId;
    }

    public void setOriginalTaskId(String originalTaskId) {
        this.originalTaskId = originalTaskId;
    }
}