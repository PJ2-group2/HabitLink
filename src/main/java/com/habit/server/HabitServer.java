package com.habit.server;

// 習慣化共有プログラムのサーバ側プログラム
// クライアントからのHTTPリクエストを受けて、チームやタスクの情報を管理します
// サーバはSQLiteを用いてチーム・タスク情報を永続化します

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.habit.server.controller.HelloController;
import com.habit.server.controller.TaskController;
import com.habit.server.controller.TeamController;
import com.habit.server.controller.AuthController;
import com.habit.server.controller.MessageController;
import com.habit.server.controller.UserController;
import com.habit.server.controller.UserTaskStatusController;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

// JDBC based team management
// import com.habit.server.DatabaseTeamManager;

/**
 * 習慣化共有アプリのサーバ本体クラス。
 * HTTPリクエストを受けてチーム・タスク・ユーザ管理など各種APIを提供する。
 * SQLiteによる永続化や、チャット・チーム機能も実装。
 */
public class HabitServer {
  private static TaskController taskController;
  // ユーザ認証用サービス
  private static UserRepository userRepository = new UserRepository();
  private static AuthService authService = new AuthService(userRepository);
  // チャットサービス用リポジトリ
  private static MessageRepository messageRepository = new MessageRepository();
  private static MessageController messageController = new MessageController(messageRepository, userRepository);

  private static UserController userController = new UserController(authService, userRepository);

  private static UserTaskStatusRepository userTaskStatusRepository = new UserTaskStatusRepository();
  private static UserTaskStatusController userTaskStatusController = new UserTaskStatusController(authService, userTaskStatusRepository);

  public static void main(String[] args) throws Exception {
    // サーバを8080番ポートで起動
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
    // 各APIエンドポイントを登録
    server.createContext("/hello", new HelloController());           // 動作確認用

    taskController = new TaskController(teamManager);
    server.createContext("/addTask", taskController.getAddTaskHandler());       // タスク追加
    server.createContext("/getTasks", taskController.getGetTasksHandler()); // タスク一覧取得
    AuthController authController = new AuthController(authService, userRepository);
    server.createContext("/login", authController.getLoginHandler());           // ログイン
    server.createContext("/register", authController.getRegisterHandler());     // 新規登録
    TeamController teamController = new TeamController(authService, userRepository);
    server.createContext("/createTeam", teamController.getCreateTeamHandler());   // チーム作成
    server.createContext("/joinTeam", teamController.getJoinTeamHandler());     // チーム参加
    server.createContext("/publicTeams", teamController.getPublicTeamsHandler()); // 公開チーム一覧
    server.createContext("/findTeamByPasscode", teamController.getFindTeamByPasscodeHandler()); // 合言葉検索
    server.createContext("/sendChatMessage", messageController.getSendChatMessageHandler()); // チャット送信
    server.createContext("/getChatLog", messageController.getGetChatLogHandler()); // チャット履歴取得
    server.createContext("/getJoinedTeamInfo", userController.getGetJoinedTeamInfoHandler()); // 参加チーム取得
    server.createContext("/getUserTaskIds", userTaskStatusController.getGetUserTaskIdsHandler()); // UserTaskStatusからTaskId取得
    server.createContext("/getTeamName", teamController.getGetTeamNameHandler()); // チーム名取得
    // タスクID→タスク名マップ取得API
    server.createContext("/getUserTeamTasks", new GetUserTeamTasksHandler()); // チーム内で自分に紐づくタスク取得
    server.createContext("/getTaskIdNameMap", taskController.getTaskIdNameMapHandler());
    server.setExecutor(null);
    // ユーザーの未完了タスク一覧取得API
    server.createContext("/getUserIncompleteTasks", new GetUserIncompleteTasksHandler());
    // ユーザー・チーム・日付ごとの全UserTaskStatus（taskId, isDone）を返すAPI
    server.createContext("/getUserTaskStatusList", new GetUserTaskStatusListHandler());
    // ユーザーのタスク完了API
    server.createContext("/completeUserTask", new CompleteUserTaskHandler());
    // タスク保存API
    server.createContext("/saveTask", new SaveTaskHandler());
    // UserTaskStatus保存API
    server.createContext("/saveUserTaskStatus", new SaveUserTaskStatusHandler());
    server.start();
    System.out.println("サーバが起動しました: http://localhost:8080/hello");
  }
  // --- 動作確認用API ---
  /**
   * 動作確認用API
   * /hello にアクセスするとサーバが動作中か確認できる
   */
  // HelloHandlerはHelloControllerへ移行

