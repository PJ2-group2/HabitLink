package com.habit.server.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.habit.server.repository.UserTaskStatusRepository;
import com.habit.server.service.AuthService;
import com.habit.domain.UserTaskStatus;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserTaskStatusController {
    private static final Logger logger = LoggerFactory.getLogger(UserTaskStatusController.class);
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

    // --- ユーザーの未完了タスク(isDone=false, dueDate > now)一覧取得API ---
    public HttpHandler getIncompleteUserTaskStatusHandler(com.habit.server.service.AuthService authService) {
        return new GetIncompleteUserTaskStatusHandler(authService);
    }

    public static class GetIncompleteUserTaskStatusHandler implements com.sun.net.httpserver.HttpHandler {
        private final com.habit.server.service.AuthService authService;

        public GetIncompleteUserTaskStatusHandler(com.habit.server.service.AuthService authService) {
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
                if (query != null) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("teamID=")) teamID = java.net.URLDecoder.decode(param.substring(7), "UTF-8");
                    }
                }
                
                String response = "[]";
                if (sessionId != null && teamID != null) {
                    var user = authService.getUserBySession(sessionId);
                    if (user != null) {
                        String userId = user.getUserId();
                        com.habit.server.repository.UserTaskStatusRepository utsRepo = new com.habit.server.repository.UserTaskStatusRepository();
                        com.habit.server.repository.TaskRepository taskRepo = new com.habit.server.repository.TaskRepository();
                        
                        java.util.List<com.habit.domain.UserTaskStatus> allUserTaskStatuses = utsRepo.findByUserIdAndTeamId(userId, teamID);
                        java.util.List<com.habit.domain.Task> allTeamTasks = taskRepo.findTeamTasksByTeamID(teamID);
                        java.util.Map<String, com.habit.domain.Task> taskMap = new java.util.HashMap<>();
                        for(com.habit.domain.Task task : allTeamTasks) {
                            taskMap.put(task.getTaskId(), task);
                        }

                        java.util.List<com.habit.domain.UserTaskStatus> incompleteStatuses = new java.util.ArrayList<>();
                        java.util.Set<String> addedTaskIds = new java.util.HashSet<>();
                        for (com.habit.domain.UserTaskStatus status : allUserTaskStatuses) {
                            com.habit.domain.Task correspondingTask = taskMap.get(status.getTaskId());
                            if (correspondingTask != null) {
                                java.time.LocalDate dueDate = correspondingTask.getDueDate();
                                if (!status.isDone() && dueDate != null && dueDate.isAfter(java.time.LocalDate.now().minusDays(1))) {
                                    if (addedTaskIds.add(status.getTaskId())) {
                                        incompleteStatuses.add(status);
                                    }
                                }
                            }
                        }
                        
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < incompleteStatuses.size(); i++) {
                            com.habit.domain.UserTaskStatus s = incompleteStatuses.get(i);
                            com.habit.domain.Task t = taskMap.get(s.getTaskId());
                            sb.append("{");
                            sb.append("\"taskId\":\"").append(s.getTaskId().replace("\"", "\\")).append("\",");
                            sb.append("\"taskName\":\"").append(t.getTaskName().replace("\"", "\\")).append("\",");
                            sb.append("\"dueDate\":\"").append(t.getDueDate().toString().replace("\"", "\\")).append("\",");
                            sb.append("\"isDone\":").append(s.isDone());
                            sb.append("}");
                            if (i < incompleteStatuses.size() - 1) sb.append(",");
                        }
                        sb.append("]");
                        response = sb.toString();

                        logger.info("[UserTaskStatusController] Responding with incomplete tasks: {}", response);
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
    // --- ユーザーの未完了タスク一覧取得API ---
    public HttpHandler getUserIncompleteTasksHandler(com.habit.server.service.AuthService authService) {
        return new GetUserIncompleteTasksHandler(authService);
    }

    public static class GetUserIncompleteTasksHandler implements com.sun.net.httpserver.HttpHandler {
        private final com.habit.server.service.AuthService authService;

        public GetUserIncompleteTasksHandler(com.habit.server.service.AuthService authService) {
            this.authService = authService;
        }

        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
            logger.info("[UserTaskStatusController] getUserIncompleteTasks API called");
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
                logger.info("[UserTaskStatusController] sessionId: {}", sessionId);
                logger.info("[UserTaskStatusController] teamID: {}", teamID);
                logger.info("[UserTaskStatusController] dateStr: {}", dateStr);
                String response = "[]";
                if (sessionId != null && teamID != null && dateStr != null) {
                    var user = authService.getUserBySession(sessionId);
                    if (user != null) {
                        String userId = user.getUserId();
                        logger.info("[UserTaskStatusController] userId: {}", userId);
                        com.habit.server.repository.UserTaskStatusRepository utsRepo = new com.habit.server.repository.UserTaskStatusRepository();
                        com.habit.server.repository.TaskRepository taskRepo = new com.habit.server.repository.TaskRepository();
                        java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
                        logger.info("[UserTaskStatusController] Parsed date: {}", date);
                        java.util.List<com.habit.domain.Task> teamTasks = taskRepo.findTeamTasksByTeamID(teamID);
                        logger.info("[UserTaskStatusController] Team tasks count: {}", teamTasks.size());
                        java.util.List<com.habit.domain.Task> filtered = new java.util.ArrayList<>();
                        
                        // 当日と翌日の両方のUserTaskStatusを取得（再設定されたタスクを含む）
                        java.time.LocalDate tomorrow = date.plusDays(1);
                        java.util.List<com.habit.domain.UserTaskStatus> statusListToday =
                            utsRepo.findByUserIdAndTeamIdAndDate(userId, teamID, date);
                        java.util.List<com.habit.domain.UserTaskStatus> statusListTomorrow =
                            utsRepo.findByUserIdAndTeamIdAndDate(userId, teamID, tomorrow);
                        
                        java.util.List<com.habit.domain.UserTaskStatus> statusList = new java.util.ArrayList<>();
                        statusList.addAll(statusListToday);
                        statusList.addAll(statusListTomorrow);
                        logger.info("[UserTaskStatusController] Status list count (today+tomorrow): {}", statusList.size());
                        
                        // 指定日のユーザーのタスク状況をマップ化
                        java.util.Map<String, com.habit.domain.UserTaskStatus> statusMapByTaskId = new java.util.HashMap<>();
                        for (com.habit.domain.UserTaskStatus status : statusList) {
                            statusMapByTaskId.put(status.getTaskId(), status);
                            logger.info("[UserTaskStatusController] Status - taskId: {} , isDone: {}", status.getTaskId(), status.isDone());
                        }
                        
                        // ユーザーが担当するタスクIDセットを取得
                        java.util.Set<String> userTaskIds = new java.util.HashSet<>(utsRepo.findTaskIdsByUserIdAndTeamId(userId, teamID));
                        logger.info("[UserTaskStatusController] User task IDs: {}", userTaskIds);
                        
                        // 未完了タスクをフィルタリング
                        for (com.habit.domain.Task task : teamTasks) {
                            if (userTaskIds.contains(task.getTaskId())) {
                                com.habit.domain.UserTaskStatus status = statusMapByTaskId.get(task.getTaskId());
                                if (status == null || !status.isDone()) {
                                    filtered.add(task);
                                }
                            }
                        }
                        
                        logger.info("[UserTaskStatusController] Final filtered tasks count: {}", filtered.size());
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < filtered.size(); i++) {
                            com.habit.domain.Task t = filtered.get(i);
                            sb.append("{");
                            sb.append("\"taskId\":\"").append(t.getTaskId().replace("\"", "\\\"")).append("\",");
                            sb.append("\"taskName\":\"").append(t.getTaskName().replace("\"", "\\\"")).append("\",");
                            String dueDate = "";
                            try {
                                java.lang.reflect.Method m = t.getClass().getMethod("getDueDate");
                                Object val = m.invoke(t);
                                if (val != null) dueDate = val.toString();
                            } catch (Exception ignore) {}
                            sb.append("\"dueDate\":\"").append(dueDate.replace("\"", "\\\"")).append("\",");
                            String cycleType = "";
                            try {
                                java.lang.reflect.Method m = t.getClass().getMethod("getCycleType");
                                Object val = m.invoke(t);
                                if (val != null) cycleType = val.toString();
                            } catch (Exception ignore) {}
                            sb.append("\"cycleType\":\"").append(cycleType.replace("\"", "\\\"")).append("\"");
                            sb.append("}");
                            if (i < filtered.size() - 1) sb.append(",");
                        }
                        sb.append("]");
                        response = sb.toString();
                    }
                }
                logger.info("[UserTaskStatusController] Responding with: {}", response);
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
                new com.habit.server.repository.UserTaskStatusRepository().save(status);
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

    /**
     * ユーザーのタスク完了APIハンドラー
     * 【処理内容】
     * 1. POSTメソッドでリクエストを受け取る
     * 2. リクエストボディからuserId, taskId, dateを取得
     * 3. UserTaskStatusRepositoryを使用して、指定ユーザーの
     *    タスク完了ステータスを更新または新規作成
     * 4. タスク完了後、即座にタスク再設定処理を実行
     * 5. レスポンスとして完了メッセージを返却
     */
    public static class CompleteUserTaskHandler implements com.sun.net.httpserver.HttpHandler {
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
            java.io.OutputStream os = null;
            try {
                // リクエストメソッドのチェック
                if (!"POST".equals(exchange.getRequestMethod())) {
                    String response = "POSTメソッドのみ対応";
                    exchange.sendResponseHeaders(405, response.getBytes().length);
                    os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    return;
                }
                // リクエストボディの解析
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
                    com.habit.server.repository.TaskRepository taskRepo = new com.habit.server.repository.TaskRepository();
                    
                    // 既存のステータスがなければエラーを返す
                    java.util.Optional<com.habit.domain.UserTaskStatus> optStatus = repo.findUpcomingIncompleteByUserIdAndTaskId(userId[0], taskId[0]);
                    if (optStatus.isEmpty()) {
                        response = "エラー: 該当のタスクステータスが見つかりません。";
                        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                        exchange.sendResponseHeaders(404, response.getBytes("UTF-8").length);
                        os = exchange.getResponseBody();
                        os.write(response.getBytes("UTF-8"));
                        return;
                    }
                    com.habit.domain.UserTaskStatus status = optStatus.get();
                    // タスク完了ステータスを更新(保存)
                    status.setDone(true);
                    repo.save(status);
                    
                    // タスク完了時にサボりポイントを即座に減らす
                    try {
                        com.habit.server.repository.UserRepository userRepo = new com.habit.server.repository.UserRepository();
                        com.habit.domain.User user = userRepo.findById(userId[0]);
                        if (user != null) {
                            user.addSabotagePoints(-1);
                            userRepo.save(user);
                            logger.info("タスク完了によりサボりポイント更新: {} {}pt", user.getUsername(), user.getSabotagePoints());
                        }
                    } catch (Exception e) {
                        logger.error("サボりポイント更新エラー: {}", e.getMessage(), e);
                    }
                    
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
                            sb.append("\"userId\":\"").append(s.getUserId().replace("\"", "\\\"")).append("\",");
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
    // チーム全員分のタスク進捗を返すAPI
    public HttpHandler getGetTeamTaskStatusListHandler() {
        return new GetTeamTaskStatusListHandler();
    }

    class GetTeamTaskStatusListHandler implements com.sun.net.httpserver.HttpHandler {
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
                int days = 1;
                if (query != null) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("teamID=")) teamID = java.net.URLDecoder.decode(param.substring(7), "UTF-8");
                        if (param.startsWith("date=")) dateStr = java.net.URLDecoder.decode(param.substring(5), "UTF-8");
                        if (param.startsWith("days=")) days = Integer.parseInt(param.substring(5));
                    }
                }
                String response = "[]";
                if (sessionId != null && teamID != null && dateStr != null) {
                    var user = authService.getUserBySession(sessionId);
                    if (user != null) {
                        UserTaskStatusRepository utsRepo = new UserTaskStatusRepository();
                        java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
                        java.time.LocalDate from = date.minusDays(days - 1);
                        java.util.List<com.habit.domain.UserTaskStatus> statusList = utsRepo.findByTeamIdAndDateRange(teamID, from, date);
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < statusList.size(); i++) {
                            com.habit.domain.UserTaskStatus s = statusList.get(i);
                            sb.append("{");
                            sb.append("\"userId\":\"").append(s.getUserId().replace("\"", "\\\"")).append("\",");
                            sb.append("\"taskId\":\"").append(s.getTaskId().replace("\"", "\\\"")).append("\",");
                            sb.append("\"date\":\"").append(s.getDate().toString()).append("\",");
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
