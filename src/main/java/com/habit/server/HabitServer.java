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
    server.setExecutor(null);
    server.start();
    System.out.println("サーバが起動しました: http://localhost:8080/hello");
  }
  // --- 動作確認用API ---
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
        var user = authService.login(username, password);
        if (user != null) {
          response = "ログイン成功";
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
      String response = "";
      int status = 200;
      OutputStream os = null;
      try {
        if (!"POST".equals(exchange.getRequestMethod())) {
          response = "POSTメソッドのみ対応";
          status = 405;
          return;
        }
        byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
        String bodyStr = (bodyBytes != null) ? new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8) : "";
        if (bodyStr == null || bodyStr.trim().isEmpty()) {
          response = "リクエストボディが空です";
          status = 400;
          return;
        }
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
        status = 200;
      } catch (Exception ex) {
        ex.printStackTrace(); // 例外内容を標準エラー出力に出す
        response = "チーム作成失敗: " + ex.getMessage();
        status = 400;
      } finally {
        try {
          exchange.sendResponseHeaders(status, response.getBytes().length);
          os = exchange.getResponseBody();
          os.write(response.getBytes());
          os.close();
        } catch (Exception ignore) {}
      }
    }
  }
}
