package com.habit.server.controller;

import com.habit.domain.Team;
import com.habit.domain.TeamMode;
import com.habit.server.repository.TaskRepository;
import com.habit.server.repository.TeamRepository;
import com.habit.server.repository.UserRepository;
import com.habit.server.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * チーム関連APIのコントローラ
 */
public class TeamController {
  private final AuthService authService;
  private final UserRepository userRepository;
  private final TaskRepository taskRepository;
  private final com.habit.server.repository.TeamRepository teamRepository;
  private final com.habit.server.repository.UserTaskStatusRepository userTaskStatusRepository;
  private final com.habit.server.service.TeamTaskService teamTaskService;

  public TeamController(AuthService authService, UserRepository userRepository,
                        TaskRepository taskRepository) {
    this.authService = authService;
    this.userRepository = userRepository;
    this.taskRepository = taskRepository;
    this.teamRepository = new com.habit.server.repository.TeamRepository();
    this.userTaskStatusRepository = new com.habit.server.repository.UserTaskStatusRepository();
    this.teamTaskService = new com.habit.server.service.TeamTaskService(
        taskRepository, teamRepository, userTaskStatusRepository);
  }

  public HttpHandler getCreateTeamHandler() { return new CreateTeamHandler(); }

  public HttpHandler getJoinTeamHandler() { return new JoinTeamHandler(); }

  public HttpHandler getPublicTeamsHandler() {
    return new PublicTeamsHandler();
  }

  public HttpHandler getFindTeamByPasscodeHandler() {
    return new FindTeamByPasscodeHandler();
  }

  public HttpHandler getGetTeamNameHandler() {
    return new GetTeamNameHandler();
  }

  public HttpHandler getGetTeamMembersHandler() {
    return new GetTeamMembersHandler();
  }
  public HttpHandler getGetTeamTasksHandler() {
    return new GetTeamTasksHandler();
  }
  
  public HttpHandler getGetTeamIdByPasscodeHandler() {
    return new GetTeamIdByPasscodeHandler();
  }

  public HttpHandler getGetTeamSabotageRankingHandler() {
    return new GetTeamSabotageRankingHandler();
  }

  // --- チーム作成API ---
  class CreateTeamHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (!"POST".equals(exchange.getRequestMethod())) {
        String response = "POSTメソッドのみ対応";
        exchange.sendResponseHeaders(405, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
        return;
      }
      byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
      String bodyStr =
          (bodyBytes != null)
              ? new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8)
              : "";
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
        String[] params = bodyStr.split("&");
        String teamID = UUID.randomUUID().toString();
        String teamName = "", passcode = "", editPerm = "";
        int maxMembers = 5;
        List<String> members = new ArrayList<>();
        for (String param : params) {
          String[] kv = param.split("=", 2);
          if (kv.length < 2)
            continue;
          switch (kv[0]) {
          case "teamName":
            teamName = java.net.URLDecoder.decode(kv[1], "UTF-8");
            break;
          case "passcode":
            passcode = java.net.URLDecoder.decode(kv[1], "UTF-8");
            break;
          case "maxMembers":
            maxMembers = Integer.parseInt(kv[1]);
            break;
          case "editPermission":
            editPerm = java.net.URLDecoder.decode(kv[1], "UTF-8");
            break;
          case "members":
            for (String m : kv[1].split(","))
              if (!m.isEmpty())
                members.add(m);
            break;
          }
        }
        String creatorUserId = null;
        var headers = exchange.getRequestHeaders();
        if (headers.containsKey("SESSION_ID")) {
          String sessionId = headers.getFirst("SESSION_ID");
          var user = authService.getUserBySession(sessionId);
          if (user != null) {
            creatorUserId = user.getUserId();
          }
        }
        if (creatorUserId == null) {
          // クエリパラメータからもSESSION_IDを探す
          String query = exchange.getRequestURI().getQuery();
          if (query != null && query.contains("SESSION_ID=")) {
            for (String param : query.split("&")) {
              if (param.startsWith("SESSION_ID=")) {
                String sessionId = param.substring("SESSION_ID=".length());
                var user = authService.getUserBySession(sessionId);
                if (user != null) {
                  creatorUserId = user.getUserId();
                }
                break;
              }
            }
          }
        }
        if (creatorUserId == null)
          creatorUserId = "creator"; // フォールバック
        Team team =
            new Team(teamID, teamName, creatorUserId, TeamMode.FIXED_TASK_MODE);
        team.setteamName(teamName);
        TeamRepository repo = new TeamRepository();
        repo.save(team, passcode, maxMembers, editPerm,
                  members);

