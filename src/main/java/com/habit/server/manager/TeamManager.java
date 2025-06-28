package com.habit.server.manager;

import java.util.HashMap;
import java.util.Map;

public class TeamManager {
  private final Map<String, TaskManager> teams = new HashMap<>();

  public synchronized boolean createTeam(String teamID) {
    if (teams.containsKey(teamID)) {
      return false;
    }
    teams.put(teamID, new TaskManager());
    return true;
  }

  public synchronized boolean teamExists(String teamID) {
    return teams.containsKey(teamID);
  }

  public synchronized TaskManager getTaskManager(String teamID) {
    return teams.get(teamID);
  }
}
