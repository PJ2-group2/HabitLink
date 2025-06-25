package com.habit.client;

import com.habit.domain.User;
import com.habit.domain.Task;
import com.habit.domain.Team;
import com.habit.domain.Message;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientDataManager {
    private User currentUser;
    private Map<LocalDate, List<Task>> dailyTasks = new HashMap<>();
    private Map<String, Team> cachedTeams = new HashMap<>();
    private Map<String, List<Message>> cachedChatHistories = new HashMap<>();

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public List<Task> getTasksForDate(LocalDate date) {
        return dailyTasks.get(date);
    }

    public void updateTaskStatus(String taskId, LocalDate date, boolean isDone) {
        // 実装省略
    }

    public void cacheTeam(Team team) {
        cachedTeams.put(team.getTeamID(), team);
    }

    public Team getCachedTeam(String teamID) {
        return cachedTeams.get(teamID);
    }

    public void addMessageToHistory(String teamID, Message message) {
        cachedChatHistories.computeIfAbsent(teamID, k -> new java.util.ArrayList<>()).add(message);
    }

    public List<Message> getChatHistory(String teamID) {
        return cachedChatHistories.get(teamID);
    }
}