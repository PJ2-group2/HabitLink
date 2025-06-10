package com.habit.server;

import java.util.HashSet;
import java.util.Set;

public class RoomManager {
  private final Set<String> rooms = new HashSet<>();

  public synchronized boolean createRoom(String roomId) {
    if (rooms.contains(roomId)) {
      return false;
    }
    rooms.add(roomId);
    return true;
  }

  public synchronized boolean joinRoom(String roomId) {
    return rooms.contains(roomId);
  }

  public synchronized boolean roomExists(String roomId) {
    return rooms.contains(roomId);
  }
}
