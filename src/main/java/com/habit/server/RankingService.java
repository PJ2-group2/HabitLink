package com.habit.server;

import com.habit.domain.User;
import java.util.List;

public class RankingService {
    private UserRepository userRepository;
    private RoomRepository roomRepository;
    private TaskService taskService;

    public RankingService(UserRepository userRepository, RoomRepository roomRepository, TaskService taskService) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.taskService = taskService;
    }

    public List<User> getSabotageRanking(String roomId) {
        // 実装省略
        return null;
    }
}