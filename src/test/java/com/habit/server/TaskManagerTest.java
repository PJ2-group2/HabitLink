package com.habit.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TaskManagerTest {

  @Test
  void newManagerHasNoTasks() {
    TaskManager manager = new TaskManager();
    assertTrue(manager.getTasks().isEmpty(),
               "New manager should have no tasks");
  }

  @Test
  void addTaskStoresTask() {
    TaskManager manager = new TaskManager();
    manager.addTask("Task1");
    manager.addTask("Task2");
    assertEquals(2, manager.getTasks().size(), "Two tasks should be stored");
    assertTrue(manager.getTasks().contains("Task1"));
    assertTrue(manager.getTasks().contains("Task2"));
  }

  @Test
  void taskExistsReflectsPresence() {
    TaskManager manager = new TaskManager();
    manager.addTask("Do something");
    assertTrue(manager.taskExists("Do something"));
    assertFalse(manager.taskExists("Missing"));
  }
}
