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

    public HttpHandler getCompleteUserTaskHandler() {
        return new CompleteUserTaskHandler();
    }

    public HttpHandler getGetUserTaskStatusListHandler() {
        return new GetUserTaskStatusListHandler();
    }
 
    public HttpHandler getSaveUserTaskStatusHandler() {
        return new SaveUserTaskStatusHandler();
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
    // --- UserTaskStatus保存API ---
    class SaveUserTaskStatusHandler implements com.sun.net.httpserver.HttpHandler {
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                String response = "POSTメソッドのみ対応";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try (java.io.OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
            byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
            String bodyStr = (bodyBytes != null) ? new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8) : "";
            String response;
            try {
                String[] params = bodyStr.split("&");
                java.util.Map<String, String> map = new java.util.HashMap<>();
                for (String param : params) {
                    String[] kv = param.split("=", 2);
                    if (kv.length < 2) continue;
                    map.put(kv[0], java.net.URLDecoder.decode(kv[1], "UTF-8"));
                }
                String userId = map.get("userId");
                String taskId = map.get("taskId");
                java.time.LocalDate date = java.time.LocalDate.parse(map.get("date"));
                boolean isDone = Boolean.parseBoolean(map.getOrDefault("isDone", "false"));
                com.habit.domain.UserTaskStatus status = new com.habit.domain.UserTaskStatus(userId, taskId, date, isDone);
                new com.habit.server.UserTaskStatusRepository().save(status);
                response = "UserTaskStatus保存成功";
            } catch (Exception ex) {
                response = "UserTaskStatus保存失敗: " + ex.getMessage();
            }
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (java.io.OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
                // --- ユーザーのタスク完了API ---
            }
        }
    }
    public static class CompleteUserTaskHandler implements com.sun.net.httpserver.HttpHandler {
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
                        java.io.OutputStream os = null;
            try {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    String response = "POSTメソッドのみ対応";
                    exchange.sendResponseHeaders(405, response.getBytes().length);
                    os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    return;
                }
                byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
                String bodyStr = (bodyBytes != null) ? new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8) : "";
                final String[] userId = {null};
                final String[] taskId = {null};
                String dateStr = null;
                if (bodyStr != null && !bodyStr.isEmpty()) {
                    String[] params = bodyStr.split("&");
                    for (String param : params) {
                        String[] kv = param.split("=", 2);
                        if (kv.length < 2) continue;
                        if (kv[0].equals("userId")) userId[0] = java.net.URLDecoder.decode(kv[1], "UTF-8");
                        if (kv[0].equals("taskId")) taskId[0] = java.net.URLDecoder.decode(kv[1], "UTF-8");
                        if (kv[0].equals("date")) dateStr = java.net.URLDecoder.decode(kv[1], "UTF-8");
                    }
                }
                String response;
                if (userId[0] != null && taskId[0] != null && dateStr != null) {
                    java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
                    UserTaskStatusRepository repo = new UserTaskStatusRepository();
                    // 既存のステータスがあれば取得、なければ新規作成
                    java.util.Optional<com.habit.domain.UserTaskStatus> optStatus = repo.findByUserIdAndTaskIdAndDate(userId[0], taskId[0], date);
                    com.habit.domain.UserTaskStatus status = optStatus.orElseGet(() ->
                        new com.habit.domain.UserTaskStatus(userId[0], taskId[0], date, false)
                    );
                    status.setDone(true);
                    repo.save(status);
                    response = "タスク完了: userId=" + userId[0] + ", taskId=" + taskId[0] + ", date=" + dateStr;
                } else {
                    response = "パラメータが不正です";
                }
                exchange.sendResponseHeaders(200, response.getBytes().length);
                os = exchange.getResponseBody();
                os.write(response.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
                String err = "サーバーエラー: " + e.getMessage();
                exchange.sendResponseHeaders(500, err.getBytes().length);
                os = exchange.getResponseBody();
                os.write(err.getBytes());
            } finally {
                if (os != null) {
                    try { os.close(); } catch (Exception ignore) {}
                }
            }
        }  
    } 
    // --- ユーザー・チーム・日付ごとの全UserTaskStatus（taskId, isDone）を返すAPI ---
    class GetUserTaskStatusListHandler implements com.sun.net.httpserver.HttpHandler {
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
                        UserTaskStatusRepository utsRepo = new UserTaskStatusRepository();
                        java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
                        java.util.List<com.habit.domain.UserTaskStatus> statusList = utsRepo.findByUserIdAndTeamIdAndDate(userId, teamID, date);
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < statusList.size(); i++) {
                            com.habit.domain.UserTaskStatus s = statusList.get(i);
                            sb.append("{");
                            sb.append("\"taskId\":\"").append(s.getTaskId().replace("\"", "\\\"")).append("\",");
                            sb.append("\"isDone\":").append(s.isDone());
                            sb.append("}");
                            if (i < statusList.size() - 1) sb.append(",");
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