package com.habit.server.controller;

import com.habit.server.repository.TaskRepository;
import com.habit.server.repository.UserTaskStatusRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;

/**
 * タスク関連APIのコントローラ
 */
public class TaskController {
  private final TaskRepository taskRepository;
  private final UserTaskStatusRepository utsRepository;

  public TaskController(TaskRepository taskRepository,
                        UserTaskStatusRepository utsRepository) {
    this.taskRepository = taskRepository;
    this.utsRepository = utsRepository;
  }

  // ------------------------------------------------------------------------------
  // GetHandler Methods
  public HttpHandler getSaveTaskHandler() { return this.new SaveTaskHandler(); }

  // --- タスクID→タスク名マップ取得API ---
  public HttpHandler getTaskIdNameMapHandler() {
    return this.new GetTaskIdNameMapHandler();
  }

  // --- チーム内で自分に紐づくタスク取得API ---
  public HttpHandler
  getUserTeamTasksHandler(com.habit.server.service.AuthService authService) {
    return this.new GetUserTeamTasksHandler(authService);
  }

  // ------------------------------------------------------------------------------
  // Handler Classes

  class GetTaskIdNameMapHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      String teamID = null;
      if (query != null && query.startsWith("id=")) {
        teamID = java.net.URLDecoder.decode(query.substring(3), "UTF-8");
      }
      String response;
      if (teamID == null || teamID.isEmpty()) {
        response = "{}";
      } else {
        var repo = TaskController.this.taskRepository;
        java.util.List<com.habit.domain.Task> tasks =
            repo.findTeamTasksByTeamID(teamID);
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < tasks.size(); i++) {
          var t = tasks.get(i);
          sb.append("\"")
              .append(t.getTaskId().replace("\"", "\\\""))
              .append("\":");
          sb.append("\"")
              .append(t.getTaskName().replace("\"", "\\\""))
              .append("\"");
          if (i < tasks.size() - 1)
            sb.append(",");
        }
        sb.append("}");
        response = sb.toString();
      }
      exchange.getResponseHeaders().set("Content-Type",
                                        "application/json; charset=UTF-8");
      exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes("UTF-8"));
      os.close();
    }
  }

  class GetUserTeamTasksHandler implements HttpHandler {
    private final com.habit.server.service.AuthService authService;

    public GetUserTeamTasksHandler(
        com.habit.server.service.AuthService authService) {
      this.authService = authService;
    }

    @Override
    public void handle(com.sun.net.httpserver.HttpExchange exchange)
        throws java.io.IOException {
      String sessionId = null;
      var headers = exchange.getRequestHeaders();
      if (headers.containsKey("SESSION_ID")) {
        sessionId = headers.getFirst("SESSION_ID");
      }
      String query = exchange.getRequestURI().getQuery();
      String teamID = null;
      if (query != null && query.startsWith("teamID=")) {
        teamID = java.net.URLDecoder.decode(query.substring(7), "UTF-8");
      }
      String response = "[]";
      if (sessionId != null && teamID != null) {
        var user = authService.getUserBySession(sessionId);
        if (user != null) {
          String userId = user.getUserId();
          var taskRepo = TaskController.this.taskRepository;
          var utsRepo = TaskController.this.utsRepository;
          java.util.List<String> taskIds =
              utsRepo.findTaskIdsByUserIdAndTeamId(userId, teamID);
          java.util.List<com.habit.domain.Task> teamTasks =
              taskRepo.findTeamTasksByTeamID(teamID);
          java.util.List<com.habit.domain.Task> filtered =
              new java.util.ArrayList<>();
          for (com.habit.domain.Task t : teamTasks) {
            if (taskIds.contains(t.getTaskId())) {
              filtered.add(t);
            }
          }
          StringBuilder sb = new StringBuilder("[");
          for (int i = 0; i < filtered.size(); i++) {
            com.habit.domain.Task t = filtered.get(i);
            sb.append("{");
            sb.append("\"taskId\":\"")
                .append(t.getTaskId().replace("\"", "\\\""))
                .append("\",");
            sb.append("\"taskName\":\"")
                .append(t.getTaskName().replace("\"", "\\\""))
                .append("\"");
            sb.append("}");
            if (i < filtered.size() - 1)
              sb.append(",");
          }
          sb.append("]");
          response = sb.toString();
        }
      }
      exchange.getResponseHeaders().set("Content-Type",
                                        "application/json; charset=UTF-8");
      exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
      java.io.OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes("UTF-8"));
      os.close();
    }
  }

  // --- タスク保存API ---
  class SaveTaskHandler implements com.sun.net.httpserver.HttpHandler {
    @Override
    public void handle(com.sun.net.httpserver.HttpExchange exchange)
        throws java.io.IOException {
      if (!"POST".equals(exchange.getRequestMethod())) {
        String response = "POSTメソッドのみ対応";
        exchange.sendResponseHeaders(405, response.getBytes().length);
        try (java.io.OutputStream os = exchange.getResponseBody()) {
          os.write(response.getBytes());
        }
        return;
      }
      byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
      String bodyStr =
          (bodyBytes != null)
              ? new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8)
              : "";
      String response;
      try {
        // key=value&...形式で受信
        String[] params = bodyStr.split("&");
        java.util.Map<String, String> map = new java.util.HashMap<>();
        for (String param : params) {
          String[] kv = param.split("=", 2);
          if (kv.length < 2)
            continue;
          map.put(kv[0], java.net.URLDecoder.decode(kv[1], "UTF-8"));
        }
        String taskId = map.get("taskId");
        String taskName = map.get("taskName");
        String description = map.getOrDefault("description", "");
        int estimatedMinutes =
            Integer.parseInt(map.getOrDefault("estimatedMinutes", "0"));
        boolean isTeamTask =
            Boolean.parseBoolean(map.getOrDefault("isTeamTask", "false"));
        String dueTimeStr = map.get("dueTime");
        java.time.LocalTime dueTime =
            dueTimeStr != null && !dueTimeStr.isEmpty()
                ? java.time.LocalTime.parse(dueTimeStr)
                : null;
        String dueDateStr = map.get("dueDate");
        java.time.LocalDate dueDate =
            dueDateStr != null && !dueDateStr.isEmpty()
                ? java.time.LocalDate.parse(dueDateStr)
                : null;
        String cycleType = map.getOrDefault("cycleType", "daily");
        String teamID = map.get("teamID");
        // repeatDaysは未対応
        com.habit.domain.Task task = new com.habit.domain.Task(
            taskId, taskName, description, estimatedMinutes,
            java.util.Collections.emptyList(), isTeamTask, dueTime, dueDate,
            cycleType);
        new com.habit.server.repository.TaskRepository().saveTask(task, teamID);
        response = "タスク保存成功";
      } catch (Exception ex) {
        response = "タスク保存失敗: " + ex.getMessage();
      }
      exchange.sendResponseHeaders(200, response.getBytes().length);
      try (java.io.OutputStream os = exchange.getResponseBody()) {
        os.write(response.getBytes());
      }
    }
  }
}
