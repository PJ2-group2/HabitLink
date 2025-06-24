package com.habit.server;

// 習慣化共有プログラムのサーバ側プログラム
// クライアントからのHTTPリクエストを受けて、ルームやタスクの情報を管理します
// サーバはSQLiteを用いてルーム・タスク情報を永続化します

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

// JDBC based room management
import com.habit.server.DatabaseRoomManager;

/**
 * 習慣化共有アプリのサーバ本体クラス。
 * HTTPリクエストを受けてルーム・タスク・ユーザ管理など各種APIを提供する。
 * SQLiteによる永続化や、チャット・チーム機能も実装。
 */
public class HabitServer {
  // ユーザ認証用サービス
  private static UserRepository userRepository = new UserRepository();
  private static AuthService authService = new AuthService(userRepository);

  public static void main(String[] args) throws Exception {
    // サーバを8080番ポートで起動
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
    // 各APIエンドポイントを登録
    server.createContext("/hello", new HelloHandler());           // 動作確認用
    server.createContext("/createRoom", new CreateRoomHandler()); // ルーム作成
    server.createContext("/joinRoom", new JoinRoomHandler());     // ルーム参加
    server.createContext("/addTask", new AddTaskHandler());       // タスク追加
    server.createContext("/getTasks", new GetTasksHandler()); // タスク一覧取得
    server.createContext("/login", new LoginHandler());           // ログイン
    server.createContext("/register", new RegisterHandler());     // 新規登録
    server.createContext("/createTeam", new CreateTeamHandler());   // チーム作成
    server.createContext("/publicTeams", new PublicTeamsHandler()); // 公開チーム一覧
    server.createContext("/findTeamByPasscode", new FindTeamByPasscodeHandler()); // 合言葉検索
    server.createContext("/joinTeam", new JoinTeamHandler()); // チーム参加
    server.createContext("/sendChatMessage", new SendChatMessageHandler()); // チャット送信
    server.createContext("/getChatLog", new GetChatLogHandler()); // チャット履歴取得
    server.setExecutor(null);
    server.start();
    System.out.println("サーバが起動しました: http://localhost:8080/hello");
  }
  // --- 動作確認用API ---
  /**
   * 動作確認用API
   * /hello にアクセスするとサーバが動作中か確認できる
   */
  static class HelloHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
      String response = "Hello, HTTP! サーバは動作中です。";
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  // --- ルーム管理用（SQLite でルームIDを保持）---
  private static DatabaseRoomManager roomManager =
      new DatabaseRoomManager("jdbc:sqlite:habit.db");

