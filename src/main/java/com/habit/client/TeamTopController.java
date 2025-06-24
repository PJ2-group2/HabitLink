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
    private ListView<String> todayTaskList;
    @FXML
    private ListView<String> chatList;
    @FXML
    private ImageView teamCharView;

    private final String serverUrl = "http://localhost:8080/sendChatMessage";
    private final String chatLogUrl = "http://localhost:8080/getChatLog";
    private final String roomId = "team1"; // 実際は動的に設定

    @FXML
    public void initialize() {
        // 仮データ削除
        todayTaskList.getItems().addAll("タスク1", "タスク2", "タスク3");
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
    }

    // サーバーからチャットログを取得して最新3件を表示
    private void loadChatLog() {
        new Thread(() -> {
            try {
                URL url = new URL(chatLogUrl + "?roomId=" + URLEncoder.encode(roomId, "UTF-8") + "&limit=3");
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
                    String sender = obj.optString("senderId", "unknown");
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

                String params = "senderId=user1&roomId=" + URLEncoder.encode(roomId, "UTF-8") + "&content=" + URLEncoder.encode(message, "UTF-8");

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