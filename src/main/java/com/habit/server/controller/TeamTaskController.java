package com.habit.server.controller;

import com.habit.server.service.TeamTaskService;
import com.habit.server.repository.TaskRepository;
import com.habit.server.repository.TeamRepository;
import com.habit.server.repository.UserTaskStatusRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * チーム共通タスク関連APIのコントローラ
 */
public class TeamTaskController {
    private final TeamTaskService teamTaskService;

    public TeamTaskController() {
        TaskRepository taskRepository = new TaskRepository();
        TeamRepository teamRepository = new TeamRepository();
        UserTaskStatusRepository userTaskStatusRepository = new UserTaskStatusRepository();
        this.teamTaskService = new TeamTaskService(taskRepository, teamRepository, userTaskStatusRepository);
    }

    public HttpHandler getTeamTaskCompletionRateHandler() {
        return new TeamTaskCompletionRateHandler();
    }

    public HttpHandler getUserTeamTasksHandler() {
        return new GetUserTeamTasksHandler();
    }

    /**
     * チーム共通タスクの完了率取得API
     */
    class TeamTaskCompletionRateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String teamId = null;
            String taskId = null;
            String dateStr = null;

            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] kv = param.split("=", 2);
                    if (kv.length < 2) continue;
                    try {
                        switch (kv[0]) {
                            case "teamId":
                                teamId = java.net.URLDecoder.decode(kv[1], "UTF-8");
                                break;
                            case "taskId":
                                taskId = java.net.URLDecoder.decode(kv[1], "UTF-8");
                                break;
                            case "date":
                                dateStr = java.net.URLDecoder.decode(kv[1], "UTF-8");
                                break;
                        }
                    } catch (Exception e) {
                        System.err.println("パラメータ解析エラー: " + e.getMessage());
                    }
                }
            }

            String response;
            try {
                if (teamId == null || taskId == null) {
                    response = "{\"error\":\"teamIdとtaskIdが必要です\"}";
                } else {
                    LocalDate date = (dateStr != null) ? LocalDate.parse(dateStr) : LocalDate.now();
                    double completionRate = teamTaskService.getTeamTaskCompletionRate(teamId, taskId, date);
                    response = String.format("{\"completionRate\":%.2f}", completionRate);
                }
            } catch (Exception e) {
                response = "{\"error\":\"" + e.getMessage() + "\"}";
            }

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes("UTF-8"));
            os.close();
        }
    }

    /**
     * ユーザーのチーム共通タスク一覧取得API
     */
    class GetUserTeamTasksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String userId = null;
            String dateStr = null;

            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] kv = param.split("=", 2);
                    if (kv.length < 2) continue;
                    try {
                        switch (kv[0]) {
                            case "userId":
                                userId = java.net.URLDecoder.decode(kv[1], "UTF-8");
                                break;
                            case "date":
                                dateStr = java.net.URLDecoder.decode(kv[1], "UTF-8");
                                break;
                        }
                    } catch (Exception e) {
                        System.err.println("パラメータ解析エラー: " + e.getMessage());
                    }
                }
            }

            String response;
            try {
                if (userId == null) {
                    response = "{\"error\":\"userIdが必要です\"}";
                } else {
                    LocalDate date = (dateStr != null) ? LocalDate.parse(dateStr) : LocalDate.now();
                    List<com.habit.domain.UserTaskStatus> teamTasks = 
                        teamTaskService.getUserTeamTaskStatuses(userId, date);
                    
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < teamTasks.size(); i++) {
                        com.habit.domain.UserTaskStatus status = teamTasks.get(i);
                        sb.append("{");
                        sb.append("\"userId\":\"").append(status.getUserId()).append("\",");
                        sb.append("\"taskId\":\"").append(status.getTaskId()).append("\",");
                        sb.append("\"teamId\":\"").append(status.getTeamId()).append("\",");
                        sb.append("\"date\":\"").append(status.getDate().toString()).append("\",");
                        sb.append("\"isDone\":").append(status.isDone());
                        sb.append("}");
                        if (i < teamTasks.size() - 1) sb.append(",");
                    }
                    sb.append("]");
                    response = sb.toString();
                }
            } catch (Exception e) {
                response = "{\"error\":\"" + e.getMessage() + "\"}";
            }

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes("UTF-8"));
            os.close();
        }
    }
}