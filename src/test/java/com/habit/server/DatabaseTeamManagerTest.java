package com.habit.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.habit.server.manager.DatabaseTeamManager;
import com.habit.server.manager.TaskManager;

/** Tests for the SQLite backed team manager. */
public class DatabaseTeamManagerTest {
  private DatabaseTeamManager mgr =
      new DatabaseTeamManager("jdbc:sqlite::memory:");

  @AfterEach
  void teardown() {
    mgr.close();
  }

  @Test
  void createAndRetrieveTeam() {
    assertTrue(mgr.createTeam("r1"));
    assertTrue(mgr.teamExists("r1"));
    TaskManager tm = mgr.getTaskManager("r1");
    tm.addTask("a");
    assertTrue(tm.taskExists("a"));
  }

  @Test
  void duplicateTeamFails() {
    assertTrue(mgr.createTeam("dup"));
    assertFalse(mgr.createTeam("dup"));
  }
}
