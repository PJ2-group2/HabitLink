package com.habit.domain;

import java.time.DayOfWeek;
import java.util.List;

/**
 * タスク情報を管理するクラス。
 */
import java.time.LocalTime;

public class Task {
    private String taskId;
    private String taskName;
    private String description;
    private int estimatedMinutes;
    private List<DayOfWeek> repeatDays;
    private boolean isTeamTask;
    private LocalTime dueTime;      // 期限時刻
    private String cycleType;       // "daily" or "weekly"

    public Task(String taskId, String taskName) {
        this.taskId = taskId;
        this.taskName = taskName;
    }

    public Task(String taskId, String taskName, String description, int estimatedMinutes, List<DayOfWeek> repeatDays, boolean isTeamTask, LocalTime dueTime, String cycleType) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.description = description;
        this.estimatedMinutes = estimatedMinutes;
        this.repeatDays = repeatDays;
        this.isTeamTask = isTeamTask;
        this.dueTime = dueTime;
        this.cycleType = cycleType;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void updateTaskDetails(String newName, String newDescription, int newEstimatedMinutes, LocalTime newDueTime, String newCycleType) {
        this.taskName = newName;
        this.description = newDescription;
        this.estimatedMinutes = newEstimatedMinutes;
        this.dueTime = newDueTime;
        this.cycleType = newCycleType;
    }

    public String getDescription() {
        return description;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public List<DayOfWeek> getRepeatDays() {
        return repeatDays;
    }

    public boolean isTeamTask() {
        return isTeamTask;
    }

    public LocalTime getDueTime() {
        return dueTime;
    }

    public String getCycleType() {
        return cycleType;
    }
}