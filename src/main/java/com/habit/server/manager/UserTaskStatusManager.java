package com.habit.server.manager;

import com.habit.domain.UserTaskStatus;
import com.habit.server.repository.UserTaskStatusRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

public class UserTaskStatusManager {
    private final Map<String, UserTaskStatus> statusMap = new ConcurrentHashMap<>();
    private final UserTaskStatusRepository repository;

    public UserTaskStatusManager(UserTaskStatusRepository repository) {
        this.repository = repository;
        // サーバ起動時に全件ロード
        List<UserTaskStatus> allStatuses = repository.findAll();
        for (UserTaskStatus status : allStatuses) {
            statusMap.put(status.getUserId(), status);
        }
    }

    public UserTaskStatus getStatus(String userId) {
        return statusMap.get(userId);
    }

    public void updateStatus(String userId, UserTaskStatus status) {
        statusMap.put(userId, status);
        repository.save(status);
    }

    public void refreshFromDatabase() {
        List<UserTaskStatus> allStatuses = repository.findAll();
        statusMap.clear();
        for (UserTaskStatus status : allStatuses) {
            statusMap.put(status.getUserId(), status);
        }
    }
}