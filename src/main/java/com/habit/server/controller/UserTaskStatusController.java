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
            System.out.println("[UserTaskStatusController] getUserIncompleteTasks API called");
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
                System.out.println("[UserTaskStatusController] sessionId: " + sessionId);
                System.out.println("[UserTaskStatusController] teamID: " + teamID);
                System.out.println("[UserTaskStatusController] dateStr: " + dateStr);
                String response = "[]";
                if (sessionId != null && teamID != null && dateStr != null) {
                    var user = authService.getUserBySession(sessionId);
                    System.out.println("[UserTaskStatusController] User from session: " + (user != null ? user.getUserId() : "null"));
                    if (user != null) {
                        String userId = user.getUserId();
                        System.out.println("[UserTaskStatusController] userId: " + userId);
                        com.habit.server.repository.UserTaskStatusRepository utsRepo = new com.habit.server.repository.UserTaskStatusRepository();
                        com.habit.server.repository.TaskRepository taskRepo = new com.habit.server.repository.TaskRepository();
                        java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
                        System.out.println("[UserTaskStatusController] Parsed date: " + date);
                        java.util.List<com.habit.domain.Task> teamTasks = taskRepo.findTeamTasksByTeamID(teamID);
                        System.out.println("[UserTaskStatusController] Team tasks count: " + teamTasks.size());
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
                        System.out.println("[UserTaskStatusController] Status list count (today+tomorrow): " + statusList.size());
                        
                        // 指定日のユーザーのタスク状況をマップ化（originalTaskIdベースで重複排除）
                        java.util.Map<String, com.habit.domain.UserTaskStatus> statusMapByTaskId = new java.util.HashMap<>();
                        java.util.Map<String, com.habit.domain.UserTaskStatus> statusMapByOriginalId = new java.util.HashMap<>();
                        for (com.habit.domain.UserTaskStatus status : statusList) {
                            statusMapByTaskId.put(status.getTaskId(), status);
                            statusMapByOriginalId.put(status.getOriginalTaskId(), status);
                            System.out.println("[UserTaskStatusController] Status - taskId: " + status.getTaskId() + ", originalTaskId: " + status.getOriginalTaskId() + ", isDone: " + status.isDone());
                        }
                        
                        // ユーザーが担当するタスクIDを取得
                        java.util.List<String> userTaskIds = utsRepo.findTaskIdsByUserIdAndTeamId(userId, teamID);
                        System.out.println("[UserTaskStatusController] User task IDs: " + userTaskIds);
                        
                        // originalTaskIdで重複を排除するためのセット
                        java.util.Set<String> addedOriginalTaskIds = new java.util.HashSet<>();
                        
                        // チームのすべてのタスクを確認
                        for (com.habit.domain.Task t : teamTasks) {
                            boolean isTeamTask = false;
                            try {
                                java.lang.reflect.Method m = t.getClass().getMethod("isTeamTask");
                                Object val = m.invoke(t);
                                isTeamTask = Boolean.TRUE.equals(val);
                            } catch (Exception ignore) {}
                            
                            if (isTeamTask) {
                                // ★修正：ユーザーが担当しているタスクかどうかをチェック
                                boolean isUserTask = userTaskIds.contains(t.getTaskId()) ||
                                                   userTaskIds.contains(t.getOriginalTaskId());
                                
                                if (isUserTask) {
                                    String originalTaskId = t.getOriginalTaskId();
                                    
                                    // 同じoriginalTaskIdのタスクが既に追加されている場合はスキップ
                                    if (addedOriginalTaskIds.contains(originalTaskId)) {
                                        System.out.println("[UserTaskStatusController] Skipping duplicate originalTaskId: " + originalTaskId + " for task: " + t.getTaskName());
                                        continue;
                                    }
                                    
                                    // originalTaskIdでステータスを確認（最新のタスクを優先）
                                    com.habit.domain.UserTaskStatus status = statusMapByOriginalId.get(originalTaskId);
                                    if (status == null) {
                                        status = statusMapByTaskId.get(t.getTaskId());
                                    }
                                    
                                    // 再設定されたタスクの場合：翌日のタスクでも当日表示する
                                    // タスクの期限日が翌日以降で、元タスクが完了済みの場合は表示対象とする
                                    boolean isResetTask = false;
                                    if (t.getDueDate() != null && t.getDueDate().isAfter(date)) {
                                        System.out.println("[UserTaskStatusController] Checking reset task: " + t.getTaskName() +
                                            ", dueDate=" + t.getDueDate() + ", requestDate=" + date);
                                        // 元タスクが完了済みかどうかを確認
                                        for (com.habit.domain.UserTaskStatus s : statusList) {
                                            if (s.getOriginalTaskId().equals(originalTaskId) && s.getDate().equals(date) && s.isDone()) {
                                                isResetTask = true;
                                                System.out.println("[UserTaskStatusController] Found completed original task for reset: " +
                                                    originalTaskId + ", completionDate=" + s.getDate());
                                                break;
                                            }
                                        }
                                    }
                                    
                                    // 未完了の場合、または再設定されたタスクの場合に返す
                                    if (status == null || !status.isDone() || isResetTask) {
                                        String statusDesc = status == null ? "new assignment" :
                                                           (isResetTask ? "reset task" : "incomplete");
                                        System.out.println("[UserTaskStatusController] Adding user task to filtered: " + t.getTaskName() +
                                            " (taskId: " + t.getTaskId() + ", originalTaskId: " + originalTaskId +
                                            ", dueDate: " + t.getDueDate() + ", status: " + statusDesc + ")");
                                        filtered.add(t);
                                        addedOriginalTaskIds.add(originalTaskId);
                                    } else {
                                        System.out.println("[UserTaskStatusController] Skipping completed user task: " + t.getTaskName() +
                                            " (taskId: " + t.getTaskId() + ", originalTaskId: " + originalTaskId + ", dueDate: " + t.getDueDate() + ")");
                                    }
                                } else {
                                    System.out.println("[UserTaskStatusController] Skipping non-user task: " + t.getTaskName() + " (not assigned to user)");
                                }
                            }
                        }
                        System.out.println("[UserTaskStatusController] Final filtered tasks count: " + filtered.size());
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < filtered.size(); i++) {
                            com.habit.domain.Task t = filtered.get(i);
                            sb.append("{");
                            sb.append("\"taskId\":\"").append(t.getTaskId().replace("\"", "\\\"")).append("\",");
                            sb.append("\"taskName\":\"").append(t.getTaskName().replace("\"", "\\\"")).append("\",");
                            String dueTime = "";
                            String dueDate = "";
                            try {
                                java.lang.reflect.Method m = t.getClass().getMethod("getDueTime");
                                Object val = m.invoke(t);
                                if (val != null) dueTime = val.toString();
                            } catch (Exception ignore) {}
                            try {
                                java.lang.reflect.Method m = t.getClass().getMethod("getDueDate");
                                Object val = m.invoke(t);
                                if (val != null) dueDate = val.toString();
                            } catch (Exception ignore) {}
                            sb.append("\"dueTime\":\"").append(dueTime.replace("\"", "\\\"")).append("\",");
                            sb.append("\"dueDate\":\"").append(dueDate.replace("\"", "\\\"")).append("\"");
                            sb.append("}");
                            if (i < filtered.size() - 1) sb.append(",");
                        }
                        sb.append("]");
                        response = sb.toString();
                    }
                }
                System.out.println("[UserTaskStatusController] Final response: " + response);
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
                    com.habit.server.repository.TaskRepository taskRepo = new com.habit.server.repository.TaskRepository();
                    
                    // 既存のステータスがあれば取得、なければ新規作成
                    java.util.Optional<com.habit.domain.UserTaskStatus> optStatus = repo.findByUserIdAndTaskIdAndDate(userId[0], taskId[0], date);
                    com.habit.domain.UserTaskStatus status = optStatus.orElseGet(() ->
                        new com.habit.domain.UserTaskStatus(userId[0], taskId[0], date, false)
                    );
                    status.setDone(true);
                    repo.save(status);
                    
                    // ★即座のタスク再設定処理を追加★
                    try {
                        // 完了したタスクの情報を取得
                        com.habit.server.repository.TeamRepository teamRepo = new com.habit.server.repository.TeamRepository();
                        java.util.List<String> allTeamIds = teamRepo.findAllTeamIds();
                        
                        com.habit.domain.Task completedTask = null;
                        String teamId = null;
                        
                        // 全チームから該当タスクを検索
                        for (String tId : allTeamIds) {
                            java.util.List<com.habit.domain.Task> teamTasks = taskRepo.findTeamTasksByTeamID(tId);
                            for (com.habit.domain.Task task : teamTasks) {
                                if (task.getTaskId().equals(taskId[0])) {
                                    completedTask = task;
                                    teamId = tId;
                                    break;
                                }
                            }
                            if (completedTask != null) break;
                        }
                        
                        if (completedTask != null && teamId != null) {
                            // タスク再設定サービスを使用して即座に再設定
                            com.habit.server.service.TaskAutoResetService autoResetService =
                                new com.habit.server.service.TaskAutoResetService(taskRepo, repo);
                            
                            System.out.println("[CompleteUserTaskHandler] 即座のタスク再設定を実行: taskId=" + taskId[0] +
                                ", userId=" + userId[0] + ", cycleType=" + completedTask.getCycleType());
                            
                            boolean resetSuccess = autoResetService.createNextTaskInstanceImmediately(
                                completedTask, userId[0], date, teamId);
                            
                            if (resetSuccess) {
                                System.out.println("[CompleteUserTaskHandler] 即座のタスク再設定成功");
                            } else {
                                System.out.println("[CompleteUserTaskHandler] 即座のタスク再設定スキップまたは対象外");
                            }
                        } else {
                            System.out.println("[CompleteUserTaskHandler] 完了タスクが見つかりません: taskId=" + taskId[0]);
                        }
                    } catch (Exception autoResetEx) {
                        System.err.println("[CompleteUserTaskHandler] 即座のタスク再設定でエラー: " + autoResetEx.getMessage());
                        autoResetEx.printStackTrace();
                        // 再設定エラーがあってもタスク完了は成功として処理を継続
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
                        // originalTaskIdでグループ化された進捗を取得（重複排除）
                        java.util.List<com.habit.domain.UserTaskStatus> statusList = utsRepo.findByTeamIdAndDateRangeGroupedByOriginalTaskId(teamID, from, date);
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < statusList.size(); i++) {
                            com.habit.domain.UserTaskStatus s = statusList.get(i);
                            sb.append("{");
                            sb.append("\"userId\":\"").append(s.getUserId().replace("\"", "\\\"")).append("\",");
                            sb.append("\"taskId\":\"").append(s.getOriginalTaskId().replace("\"", "\\\"")).append("\","); // originalTaskIdを使用
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