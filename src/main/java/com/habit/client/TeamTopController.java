package com.habit.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import java.net.*;
import java.io.*;
import java.util.*;
import org.json.*;

public class TeamTopController {
    @FXML
    private Label teamNameLabel;

    // ユーザーID保持用
    private String userId;

    // userIdのsetter
    public void setUserId(String userId) {
        this.userId = userId;
        System.out.println("TeamTopController: userIdを受け取りました: " + userId);
    }
    @FXML
    private Button btnBackHome;
    @FXML
    private Button btnToPersonal;
    @FXML
    private Button btnToChat; // チャットページへ遷移するボタンを追加
    @FXML
    private TableView<?> taskTable;
    @FXML
    private Button btnCreateTask;
    @FXML
    private ListView<String> todayTaskList;
    @FXML
    private ListView<String> chatList;
    @FXML
    private ImageView teamCharView;

    // セッションID保持用
    private String sessionID;

    private final String serverUrl = "http://localhost:8080/sendChatMessage";
    private final String chatLogUrl = "http://localhost:8080/getChatLog";
    private String teamID = "team1"; // 実際は動的に設定

    public void setTeamID(String teamID) {
        this.teamID = teamID;
        // teamIDがセットされたタイミングで初期化処理を呼ぶ
        loadTeamTasksAndUserTasks();
        loadChatLog();
    }

