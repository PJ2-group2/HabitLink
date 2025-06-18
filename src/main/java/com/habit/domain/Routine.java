package com.habit.domain;

import java.util.ArrayList;
import java.util.List;

public class Routine {
    private String routineId;
    private String routineName;
    private String description;
    private String creatorId;
    private List<Task> tasks;
    private boolean isPublic;

    public Routine(String routineId, String routineName, String creatorId, List<Task> tasks) {
        this.routineId = routineId;
        this.routineName = routineName;
        this.creatorId = creatorId;
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.isPublic = false;
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void removeTask(String taskId) {
        tasks.removeIf(t -> t.getTaskId().equals(taskId));
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public String getRoutineId() {
        return routineId;
    }

    public String getRoutineName() {
        return routineName;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public boolean isPublic() {
        return isPublic;
    }
}