  // --- チーム管理用（SQLite でチームIDを保持）---
  private static DatabaseTeamManager teamManager =
      new DatabaseTeamManager("jdbc:sqlite:habit.db");

  // --- タスク追加API ---
  /**
   * タスク追加API
   * /addTask?id=xxx&task=yyy で指定チームにタスクを追加する
   */
  // AddTaskHandlerはTaskControllerへ移行

  // --- タスク一覧取得API ---
  /**
   * タスク一覧取得API
   * /getTasks?id=xxx で指定チームのタスク一覧を取得する
   */
  // GetTasksHandlerはTaskControllerへ移行
  // --- ログインAPI ---
  // LoginHandlerはAuthControllerへ移行

  // --- 新規登録API ---
  // RegisterHandlerはAuthControllerへ移行
  // --- チーム作成API ---
  // CreateTeamHandlerはTeamControllerへ移行\n// --- 公開チーム一覧取得API ---
  // PublicTeamsHandlerはTeamControllerへ移行

  // --- 合言葉でチーム検索API ---
  // FindTeamByPasscodeHandlerはTeamControllerへ移行

  // --- チーム参加API（メンバー追加） ---
  // JoinTeamHandlerはTeamControllerへ移行

    // --- チャット履歴取得API ---
    // GetChatLogHandlerはMessageControllerへ移行

    // --- チャット送信API ---
    // SendChatMessageHandlerはMessageControllerへ移行
  /**
   * 現在ログイン中ユーザのuserId, joinedTeamIds, joinedTeamNamesを取得するAPI。
   * ユーザがログインしている場合、SESSION_IDヘッダからセッションIDを取得し、
   * ユーザ情報を返す。
   */
  // --- 参加チーム取得API ---
  // GetJoinedTeamInfoHandlerはUserControllerへ移行
    /**
     * セッションIDからUserを検索し、そのuserIdからUserTaskStatus中の該当TaskId一覧を返すAPI。
     * /getUserTaskIds エンドポイント
     * SESSION_IDヘッダ必須
     */
    // --- UserTaskStatusからTaskId取得API ---
    // GetUserTaskIdsHandlerはUserTaskStatusControllerへ移行
    /**
     * /getTeamName?teamID=xxx でチーム名を返すAPI
     */
    /**
     * /getTaskIdNameMap?id=xxx でチームIDからタスクID→タスク名のマップ(JSON)を返すAPI
     */
    // --- タスクID→タスク名マップ取得API ---
    // GetTaskIdNameMapHandlerはTaskControllerへ移行

