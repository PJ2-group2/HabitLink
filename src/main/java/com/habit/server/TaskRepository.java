package com.habit.server;

import com.habit.domain.UserTaskStatus;
import com.habit.domain.Task;
import java.time.LocalDate;
import java.util.List;

public class TaskRepository {
    // 実際のDB接続は省略
    public UserTaskStatus findUserTaskStatus(String userId, String taskId, LocalDate date) {
        // 実装省略
        return null;
    }

    public void saveUserTaskStatus(UserTaskStatus status) {
        // 実装省略
    }

    public List<UserTaskStatus> findUserTaskStatusesForPeriod(String userId, LocalDate startDate, LocalDate endDate) {
        // 実装省略
        return null;
    }

    public List<Task> findTeamTasksByRoomId(String roomId) {
        // 実装省略
        return null;
    }
}