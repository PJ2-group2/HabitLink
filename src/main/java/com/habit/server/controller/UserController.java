package com.habit.server.controller;

import com.habit.domain.User;
import com.habit.server.repository.TeamRepository;
import com.habit.server.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class UserController {
  private final AuthService authService;
  private final TeamRepository teamRepository;

  public UserController(AuthService authService,
                        TeamRepository teamRepository) {
    this.authService = authService;
    this.teamRepository = teamRepository;
  }

  public HttpHandler getGetJoinedTeamInfoHandler() {
    return new GetJoinedTeamInfoHandler();
  }

  public HttpHandler getSabotagePointsHandler() {
    return new GetSabotagePointsHandler();
  }

  public HttpHandler getUpdateSabotagePointsHandler() {
    return new UpdateSabotagePointsHandler();
  }

  private class GetSabotagePointsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String sessionId = null;
      var headers = exchange.getRequestHeaders();
      if (headers.containsKey("SESSION_ID")) {
        sessionId = headers.getFirst("SESSION_ID");
      }

      int sabotagePoints = 0; // Default to 0 or an appropriate error code
      if (sessionId != null) {
        User user = authService.getUserBySession(sessionId);
        if (user != null) {
          sabotagePoints = user.getSabotagePoints();
        }
      }

      String response = String.valueOf(sabotagePoints);
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  private class GetJoinedTeamInfoHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String sessionId = null;
      var headers = exchange.getRequestHeaders();
      if (headers.containsKey("SESSION_ID")) {
        sessionId = headers.getFirst("SESSION_ID");
      }

      org.json.JSONObject responseObject = new org.json.JSONObject();
      org.json.JSONArray responseArray = new org.json.JSONArray();
      String currentUserId = null;

      if (sessionId != null) {
        User user = authService.getUserBySession(sessionId);
        if (user != null) {
          currentUserId = user.getUserId();
          List<String> teamIds = user.getJoinedTeamIds();
          for (String id : teamIds) {
            com.habit.domain.Team team = teamRepository.findById(id);
            if (team != null) {
              org.json.JSONObject teamJson = new org.json.JSONObject();
              teamJson.put("teamId", team.getTeamID());
              teamJson.put("teamName", team.getteamName());
              teamJson.put("creatorId", team.getCreatorId());
              responseArray.put(teamJson);
            }
          }
        }
      }

      responseObject.put("teams", responseArray);
      responseObject.put("userId", currentUserId);

      String response = responseObject.toString();
      exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
      exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(response.getBytes("UTF-8"));
      }
    }
  }

  private class UpdateSabotagePointsHandler implements HttpHandler {
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
      String bodyStr = (bodyBytes != null) ? new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8) : "";
      String response;
      
      try {
        String[] params = bodyStr.split("&");
        String userId = null;
        int newPoints = -1;
        
        for (String param : params) {
          String[] kv = param.split("=", 2);
          if (kv.length < 2) continue;
          if (kv[0].equals("userId")) {
            userId = java.net.URLDecoder.decode(kv[1], "UTF-8");
          } else if (kv[0].equals("sabotagePoints")) {
            newPoints = Integer.parseInt(kv[1]);
          }
        }
        
        if (userId != null && newPoints >= 0) {
          com.habit.server.repository.UserRepository userRepo = new com.habit.server.repository.UserRepository();
          User user = userRepo.findById(userId);
          if (user != null) {
            int oldPoints = user.getSabotagePoints();
            user.setSabotagePoints(Math.max(0, Math.min(9, newPoints))); // 0-9の範囲に制限
            userRepo.save(user);
            response = "サボりポイント更新成功: " + user.getUsername() + " " + oldPoints + "pt → " + user.getSabotagePoints() + "pt";
          } else {
            response = "ユーザーが見つかりません";
          }
        } else {
          response = "パラメータが不正です";
        }
      } catch (Exception ex) {
        response = "サボりポイント更新失敗: " + ex.getMessage();
      }
      
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }
}
