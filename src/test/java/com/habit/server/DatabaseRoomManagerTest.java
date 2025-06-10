package com.habit.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Tests for the SQLite backed room manager. */
public class DatabaseRoomManagerTest {
  private DatabaseRoomManager mgr =
      new DatabaseRoomManager("jdbc:sqlite::memory:");

  @AfterEach
  void teardown() {
    mgr.close();
  }

  @Test
  void createAndRetrieveRoom() {
    assertTrue(mgr.createRoom("r1"));
    assertTrue(mgr.roomExists("r1"));
    TaskManager tm = mgr.getTaskManager("r1");
    tm.addTask("a");
    assertTrue(tm.taskExists("a"));
  }

  @Test
  void duplicateRoomFails() {
    assertTrue(mgr.createRoom("dup"));
    assertFalse(mgr.createRoom("dup"));
  }
}