    // セッションIDのsetter
    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    // チームタスク・ユーザタスク取得＆フィルタ処理（タスク名表示対応）
    private void loadTeamTasksAndUserTasks() {
        new Thread(() -> {
            try {
                // 1. チームのタスク一覧取得（taskId→taskNameマップ作成）
                com.habit.server.TaskRepository repo = new com.habit.server.TaskRepository();
                java.util.List<com.habit.domain.Task> teamTaskObjs = repo.findTeamTasksByteamID(teamID);
                java.util.Map<String, String> idToName = new java.util.HashMap<>();
                for (com.habit.domain.Task t : teamTaskObjs) {
                    idToName.put(t.getTaskId(), t.getTaskName());
                }

                // 2. サーバからチームタスクID一覧取得（従来通り）
                URL url = new URL("http://localhost:8080/getTasks?id=" + URLEncoder.encode(teamID, "UTF-8"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                java.util.List<String> teamTasks = new java.util.ArrayList<>();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (!line.trim().isEmpty()) {
                            teamTasks.add(line.trim());
                        }
                    }
                }

                // 3. ユーザのタスクID一覧取得
                String sessionId = LoginController.getSessionId();
                URL url2 = new URL("http://localhost:8080/getUserTaskIds");
                HttpURLConnection conn2 = (HttpURLConnection) url2.openConnection();
                conn2.setRequestMethod("GET");
                conn2.setRequestProperty("SESSION_ID", sessionId);
                conn2.setConnectTimeout(3000);
                conn2.setReadTimeout(3000);
                java.util.Set<String> userTaskIds = new java.util.HashSet<>();
                try (BufferedReader in2 = new BufferedReader(new InputStreamReader(conn2.getInputStream(), "UTF-8"))) {
                    String response2 = in2.readLine();
                    if (response2 != null && response2.startsWith("taskIds=")) {
                        String[] ids = response2.substring(8).split(",");
                        for (String id : ids) {
                            if (!id.trim().isEmpty()) {
                                userTaskIds.add(id.trim());
                            }
                        }
                    }
                }

                // 4. 両方に含まれるタスクIDのみ抽出
                java.util.List<String> filteredTasks = new java.util.ArrayList<>();
                for (String task : teamTasks) {
                    if (userTaskIds.contains(task)) {
                        filteredTasks.add(task);
                    }
                }

                // 5. タスクID→タスク名変換
                java.util.List<String> filteredTaskNames = new java.util.ArrayList<>();
                for (String id : filteredTasks) {
                    String name = idToName.get(id);
                    if (name != null) {
                        filteredTaskNames.add(name);
                    } else {
                        filteredTaskNames.add(id); // 名前が取得できなければID表示
                    }
                }

                // 6. UIスレッドで表示
                Platform.runLater(() -> {
                    todayTaskList.getItems().setAll(filteredTaskNames);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    public String getTeamID() {
        return teamID;
    }

    @FXML
    public void initialize() {
        // UI部品の初期化のみ行う（データ取得はsetTeamIDで行う）
        teamCharView.setImage(new Image(
            "https://raw.githubusercontent.com/google/material-design-icons/master/png/social/mood/materialicons/48dp/2x/baseline_mood_black_48dp.png", true));

        btnBackHome.setOnAction(e -> {
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) btnBackHome.getScene().getWindow();
                javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/com/habit/client/gui/Home.fxml"));
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("ホーム");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        btnToPersonal.setOnAction(e -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/PersonalPage.fxml"));
                javafx.scene.Parent root = loader.load();
                PersonalPageController controller = loader.getController();
                // チームIDを渡す
                controller.setTeamID(teamID);
                // ユーザーのタスク一覧を渡す
                controller.setUserTasks(getUserTasksForPersonalPage());
                // セッションIDも渡す
                controller.setSessionID(LoginController.getSessionId());
                javafx.stage.Stage stage = (javafx.stage.Stage) btnToPersonal.getScene().getWindow();
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("個人ページ");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        btnToChat.setOnAction(e -> {
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) btnToChat.getScene().getWindow();
                javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/com/habit/client/gui/Chat.fxml"));
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("チームチャット");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        btnCreateTask.setOnAction(e -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/TaskCreate.fxml"));
                javafx.scene.Parent root = loader.load();
                // コントローラにteamIDを渡す
                TaskCreateController controller = loader.getController();
                controller.setTeamID(teamID);
                javafx.stage.Stage stage = (javafx.stage.Stage) btnCreateTask.getScene().getWindow();
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("タスク作成");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    // ユーザーのタスク一覧を返す（todayTaskListの内容からTaskオブジェクトを取得する例）
    // 必要に応じてキャッシュやフィールドに保持しておく
    // ここでは簡易的に再取得する例
    private java.util.List<com.habit.domain.Task> getUserTasksForPersonalPage() {
        try {
            com.habit.server.TaskRepository repo = new com.habit.server.TaskRepository();
            java.util.List<com.habit.domain.Task> teamTaskObjs = repo.findTeamTasksByteamID(teamID);
            String sessionId = LoginController.getSessionId();
            java.net.URL url2 = new java.net.URL("http://localhost:8080/getUserTaskIds");
            java.net.HttpURLConnection conn2 = (java.net.HttpURLConnection) url2.openConnection();
            conn2.setRequestMethod("GET");
            conn2.setRequestProperty("SESSION_ID", sessionId);
            conn2.setConnectTimeout(3000);
            conn2.setReadTimeout(3000);
            java.util.Set<String> userTaskIds = new java.util.HashSet<>();
            try (java.io.BufferedReader in2 = new java.io.BufferedReader(new java.io.InputStreamReader(conn2.getInputStream(), "UTF-8"))) {
                String response2 = in2.readLine();
                if (response2 != null && response2.startsWith("taskIds=")) {
                    String[] ids = response2.substring(8).split(",");
                    for (String id : ids) {
                        if (!id.trim().isEmpty()) {
                            userTaskIds.add(id.trim());
                        }
                    }
                }
            }
            java.util.List<com.habit.domain.Task> filteredTasks = new java.util.ArrayList<>();
            for (com.habit.domain.Task t : teamTaskObjs) {
                if (userTaskIds.contains(t.getTaskId())) {
                    filteredTasks.add(t);
                }
            }
            return filteredTasks;
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    // サーバーからチャットログを取得して最新3件を表示
    private void loadChatLog() {
        new Thread(() -> {
            try {
                URL url = new URL(chatLogUrl + "?teamID=" + URLEncoder.encode(teamID, "UTF-8") + "&limit=3");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                List<String> messages = new ArrayList<>();
                JSONArray arr = new JSONArray(response.toString());
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String username = obj.optString("username", null);
                    String sender = username != null && !username.isEmpty() ? username : obj.optString("senderId", "unknown");
                    String content = obj.optString("content", "");
                    messages.add(sender + ": " + content);
                }

                Platform.runLater(() -> {
                    chatList.getItems().setAll(messages);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // サーバーにチャットメッセージを送信
    private void sendChatMessage(String message) {
        new Thread(() -> {
            try {
                URL url = new URL(serverUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                String params = "senderId=user1&teamID=" + URLEncoder.encode(teamID, "UTF-8") + "&content=" + URLEncoder.encode(message, "UTF-8");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(params.getBytes("UTF-8"));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    loadChatLog();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // チーム名を外部からセット
    public void setTeamName(String name) {
        if (teamNameLabel != null) {
            teamNameLabel.setText(name);
        }
    }
}