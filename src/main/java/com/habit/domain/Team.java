package com.habit.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * チャットチームやチームの情報を管理するクラス。
 */
public class Team {
    private String teamID;
    private String teamName;
    private String creatorId;
    private TeamMode mode;
    private List<String> memberIds;
    private List<Task> teamTasks;

    public Team(String teamID, String creatorId, TeamMode mode) {
        this.teamID = teamID;
        this.creatorId = creatorId;
        this.mode = mode;
        this.memberIds = new ArrayList<>();
        this.teamTasks = new ArrayList<>();
    }

    public void addMember(String userId) {
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
        }
    }

    public void removeMember(String userId) {
        memberIds.remove(userId);
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void addTeamTask(Task task) {
        if (mode == TeamMode.FIXED_TASK_MODE) {
            teamTasks.add(task);
        }
    }

    public TeamMode getMode() {
        return mode;
    }

    public String getTeamID() {
        return teamID;
    }

    public String getteamName() {
        return teamName;
    }

    public void setteamName(String teamName) {
        this.teamName = teamName;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public List<Task> getTeamTasks() {
        return teamTasks;
    }
}