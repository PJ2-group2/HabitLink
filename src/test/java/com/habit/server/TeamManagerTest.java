package com.habit.server;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class TeamManagerTest {

  @Test
  void createTeamAndRetrieveManager() {
    TeamManager rm = new TeamManager();
    assertTrue(rm.createTeam("team1"));
    assertTrue(rm.teamExists("team1"));
    TaskManager tm = rm.getTaskManager("team1");
    assertNotNull(tm, "Task manager should be returned for created team");
    // retrieving again returns same instance
    assertSame(tm, rm.getTaskManager("team1"));
  }

  @Test
  void createTeamFailsWhenExists() {
    TeamManager rm = new TeamManager();
    assertTrue(rm.createTeam("r"));
    assertFalse(rm.createTeam("r"), "Creating same team should fail");
  }
}
