package com.habit.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskManager {
  private final Map<String, List<String>> roomTasks = new HashMap<>();

  public synchronized void addTask(String roomId, String task) {
    roomTasks.putIfAbsent(roomId, new ArrayList<>());
    roomTasks.get(roomId).add(task);
  }

  public synchronized List<String> getTasks(String roomId) {
    return new ArrayList<>(
        roomTasks.getOrDefault(roomId, Collections.emptyList()));
  }
}
