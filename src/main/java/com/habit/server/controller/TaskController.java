package com.habit.server.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.habit.server.DatabaseTeamManager;
import java.io.IOException;
import java.io.OutputStream;

/**
 * タスク関連APIのコントローラ
 */
public class TaskController {
    private final DatabaseTeamManager teamManager;

    public TaskController(DatabaseTeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public HttpHandler getAddTaskHandler() {
        return new AddTaskHandler();
    }

    public HttpHandler getGetTasksHandler() {
        return new GetTasksHandler();
    }

    // --- タスク追加API ---
    class AddTaskHandler implements HttpHandler {
        @Override
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
    class GetTasksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response;
            try {
                String query = exchange.getRequestURI().getQuery();
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
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes("UTF-8"));
                os.close();
            } catch (Exception e) {
                String err = "サーバーエラー: " + e.getMessage();
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(500, err.getBytes("UTF-8").length);
                OutputStream os = exchange.getResponseBody();
                os.write(err.getBytes("UTF-8"));
                os.close();
                e.printStackTrace();
            }
        }
    }
   // --- タスクID→タスク名マップ取得API ---
   public HttpHandler getTaskIdNameMapHandler() {
       return new GetTaskIdNameMapHandler();
   }

   class GetTaskIdNameMapHandler implements HttpHandler {
       @Override
       public void handle(HttpExchange exchange) throws IOException {
           String query = exchange.getRequestURI().getQuery();
           String teamID = null;
           if (query != null && query.startsWith("id=")) {
               teamID = java.net.URLDecoder.decode(query.substring(3), "UTF-8");
           }
           String response;
           if (teamID == null || teamID.isEmpty()) {
               response = "{}";
           } else {
               com.habit.server.TaskRepository repo = new com.habit.server.TaskRepository();
               java.util.List<com.habit.domain.Task> tasks = repo.findTeamTasksByTeamID(teamID);
               StringBuilder sb = new StringBuilder("{");
               for (int i = 0; i < tasks.size(); i++) {
                   var t = tasks.get(i);
                   sb.append("\"").append(t.getTaskId().replace("\"", "\\\"")).append("\":");
                   sb.append("\"").append(t.getTaskName().replace("\"", "\\\"")).append("\"");
                   if (i < tasks.size() - 1) sb.append(",");
               }
               sb.append("}");
               response = sb.toString();
           }
           exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
           exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
           OutputStream os = exchange.getResponseBody();
           os.write(response.getBytes("UTF-8"));
           os.close();
       }
    }
}