  // --- ルーム作成API ---
  /**
   * ルーム作成API
   * /createRoom?id=xxx で新しいルームを作成する
   */
  static class CreateRoomHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      String response;
      if (query != null && query.startsWith("id=")) {
        String roomId = query.substring(3);
        synchronized (roomManager) {
          if (roomManager.roomExists(roomId)) {
            response = "ルーム『" + roomId + "』は既に存在します。";
          } else {
            roomManager.createRoom(roomId);
            response = "ルーム『" + roomId + "』を作成しました。";
          }
        }
      } else {
        response = "ルームIDが指定されていません。";
      }
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  // --- ルーム参加API ---
  /**
   * ルーム参加API
   * /joinRoom?id=xxx で指定ルームに参加する
   */
  static class JoinRoomHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      String response;
      if (query != null && query.startsWith("id=")) {
        String roomId = query.substring(3);
        synchronized (roomManager) {
          if (roomManager.roomExists(roomId)) {
            response = "ルーム『" + roomId + "』に参加しました。";
          } else {
            response = "ルーム『" + roomId + "』は存在しません。";
          }
        }
      } else {
        response = "ルームIDが指定されていません。";
      }
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  // --- タスク追加API ---
  /**
   * タスク追加API
   * /addTask?id=xxx&task=yyy で指定ルームにタスクを追加する
   */
  static class AddTaskHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      String response;
      if (query != null && query.contains("id=") && query.contains("task=")) {
        String[] params = query.split("&");
        String roomId = null, task = null;
        for (String param : params) {
          if (param.startsWith("id="))
            roomId = param.substring(3);
          if (param.startsWith("task="))
            task = param.substring(5);
        }
        if (roomId != null && task != null) {
          synchronized (roomManager) {
            if (!roomManager.roomExists(roomId)) {
              response = "ルーム『" + roomId + "』は存在しません。";
            } else {
              var room = roomManager.getTaskManager(roomId);
              room.addTask(task);
              response = "タスクを追加しました。";
            }
          }
        } else {
          response = "パラメータが不正です。";
        }
      } else {
        response = "パラメータが不正です。";
      }
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  // --- タスク一覧取得API ---
  /**
   * タスク一覧取得API
   * /getTasks?id=xxx で指定ルームのタスク一覧を取得する
   */
  static class GetTasksHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      String response;
      if (query != null && query.startsWith("id=")) {
        String roomId = query.substring(3);
        synchronized (roomManager) {
          if (!roomManager.roomExists(roomId)) {
            response = "ルーム『" + roomId + "』は存在しません。";
          } else {
            var room = roomManager.getTaskManager(roomId);
            var tasks = room.getTasks();
            if (tasks.isEmpty())
              response = "タスクはありません。";
            else
              response = String.join("\n", tasks);
          }
        }
      } else {
        response = "ルームIDが指定されていません。";
      }
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }
  // --- ログインAPI ---
  static class LoginHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
      if (!"POST".equals(exchange.getRequestMethod())) {
        String response = "POSTメソッドのみ対応";
        exchange.sendResponseHeaders(405, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
        return;
      }
      String query = exchange.getRequestURI().getQuery();
      String response;
      String username = null, password = null;
      if (query != null) {
        String[] params = query.split("&");
        for (String param : params) {
          if (param.startsWith("username=")) username = param.substring(9);
          if (param.startsWith("password=")) password = param.substring(9);
        }
      }
      if (username != null && password != null) {
        String sessionId = authService.loginAndCreateSession(username, password);
        if (sessionId != null) {
          response = "ログイン成功\nSESSION_ID:" + sessionId;
        } else {
          response = "ユーザ名またはパスワードが間違っています";
        }
      } else {
        response = "パラメータが不正です";
      }
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  // --- 新規登録API ---
  static class RegisterHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
      if (!"POST".equals(exchange.getRequestMethod())) {
        String response = "POSTメソッドのみ対応";
        exchange.sendResponseHeaders(405, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
        return;
      }
      String query = exchange.getRequestURI().getQuery();
      String response;
      String username = null, password = null;
      if (query != null) {
        String[] params = query.split("&");
        for (String param : params) {
          if (param.startsWith("username=")) username = param.substring(9);
          if (param.startsWith("password=")) password = param.substring(9);
        }
      }
      if (username != null && password != null) {
        var user = authService.register(username, password);
        if (user != null) {
          response = "登録成功";
        } else {
          response = "登録失敗";
        }
      } else {
        response = "パラメータが不正です";
      }
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }
  // --- チーム作成API ---
  static class CreateTeamHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
      if (!"POST".equals(exchange.getRequestMethod())) {
        String response = "POSTメソッドのみ対応";
        exchange.sendResponseHeaders(405, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
        return;
      }
      // クライアントからのPOSTボディを取得
      byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
      String bodyStr = (bodyBytes != null) ? new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8) : "";
      String response;
      if (bodyStr == null || bodyStr.trim().isEmpty()) {
        response = "リクエストボディが空です";
        exchange.sendResponseHeaders(400, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
        return;
      }
      try {
        // 超簡易パース: key=value&... 形式を想定
        String[] params = bodyStr.split("&");
        String teamName = "", passcode = "", editPerm = "", category = "", scope = "public";
        int maxMembers = 5;
        java.util.List<String> members = new java.util.ArrayList<>();
        for (String param : params) {
          String[] kv = param.split("=", 2);
          if (kv.length < 2) continue;
          switch (kv[0]) {
            case "teamName": teamName = java.net.URLDecoder.decode(kv[1], "UTF-8"); break;
            case "passcode": passcode = java.net.URLDecoder.decode(kv[1], "UTF-8"); break;
            case "maxMembers": maxMembers = Integer.parseInt(kv[1]); break;
            case "editPermission": editPerm = java.net.URLDecoder.decode(kv[1], "UTF-8"); break;
            case "category": category = java.net.URLDecoder.decode(kv[1], "UTF-8"); break;
            case "scope": scope = java.net.URLDecoder.decode(kv[1], "UTF-8"); break;
            case "members": for (String m : kv[1].split(",")) if (!m.isEmpty()) members.add(m); break;
          }
        }
        com.habit.domain.Room room = new com.habit.domain.Room(teamName, "creator", com.habit.domain.RoomMode.FIXED_TASK_MODE);
        room.setRoomName(teamName);
        RoomRepository repo = new RoomRepository();
        repo.save(room, passcode, maxMembers, editPerm, category, scope, members);
        response = "チーム作成成功";
      } catch (Exception ex) {
        response = "チーム作成失敗: " + ex.getMessage();
      }
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  // --- 公開チーム一覧取得API ---
  static class PublicTeamsHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
      String response;
      try {
        RoomRepository repo = new RoomRepository();
        java.util.List<String> teamNames = repo.findAllPublicTeamNames();
        response = String.join("\n", teamNames);
      } catch (Exception ex) {
        response = "エラー: " + ex.getMessage();
      }
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  // --- 合言葉でチーム検索API ---
  static class FindTeamByPasscodeHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
      String response;
      String query = exchange.getRequestURI().getQuery();
      String passcode = null;
      if (query != null && query.startsWith("passcode=")) {
        passcode = java.net.URLDecoder.decode(query.substring(9), "UTF-8");
      }
      if (passcode == null || passcode.isEmpty()) {
        response = "合言葉が指定されていません";
      } else {
        RoomRepository repo = new RoomRepository();
        String teamName = repo.findTeamNameByPasscode(passcode);
        if (teamName != null) {
          response = teamName;
        } else {
          response = "該当チームなし";
        }
      }
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  // --- チーム参加API（メンバー追加） ---
  static class JoinTeamHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
      String response;
      String query = exchange.getRequestURI().getQuery();
      String teamName = null, memberId = null;
      if (query != null) {
        String[] params = query.split("&");
        for (String param : params) {
          if (param.startsWith("teamName=")) teamName = java.net.URLDecoder.decode(param.substring(9), "UTF-8");
          if (param.startsWith("memberId=")) memberId = java.net.URLDecoder.decode(param.substring(9), "UTF-8");
        }
      }
      if (teamName == null || memberId == null || teamName.isEmpty() || memberId.isEmpty()) {
        response = "パラメータが不正です";
      } else {
        RoomRepository repo = new RoomRepository();
        boolean ok = repo.addMemberByTeamName(teamName, memberId);
        response = ok ? "参加成功" : "参加失敗";
      }
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  // --- チャット履歴取得API ---
  static class GetChatLogHandler implements HttpHandler {
    // メモリ上にチャット履歴を保持（roomIdごと）
    private static final java.util.Map<String, java.util.List<String>> chatLogMap = SendChatMessageHandler.chatLogMap;

    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      String roomId = null;
      int limit = 50;
      if (query != null) {
        String[] params = query.split("&");
        for (String param : params) {
          if (param.startsWith("roomId=")) roomId = java.net.URLDecoder.decode(param.substring(7), "UTF-8");
          if (param.startsWith("limit=")) try { limit = Integer.parseInt(param.substring(6)); } catch (Exception ignored) {}
        }
      }
      String response;
      if (roomId == null) {
        response = "[]";
      } else {
        java.util.List<String> log = chatLogMap.getOrDefault(roomId, new java.util.ArrayList<>());
        // 最新limit件だけ返す
        java.util.List<String> limited = log.size() > limit ? log.subList(log.size() - limit, log.size()) : log;
        // JSON配列形式で返す
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < limited.size(); i++) {
          sb.append(limited.get(i));
          if (i < limited.size() - 1) sb.append(",");
        }
        sb.append("]");
        response = sb.toString();
      }
      exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  // --- チャット送信API ---
  static class SendChatMessageHandler implements HttpHandler {
    // メモリ上にチャット履歴を保持（roomIdごと）
    static final java.util.Map<String, java.util.List<String>> chatLogMap = new java.util.HashMap<>();
    public void handle(HttpExchange exchange) throws IOException {
      if (!"POST".equals(exchange.getRequestMethod())) {
        String response = "POSTメソッドのみ対応";
        exchange.sendResponseHeaders(405, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
        return;
      }
      // POSTボディを取得
      byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
      String bodyStr = (bodyBytes != null) ? new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8) : "";
      String response;
      String roomId = null, senderId = null, content = null;
      if (bodyStr != null && !bodyStr.isEmpty()) {
        String[] params = bodyStr.split("&");
        for (String param : params) {
          String[] kv = param.split("=", 2);
          if (kv.length < 2) continue;
          switch (kv[0]) {
            case "roomId": roomId = java.net.URLDecoder.decode(kv[1], "UTF-8"); break;
            case "senderId": senderId = java.net.URLDecoder.decode(kv[1], "UTF-8"); break;
            case "content": content = java.net.URLDecoder.decode(kv[1], "UTF-8"); break;
          }
        }
      }
      if (roomId == null || senderId == null || content == null) {
        response = "パラメータが不正です";
        exchange.sendResponseHeaders(400, response.getBytes().length);
      } else {
        // ユーザ名を取得
        String username = senderId;
        try {
          // senderIdはユーザID。userRepositoryからfindByIdでUserを取得し、getUsername()でユーザ名を取得
          com.habit.domain.User user = userRepository.findById(senderId);
          if (user != null) {
            String uname = user.getUsername();
            if (uname != null && !uname.trim().isEmpty()) {
              username = uname;
            }
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
        // チャット履歴をメモリに保存（JSON形式で）
        String json = String.format("{\"senderId\":\"%s\",\"username\":\"%s\",\"content\":\"%s\"}",
          senderId.replace("\"","\\\""), username.replace("\"","\\\""), content.replace("\"","\\\""));
        synchronized (chatLogMap) {
          chatLogMap.computeIfAbsent(roomId, k -> new java.util.ArrayList<>()).add(json);
        }
        System.out.println("[チャット] roomId=" + roomId + ", senderId=" + senderId + ", username=" + username + ", content=" + content);
        response = "チャット送信成功";
        exchange.sendResponseHeaders(200, response.getBytes().length);
      }
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }
}