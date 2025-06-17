package com.habit.server;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {
  private List<String> tasks = new ArrayList<>();

  public synchronized void addTask(String task) { tasks.add(task); }

  public synchronized List<String> getTasks() { return tasks; }

  public synchronized boolean taskExists(String task) {
    return tasks.contains(task);
  }
}
