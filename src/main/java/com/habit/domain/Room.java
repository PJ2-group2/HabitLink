package com.habit.domain;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String roomId;
    private String roomName;
    private String creatorId;
    private RoomMode mode;
    private List<String> memberIds;
    private List<Task> teamTasks;

    public Room(String roomId, String creatorId, RoomMode mode) {
        this.roomId = roomId;
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
        if (mode == RoomMode.FIXED_TASK_MODE) {
            teamTasks.add(task);
        }
    }

    public RoomMode getMode() {
        return mode;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public List<Task> getTeamTasks() {
        return teamTasks;
    }
}