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

  private class GetJoinedTeamInfoHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String sessionId = null;
      var headers = exchange.getRequestHeaders();
      if (headers.containsKey("SESSION_ID")) {
        sessionId = headers.getFirst("SESSION_ID");
      }
      String response = "joinedTeamIds=";
      if (sessionId != null) {
        User user = authService.getUserBySession(sessionId);
        if (user != null) {
          List<String> teamIds = user.getJoinedTeamIds();
          response = "userId=" + user.getUserId();
          response += "\njoinedTeamIds=" + String.join(",", teamIds);
          // チーム名も取得
          List<String> teamNames = new ArrayList<>();
          for (String id : teamIds) {
            String name = teamRepository.findTeamNameById(id);
            if (name == null)
              name = id;
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
