package com.habit.server;

import com.habit.domain.Task;
import com.habit.domain.UserTaskStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class TaskService {
    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private RoomRepository roomRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository, RoomRepository roomRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
    }

    public void updateTaskCompletion(String userId, String taskId, LocalDate date, boolean isDone) {
        UserTaskStatus status = taskRepository.findUserTaskStatus(userId, taskId, date);
        if (status == null) {
            status = new UserTaskStatus(userId, taskId, date, isDone);
        } else {
            status.setDone(isDone);
        }
        taskRepository.saveUserTaskStatus(status);
    }

    public Map<String, Integer> getRoomTaskProgress(String roomId) {
        // 実装省略
        return null;
    }

    public void applyPenalty(String userId, String roomId, Task task) {
        // 実装省略
    }

    public int calculateSabotagePoints(String userId, String roomId) {
        // 実装省略
        return 0;
    }

    public List<Task> getTeamTasks(String roomId) {
        return taskRepository.findTeamTasksByRoomId(roomId);
    }
}