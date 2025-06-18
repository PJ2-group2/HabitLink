package com.habit.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TaskCharacter {
    private int level;
    private int taskForgetCount;
    private Map<Integer, String> levelToImagePath = new HashMap<>();
    private Map<Integer, List<String>> levelToMessages = new HashMap<>();

    public void updateLevel(boolean taskCompleted) {
        if (taskCompleted) {
            level++;
        } else {
            taskForgetCount++;
            if (taskForgetCount > 3 && level > 0) {
                level--;
                taskForgetCount = 0;
            }
        }
    }

    public String getCurrentImagePath() {
        return levelToImagePath.getOrDefault(level, "");
    }

    public String getRandomMessage() {
        List<String> messages = levelToMessages.get(level);
        if (messages == null || messages.isEmpty()) return "";
        return messages.get(new Random().nextInt(messages.size()));
    }

    public void setLevelToImagePath(Map<Integer, String> map) {
        this.levelToImagePath = map;
    }

    public void setLevelToMessages(Map<Integer, List<String>> map) {
        this.levelToMessages = map;
    }
}