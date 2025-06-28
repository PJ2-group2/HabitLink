package com.habit.server.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.habit.server.AuthService;
import com.habit.server.UserRepository;
import com.habit.server.TeamRepository;
import com.habit.domain.User;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;

public class UserController {
    private final AuthService authService;
    private final UserRepository userRepository;

    public UserController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
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
                    TeamRepository repo = new TeamRepository();
                    List<String> teamNames = new ArrayList<>();
                    for (String id : teamIds) {
                        String name = repo.findTeamNameById(id);
                        if (name == null) name = id;
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