        String sessionId = null;
        if (headers.containsKey("SESSION_ID")) {
          sessionId = headers.getFirst("SESSION_ID");
        }
        if (sessionId == null) {
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
            userRepository.save(user);
          }
        }
        // JSONレスポンスでチームIDを返す
        response =
            "{\"message\":\"チーム作成成功\",\"teamId\":\"" + teamID + "\"}";
      } catch (Exception ex) {
        response = "{\"message\":\"チーム作成失敗: " + ex.getMessage() + "\"}";
      }
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  // --- チーム参加API ---
  class JoinTeamHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String response;
      String query = exchange.getRequestURI().getQuery();
      String teamName = null;
      if (query != null) {
        String[] params = query.split("&");
        for (String param : params) {
          if (param.startsWith("teamName="))
            teamName = java.net.URLDecoder.decode(param.substring(9), "UTF-8");
        }
      }
      String sessionId = null;
      var headers = exchange.getRequestHeaders();
      if (headers.containsKey("SESSION_ID")) {
        sessionId = headers.getFirst("SESSION_ID");
      }
      if (teamName == null || teamName.isEmpty() || sessionId == null) {
        response = "パラメータまたはセッションIDが不正です";
      } else {
        var user = authService.getUserBySession(sessionId);
        if (user == null) {
          response = "ユーザが見つかりません";
        } else {
          String memberId = user.getUserId();
          TeamRepository repo = new TeamRepository();
          boolean ok = repo.addMemberByTeamName(teamName, memberId);
          if (ok) {
            String teamID = repo.findTeamIdByName(teamName);
            if (teamID != null) {
              user.addJoinedTeamId(teamID);
              userRepository.save(user);
              
              // 新メンバーに既存のチーム共通タスクを自動紐づけ
              try {
                teamTaskService.createUserTaskStatusForNewMember(teamID, memberId);
                System.out.println("新メンバー " + memberId + " にチーム " + teamID + " の既存タスクを紐づけました");
              } catch (Exception e) {
                System.err.println("チーム共通タスクの自動紐づけに失敗: " + e.getMessage());
              }
            }
          }
          response = ok ? "参加成功" : "参加失敗";
        }
      }
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  // --- 公開チーム一覧取得API ---
  class PublicTeamsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String response;
      try {
        TeamRepository repo = new TeamRepository();
        List<String> teamNames = repo.findAllPublicTeamNames();
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
  class FindTeamByPasscodeHandler implements HttpHandler {
    @Override
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
  // --- チーム名取得API ---
  class GetTeamNameHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      String teamID = null;
      if (query != null && query.startsWith("teamID=")) {
        teamID = java.net.URLDecoder.decode(query.substring(7), "UTF-8");
      }
      String response;
      if (teamID == null || teamID.isEmpty()) {
        response = "";
      } else {
        TeamRepository repo = new TeamRepository();
        String name = repo.findTeamNameById(teamID);
        response = (name != null) ? name : "";
      }
      exchange.getResponseHeaders().set("Content-Type",
                                        "text/plain; charset=UTF-8");
      exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes("UTF-8"));
      os.close();
    }
  }

  // --- チームメンバー一覧取得API ---
  class GetTeamMembersHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      String teamID = null;
      if (query != null && query.contains("teamID=")) {
        for (String param : query.split("&")) {
          if (param.startsWith("teamID=")) {
            teamID = java.net.URLDecoder.decode(param.substring(7), "UTF-8");
            break;
          }
        }
      }
      String response;
      if (teamID == null || teamID.isEmpty()) {
        response = "[]";
      } else {
        TeamRepository repo = new TeamRepository();
        List<String> userIds = repo.findMemberIdsByTeamId(teamID);
        List<String> userJsons = new ArrayList<>();
        for (String uid : userIds) {
          var user = userRepository.findById(uid);
          if (user != null) {
            String username = user.getUsername();
            userJsons.add(String.format(
                "{\"userId\":\"%s\",\"username\":\"%s\"}", uid, username));
          }
        }
        response = "[" + String.join(",", userJsons) + "]";
      }
      exchange.getResponseHeaders().set("Content-Type",
                                        "application/json; charset=UTF-8");
      exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes("UTF-8"));
      os.close();
    }
  }

  // --- チームタスク一覧取得API ---
  class GetTeamTasksHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      String teamID = null;
      if (query != null && query.contains("teamID=")) {
        for (String param : query.split("&")) {
          if (param.startsWith("teamID=")) {
            teamID = java.net.URLDecoder.decode(param.substring(7), "UTF-8");
            break;
          }
        }
      }
      String response;
      if (teamID == null || teamID.isEmpty()) {
        response = "[]";
      } else {
        List<com.habit.domain.Task> tasks =
            taskRepository.findTeamTasksByTeamID(teamID);
        List<String> taskJsons = new ArrayList<>();
        for (var t : tasks) {
          String tid = t.getTaskId();
          String tname = t.getTaskName();
          String cycleType = t.getCycleType();
          taskJsons.add(String.format("{\"taskId\":\"%s\",\"taskName\":\"%s\",\"cycleType\":\"%s\"}",
                                      tid, tname, cycleType));
        }
        response = "[" + String.join(",", taskJsons) + "]";
      }
      exchange.getResponseHeaders().set("Content-Type",
                                        "application/json; charset=UTF-8");
      exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes("UTF-8"));
      os.close();
    }
  }
  // --- パスコードからチームID取得API ---
  class GetTeamIdByPasscodeHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      String passcode = null;
      if (query != null && query.startsWith("passcode=")) {
        passcode = java.net.URLDecoder.decode(query.substring(9), "UTF-8");
      }
      String response;
      if (passcode == null || passcode.isEmpty()) {
        response = "";
      } else {
        TeamRepository repo = new TeamRepository();
        String teamId = repo.findTeamIdByPasscode(passcode);
        response = (teamId != null) ? teamId : "";
      }
      exchange.getResponseHeaders().set("Content-Type",
                                        "text/plain; charset=UTF-8");

      exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes("UTF-8"));
      os.close();
    }
  }

  // --- チーム内サボりランキング取得API ---
  class GetTeamSabotageRankingHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      String teamID = null;
      if (query != null && query.contains("teamID=")) {
        for (String param : query.split("&")) {
          if (param.startsWith("teamID=")) {
            teamID = java.net.URLDecoder.decode(param.substring(7), "UTF-8");
            break;
          }
        }
      }
      String response;
      if (teamID == null || teamID.isEmpty()) {
        response = "[]";
      } else {
        TeamRepository repo = new TeamRepository();
        List<String> userIds = repo.findMemberIdsByTeamId(teamID);
        List<String> rankingJsons = new ArrayList<>();
        
        // ユーザーIDとサボりポイントのペアを作成
        List<UserSabotageInfo> userInfos = new ArrayList<>();
        for (String uid : userIds) {
          var user = userRepository.findById(uid);
          if (user != null) {
            userInfos.add(new UserSabotageInfo(uid, user.getUsername(), user.getSabotagePoints()));
          }
        }
        
        // サボりポイントの降順でソート
        userInfos.sort((a, b) -> Integer.compare(b.sabotagePoints, a.sabotagePoints));
        
        // 上位5名まで取得
        int count = Math.min(5, userInfos.size());
        for (int i = 0; i < count; i++) {
          UserSabotageInfo info = userInfos.get(i);
          String rankingJson = String.format(
            "{\"rank\":%d,\"userId\":\"%s\",\"username\":\"%s\",\"sabotagePoints\":%d}",
            i + 1, info.userId, info.username, info.sabotagePoints
          );
          rankingJsons.add(rankingJson);
        }
        
        response = "[" + String.join(",", rankingJsons) + "]";
      }
      exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
      exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes("UTF-8"));
      os.close();
    }
  }

  // サボりポイント情報を保持するヘルパークラス
  private static class UserSabotageInfo {
    public final String userId;
    public final String username;
    public final int sabotagePoints;
    
    public UserSabotageInfo(String userId, String username, int sabotagePoints) {
      this.userId = userId;
      this.username = username;
      this.sabotagePoints = sabotagePoints;
    }
  }
}
