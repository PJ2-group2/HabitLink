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
    private String editPermission; // modeから変更
    private List<String> memberIds;
    private List<Task> teamTasks;

    public Team(String teamID, String teamName, String creatorId, String editPermission) {
        this.teamID = teamID;
        this.teamName = teamName;
        this.creatorId = creatorId;
        this.editPermission = editPermission; // modeから変更
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
        // タスク追加ロジックは一旦シンプルにします（必要に応じて復活させます）
        teamTasks.add(task);
    }

    public String getEditPermission() { // getModeから変更
        return editPermission;
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