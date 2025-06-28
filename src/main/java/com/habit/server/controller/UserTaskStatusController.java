package com.habit.server.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.habit.server.AuthService;
import com.habit.server.UserTaskStatusRepository;
import com.habit.domain.UserTaskStatus;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class UserTaskStatusController {
    private final AuthService authService;
    private final UserTaskStatusRepository userTaskStatusRepository;

    public UserTaskStatusController(AuthService authService, UserTaskStatusRepository userTaskStatusRepository) {
        this.authService = authService;
        this.userTaskStatusRepository = userTaskStatusRepository;
    }

    public HttpHandler getGetUserTaskIdsHandler() {
        return new GetUserTaskIdsHandler();
    }

    private class GetUserTaskIdsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String sessionId = null;
            var headers = exchange.getRequestHeaders();
            if (headers.containsKey("SESSION_ID")) {
                sessionId = headers.getFirst("SESSION_ID");
            }
            String response = "taskIds=";
            if (sessionId != null) {
                var user = authService.getUserBySession(sessionId);
                if (user != null) {
                    String userId = user.getUserId();
                    List<UserTaskStatus> statusList = userTaskStatusRepository.findByUserId(userId);
                    Set<String> taskIds = new HashSet<>();
                    for (UserTaskStatus status : statusList) {
                        taskIds.add(status.getTaskId());
                    }
                    response = "taskIds=" + String.join(",", taskIds);
                }
            }
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
    // --- ユーザーの未完了タスク一覧取得API ---
    public HttpHandler getUserIncompleteTasksHandler(com.habit.server.AuthService authService) {
        return new GetUserIncompleteTasksHandler(authService);
    }

    public static class GetUserIncompleteTasksHandler implements com.sun.net.httpserver.HttpHandler {
        private final com.habit.server.AuthService authService;

        public GetUserIncompleteTasksHandler(com.habit.server.AuthService authService) {
            this.authService = authService;
        }

        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
            java.io.OutputStream os = null;
            try {
                String sessionId = null;
                var headers = exchange.getRequestHeaders();
                if (headers.containsKey("SESSION_ID")) {
                    sessionId = headers.getFirst("SESSION_ID");
                }
                String query = exchange.getRequestURI().getQuery();
                String teamID = null;
                String dateStr = null;
                if (query != null) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("teamID=")) teamID = java.net.URLDecoder.decode(param.substring(7), "UTF-8");
                        if (param.startsWith("date=")) dateStr = java.net.URLDecoder.decode(param.substring(5), "UTF-8");
                    }
                }
                String response = "[]";
                if (sessionId != null && teamID != null && dateStr != null) {
                    var user = authService.getUserBySession(sessionId);
                    if (user != null) {
                        String userId = user.getUserId();
                        com.habit.server.UserTaskStatusRepository utsRepo = new com.habit.server.UserTaskStatusRepository();
                        com.habit.server.TaskRepository taskRepo = new com.habit.server.TaskRepository();
                        java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
                        java.util.List<String> taskIds = utsRepo.findTaskIdsByUserIdAndTeamId(userId, teamID);
                        java.util.List<com.habit.domain.Task> teamTasks = taskRepo.findTeamTasksByTeamID(teamID);
                        java.util.List<com.habit.domain.Task> filtered = new java.util.ArrayList<>();
                        java.util.List<com.habit.domain.UserTaskStatus> statusList =
                            utsRepo.findByUserIdAndTeamIdAndDate(userId, teamID, date);
                        java.util.Set<String> incompleteTaskIds = new java.util.HashSet<>();
                        for (com.habit.domain.UserTaskStatus status : statusList) {
                            if (!status.isDone()) {
                                incompleteTaskIds.add(status.getTaskId());
                            }
                        }
                        for (com.habit.domain.Task t : teamTasks) {
                            if (taskIds.contains(t.getTaskId()) && incompleteTaskIds.contains(t.getTaskId())) {
                                filtered.add(t);
                            }
                        }
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < filtered.size(); i++) {
                            com.habit.domain.Task t = filtered.get(i);
                            sb.append("{");
                            sb.append("\"taskId\":\"").append(t.getTaskId().replace("\"", "\\\"")).append("\",");
                            sb.append("\"taskName\":\"").append(t.getTaskName().replace("\"", "\\\"")).append("\",");
                            String dueTime = "";
                            try {
                                java.lang.reflect.Method m = t.getClass().getMethod("getDueTime");
                                Object val = m.invoke(t);
                                if (val != null) dueTime = val.toString();
                            } catch (Exception ignore) {}
                            sb.append("\"dueTime\":\"").append(dueTime.replace("\"", "\\\"")).append("\"");
                            sb.append("}");
                            if (i < filtered.size() - 1) sb.append(",");
                        }
                        sb.append("]");
                        response = sb.toString();
                    }
                }
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
                os = exchange.getResponseBody();
                os.write(response.getBytes("UTF-8"));
            } catch (Exception e) {
                e.printStackTrace();
                String errJson = "{\"error\":\"" + e.getClass().getSimpleName() + ": " + e.getMessage() + "\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(500, errJson.getBytes("UTF-8").length);
                os = exchange.getResponseBody();
                os.write(errJson.getBytes("UTF-8"));
            } finally {
                if (os != null) {
                    try { os.close(); } catch (Exception ignore) {}
                }
            }
        }
    }
}