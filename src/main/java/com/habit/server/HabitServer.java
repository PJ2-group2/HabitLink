package com.habit.server;

// 習慣化共有プログラムのサーバ側プログラム
// クライアントからのHTTPリクエストを受けて、チームやタスクの情報を管理します
// サーバはSQLiteを用いてチーム・タスク情報を永続化します

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

// JDBC based team management
import com.habit.server.DatabaseTeamManager;

/**
 * 習慣化共有アプリのサーバ本体クラス。
 * HTTPリクエストを受けてチーム・タスク・ユーザ管理など各種APIを提供する。
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
    server.createContext("/addTask", new AddTaskHandler());       // タスク追加
    server.createContext("/getTasks", new GetTasksHandler()); // タスク一覧取得
    server.createContext("/login", new LoginHandler());           // ログイン
    server.createContext("/register", new RegisterHandler());     // 新規登録
    server.createContext("/createTeam", new CreateTeamHandler());   // チーム作成
    server.createContext("/publicTeams", new PublicTeamsHandler()); // 公開チーム一覧
    server.createContext("/findTeamByPasscode", new FindTeamByPasscodeHandler()); // 合言葉検索
    server.createContext("/sendChatMessage", new SendChatMessageHandler()); // チャット送信
    server.createContext("/getChatLog", new GetChatLogHandler()); // チャット履歴取得
    server.createContext("/getUserInfo", new GetUserInfoHandler()); // ユーザ情報取得API追加
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

  // --- チーム管理用（SQLite でチームIDを保持）---
  private static DatabaseTeamManager teamManager =
      new DatabaseTeamManager("jdbc:sqlite:habit.db");

  // --- チーム作成API ---
  /**
   * チーム作成API
   * /createTeam?id=xxx で新しいチームを作成する
   */

  // --- チーム参加API ---
  /**
   * チーム参加API
   * /joinTeam?id=xxx で指定チームに参加する
   */

  // --- タスク追加API ---
  /**
   * タスク追加API
   * /addTask?id=xxx&task=yyy で指定チームにタスクを追加する
   */
  static class AddTaskHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      String response;
      if (query != null && query.contains("id=") && query.contains("task=")) {
        String[] params = query.split("&");
        String teamID = null, task = null;
        for (String param : params) {
          if (param.startsWith("id="))
            teamID = param.substring(3);
          if (param.startsWith("task="))
            task = param.substring(5);
        }
        if (teamID != null && task != null) {
          synchronized (teamManager) {
            if (!teamManager.teamExists(teamID)) {
              response = "チーム『" + teamID + "』は存在しません。";
            } else {
              var team = teamManager.getTaskManager(teamID);
              team.addTask(task);
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
   * /getTasks?id=xxx で指定チームのタスク一覧を取得する
   */
  static class GetTasksHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      String response;
      if (query != null && query.startsWith("id=")) {
        String teamID = query.substring(3);
        synchronized (teamManager) {
          if (!teamManager.teamExists(teamID)) {
            response = "チーム『" + teamID + "』は存在しません。";
          } else {
            var team = teamManager.getTaskManager(teamID);
            var tasks = team.getTasks();
            if (tasks.isEmpty())
              response = "タスクはありません。";
            else
              response = String.join("\n", tasks);
          }
        }
      } else {
        response = "チームIDが指定されていません。";
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
          // セッションIDからユーザー情報を取得し、コンソールに表示
          var user = authService.getUserBySession(sessionId);
          if (user != null) {
            System.out.println("ログインユーザ情報: userId=" + user.getUserId() +
              ", username=" + user.getUsername() +
              ", sabotagePoints=" + user.getSabotagePoints() +
              ", joinedTeamIds=" + user.getJoinedTeamIds());
          }
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
        String sessionId = authService.registerAndCreateSession(username, password);
        if (sessionId != null) {
          var user = authService.getUserBySession(sessionId);
          if (user != null) {
            System.out.println("新規登録ユーザ情報: userId=" + user.getUserId() +
              ", username=" + user.getUsername() +
              ", sabotagePoints=" + user.getSabotagePoints() +
              ", joinedTeamIds=" + user.getJoinedTeamIds());
          }
          response = "登録成功\nSESSION_ID:" + sessionId;
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
        String teamID = java.util.UUID.randomUUID().toString();
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
        com.habit.domain.Team team = new com.habit.domain.Team(teamID, teamName, "creator", com.habit.domain.TeamMode.FIXED_TASK_MODE);
        team.setteamName(teamName);
        TeamRepository repo = new TeamRepository();
        repo.save(team, passcode, maxMembers, editPerm, category, scope, members);
        
        // セッションIDから現在のユーザを取得し、チームIDを追加・DB更新
        String sessionId = null;
        // ヘッダまたはクエリからSESSION_IDを取得（例: "SESSION_ID"ヘッダを想定）
        var headers = exchange.getRequestHeaders();
        if (headers.containsKey("SESSION_ID")) {
            sessionId = headers.getFirst("SESSION_ID");
        }
        if (sessionId == null) {
            // クエリパラメータからも試行
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("SESSION_ID=")) {
                for (String param : query.split("&")) {
                    if (param.startsWith("SESSION_ID=")) {
                        sessionId = param.substring("SESSION_ID=".length());
                        break;
                    }
                }
            }
        }
        if (sessionId != null) {
            var user = authService.getUserBySession(sessionId);
            if (user != null) {
                user.addJoinedTeamId(teamID);
                System.out.println("joinedTeamIds更新: userId=" + user.getUserId() +
                  ", username=" + user.getUsername() +
                  ", joinedTeamIds=" + user.getJoinedTeamIds());
                System.out.println("save直前 joinedTeamIds=" + user.getJoinedTeamIds());
                userRepository.save(user);
            }
        }
        
        System.out.println("チーム作成: teamID=" + teamID +
          ", teamName=" + teamName +
          ", passcode=" + passcode +
          ", maxMembers=" + maxMembers +
          ", editPermission=" + editPerm +
          ", category=" + category +
          ", scope=" + scope +
          ", members=" + members);
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
        TeamRepository repo = new TeamRepository();
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
        TeamRepository repo = new TeamRepository();
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
        TeamRepository repo = new TeamRepository();
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
    // メモリ上にチャット履歴を保持（teamIDごと）
    private static final java.util.Map<String, java.util.List<String>> chatLogMap = SendChatMessageHandler.chatLogMap;

    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      String teamID = null;
      int limit = 50;
      if (query != null) {
        String[] params = query.split("&");
        for (String param : params) {
          if (param.startsWith("teamID=")) teamID = java.net.URLDecoder.decode(param.substring(7), "UTF-8");
          if (param.startsWith("limit=")) try { limit = Integer.parseInt(param.substring(6)); } catch (Exception ignored) {}
        }
      }
      String response;
      if (teamID == null) {
        response = "[]";
      } else {
        java.util.List<String> log = chatLogMap.getOrDefault(teamID, new java.util.ArrayList<>());
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
    // メモリ上にチャット履歴を保持（teamIDごと）
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
      String teamID = null, senderId = null, content = null;
      if (bodyStr != null && !bodyStr.isEmpty()) {
        String[] params = bodyStr.split("&");
        for (String param : params) {
          String[] kv = param.split("=", 2);
          if (kv.length < 2) continue;
          switch (kv[0]) {
            case "teamID": teamID = java.net.URLDecoder.decode(kv[1], "UTF-8"); break;
            case "senderId": senderId = java.net.URLDecoder.decode(kv[1], "UTF-8"); break;
            case "content": content = java.net.URLDecoder.decode(kv[1], "UTF-8"); break;
          }
        }
      }
      if (teamID == null || senderId == null || content == null) {
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
          chatLogMap.computeIfAbsent(teamID, k -> new java.util.ArrayList<>()).add(json);
        }
        System.out.println("[チャット] teamID=" + teamID + ", senderId=" + senderId + ", username=" + username + ", content=" + content);
        response = "チャット送信成功";
        exchange.sendResponseHeaders(200, response.getBytes().length);
      }
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }
  /**
   * 現在ログイン中ユーザの情報を返すAPI
   * joinedTeamIds=... の形式で返す
   */
  static class GetUserInfoHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
      String sessionId = null;
      var headers = exchange.getRequestHeaders();
      if (headers.containsKey("SESSION_ID")) {
        sessionId = headers.getFirst("SESSION_ID");
      }
      String response = "joinedTeamIds=";
      if (sessionId != null) {
        var user = authService.getUserBySession(sessionId);
        if (user != null) {
          java.util.List<String> teamIds = user.getJoinedTeamIds();
          response = "userId=" + user.getUserId();
          response += "\njoinedTeamIds=" + String.join(",", teamIds);
          // チーム名も取得
          TeamRepository repo = new TeamRepository();
          java.util.List<String> teamNames = new java.util.ArrayList<>();
          for (String id : teamIds) {
            String name = repo.findTeamNameById(id);
            if (name == null) name = id; // 名前がなければIDをそのまま
            teamNames.add(name);
          }
          response += "\njoinedTeamNames=" + String.join(",", teamNames);
        }
      }
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }
}