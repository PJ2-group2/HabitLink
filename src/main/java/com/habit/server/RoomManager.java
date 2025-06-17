package com.habit.server;

import java.util.HashMap;
import java.util.Map;

public class RoomManager {
  private final Map<String, TaskManager> rooms = new HashMap<>();

  public synchronized boolean createRoom(String roomId) {
    if (rooms.containsKey(roomId)) {
      return false;
    }
    rooms.put(roomId, new TaskManager());
    return true;
  }

  public synchronized boolean roomExists(String roomId) {
    return rooms.containsKey(roomId);
  }

  public synchronized TaskManager getTaskManager(String roomId) {
    return rooms.get(roomId);
  }
}
