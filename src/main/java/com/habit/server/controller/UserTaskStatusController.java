package com.habit.server.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.habit.server.AuthService;
import com.habit.server.UserTaskStatusRepository;
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
}