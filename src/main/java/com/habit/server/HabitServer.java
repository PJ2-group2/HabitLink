package com.habit.server;

// 習慣化共有プログラムのサーバ側プログラム
// クライアントからのHTTPリクエストを受けて、ルームやタスクの情報を管理します
// サーバはメモリ上でルーム・タスク情報を保持します

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class HabitServer {
  public static void main(String[] args) throws Exception {
    // サーバを8080番ポートで起動
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
    // 各APIエンドポイントを登録
    server.createContext("/hello", new HelloHandler());           // 動作確認用
    server.createContext("/createRoom", new CreateRoomHandler()); // ルーム作成
    server.createContext("/joinRoom", new JoinRoomHandler());     // ルーム参加
    server.createContext("/addTask", new AddTaskHandler());       // タスク追加
    server.createContext("/getTasks", new GetTasksHandler()); // タスク一覧取得
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

  // --- ルーム管理用（メモリ上でルームIDを保持）---
  private static java.util.Set<String> rooms = new java.util.HashSet<>();

  // --- ルーム作成API ---
  static class CreateRoomHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      String response;
      if (query != null && query.startsWith("id=")) {
        String roomId = query.substring(3);
        synchronized (rooms) {
          if (rooms.contains(roomId)) {
            response = "ルーム『" + roomId + "』は既に存在します。";
          } else {
            rooms.add(roomId);
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
        synchronized (rooms) {
          if (rooms.contains(roomId)) {
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

  // --- ルームごとのタスク管理（メモリ上で保持）---
  private static java.util.Map<String, java.util.List<String>> roomTasks =
      new java.util.HashMap<>();

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
          synchronized (roomTasks) {
            if (!rooms.contains(roomId)) {
              response = "ルーム『" + roomId + "』は存在しません。";
            } else {
              roomTasks.putIfAbsent(roomId, new java.util.ArrayList<>());
              roomTasks.get(roomId).add(task);
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
        synchronized (roomTasks) {
          if (!rooms.contains(roomId)) {
            response = "ルーム『" + roomId + "』は存在しません。";
          } else {
            java.util.List<String> tasks =
                roomTasks.getOrDefault(roomId, new java.util.ArrayList<>());
            response = String.join("\n", tasks);
            if (response.isEmpty())
              response = "タスクはありません。";
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
}
