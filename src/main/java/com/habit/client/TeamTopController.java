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

    private final String serverUrl = "http://localhost:8080/sendChatMessage";
    private final String chatLogUrl = "http://localhost:8080/getChatLog";
    private String teamID = "team1"; // 実際は動的に設定

    public void setTeamID(String teamID) {
        this.teamID = teamID;
    }
    public String getTeamID() {
        return teamID;
    }

    @FXML
    public void initialize() {
        // チームタスク・ユーザタスク取得＆フィルタ処理
        new Thread(() -> {
            try {
                // 1. チームのタスク一覧取得
                URL url = new URL("http://localhost:8080/getTasks?id=" + URLEncoder.encode(teamID, "UTF-8"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                List<String> teamTasks = new ArrayList<>();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (!line.trim().isEmpty()) {
                            teamTasks.add(line.trim());
                        }
                    }
                }

                // 2. ユーザのタスクID一覧取得
                String sessionId = LoginController.getSessionId();
                URL url2 = new URL("http://localhost:8080/getUserTaskIds");
                HttpURLConnection conn2 = (HttpURLConnection) url2.openConnection();
                conn2.setRequestMethod("GET");
                conn2.setRequestProperty("SESSION_ID", sessionId);
                conn2.setConnectTimeout(3000);
                conn2.setReadTimeout(3000);
                Set<String> userTaskIds = new HashSet<>();
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

                // 3. 両方に含まれるタスクのみ抽出
                List<String> filteredTasks = new ArrayList<>();
                for (String task : teamTasks) {
                    if (userTaskIds.contains(task)) {
                        filteredTasks.add(task);
                    }
                }

                // 4. UIスレッドで表示
                Platform.runLater(() -> {
                    todayTaskList.getItems().setAll(filteredTasks);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        teamCharView.setImage(new Image(
            "https://raw.githubusercontent.com/google/material-design-icons/master/png/social/mood/materialicons/48dp/2x/baseline_mood_black_48dp.png", true));
        // TableViewのカラムやデータは必要に応じて追加

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
                javafx.stage.Stage stage = (javafx.stage.Stage) btnToPersonal.getScene().getWindow();
                javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/com/habit/client/gui/PersonalPage.fxml"));
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

        // チャットログの初期読み込み
        loadChatLog();
        // タスク作成ボタン
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