    // --- チーム名取得API ---
    // GetTeamNameHandlerはTeamControllerへ移行
    /**
     * /getUserTeamTasks?teamID=xxx
     * SESSION_IDヘッダ必須
     * 指定チーム内で自分に紐づくタスク（Task情報）をJSON配列で返す
     */
    static class GetUserTeamTasksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
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
                    UserTaskStatusRepository utsRepo = new UserTaskStatusRepository();
                    TaskRepository taskRepo = new TaskRepository();
                    // 1. ユーザーが担当するチーム内タスクID一覧
                    java.util.List<String> taskIds = utsRepo.findTaskIdsByUserIdAndTeamId(userId, teamID);
                    // 2. チーム内全タスク
                    java.util.List<com.habit.domain.Task> teamTasks = taskRepo.findTeamTasksByTeamID(teamID);
                    // 3. 担当タスクのみ抽出
                    java.util.List<com.habit.domain.Task> filtered = new java.util.ArrayList<>();
                    for (com.habit.domain.Task t : teamTasks) {
                        if (taskIds.contains(t.getTaskId())) {
                            filtered.add(t);
                        }
                    }
                    // 4. JSON配列で返す
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < filtered.size(); i++) {
                        com.habit.domain.Task t = filtered.get(i);
                        sb.append("{");
                        sb.append("\"taskId\":\"").append(t.getTaskId().replace("\"", "\\\"")).append("\",");
                        sb.append("\"taskName\":\"").append(t.getTaskName().replace("\"", "\\\"")).append("\"");
                        sb.append("}");
                        if (i < filtered.size() - 1) sb.append(",");
                    }
                    sb.append("]");
                    response = sb.toString();
                }
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes("UTF-8"));
            os.close();
        }
    }
    // --- ユーザーの未完了タスク一覧取得API ---
    static class GetUserIncompleteTasksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            OutputStream os = null;
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
                        TaskRepository taskRepo = new TaskRepository();
                        java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
                        // チーム内タスクID一覧
                        java.util.List<String> taskIds = utsRepo.findTaskIdsByUserIdAndTeamId(userId, teamID);
                        java.util.List<com.habit.domain.Task> teamTasks = taskRepo.findTeamTasksByTeamID(teamID);
                        java.util.List<com.habit.domain.Task> filtered = new java.util.ArrayList<>();
                        // 一括取得で未完了のみ抽出
                        List<com.habit.domain.UserTaskStatus> statusList =
                            utsRepo.findByUserIdAndTeamIdAndDate(userId, teamID, date);
                        Set<String> incompleteTaskIds = new HashSet<>();
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
                        // JSON配列で返す（dueTimeも含める）
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < filtered.size(); i++) {
                            com.habit.domain.Task t = filtered.get(i);
                            sb.append("{");
                            sb.append("\"taskId\":\"").append(t.getTaskId().replace("\"", "\\\"")).append("\",");
                            sb.append("\"taskName\":\"").append(t.getTaskName().replace("\"", "\\\"")).append("\",");
                            // dueTimeがnullでなければ出力、nullなら空文字
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
    // --- ユーザー・チーム・日付ごとの全UserTaskStatus（taskId, isDone）を返すAPI ---
    static class GetUserTaskStatusListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            OutputStream os = null;
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
                        List<com.habit.domain.UserTaskStatus> statusList = utsRepo.findByUserIdAndTeamIdAndDate(userId, teamID, date);
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
    // --- ユーザーのタスク完了API ---
    static class CompleteUserTaskHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            OutputStream os = null;
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
    // --- タスク保存API ---
    static class SaveTaskHandler implements HttpHandler {
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
                // key=value&...形式で受信
                String[] params = bodyStr.split("&");
                java.util.Map<String, String> map = new java.util.HashMap<>();
                for (String param : params) {
                    String[] kv = param.split("=", 2);
                    if (kv.length < 2) continue;
                    map.put(kv[0], java.net.URLDecoder.decode(kv[1], "UTF-8"));
                }
                String taskId = map.get("taskId");
                String taskName = map.get("taskName");
                String description = map.getOrDefault("description", "");
                int estimatedMinutes = Integer.parseInt(map.getOrDefault("estimatedMinutes", "0"));
                boolean isTeamTask = Boolean.parseBoolean(map.getOrDefault("isTeamTask", "false"));
                String dueTimeStr = map.get("dueTime");
                java.time.LocalTime dueTime = dueTimeStr != null && !dueTimeStr.isEmpty() ? java.time.LocalTime.parse(dueTimeStr) : null;
                String cycleType = map.getOrDefault("cycleType", "daily");
                String teamID = map.get("teamID");
                // repeatDaysは未対応
                com.habit.domain.Task task = new com.habit.domain.Task(
                    taskId, taskName, description, estimatedMinutes,
                    java.util.Collections.emptyList(), isTeamTask, dueTime, cycleType
                );
                new com.habit.server.TaskRepository().saveTask(task, teamID);
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

    // --- UserTaskStatus保存API ---
    static class SaveUserTaskStatusHandler implements com.sun.net.httpserver.HttpHandler {
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
            }
        }
    }
  }