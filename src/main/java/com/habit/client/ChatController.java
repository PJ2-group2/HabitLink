package com.habit.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import java.net.*;
import java.io.*;
import java.util.*;
import org.json.*;

public class ChatController {
    @FXML
    private Label teamNameLabel;
    @FXML
    private ListView<String> chatList;
    @FXML
    private TextField chatInput;
    @FXML
    private Button btnSend;
    @FXML
    private Button btnBackToTeamTop; // チームトップに戻るボタン

    private final String serverUrl = "http://localhost:8080/sendChatMessage";
    private final String chatLogUrl = "http://localhost:8080/getChatLog";

    // 遷移元からセットする
    private String userId;
    private String teamID;
    private String teamName = "チーム名未取得";

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setTeamID(String teamID) {
        this.teamID = teamID;
        fetchAndSetTeamName(teamID);
        loadChatLog(); // teamIDがセットされた後に履歴を取得
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
        if (teamNameLabel != null) {
            teamNameLabel.setText(teamName);
        }
    }


    private void fetchAndSetTeamName(String teamID) {
        new Thread(() -> {
            try {
                String urlStr = "http://localhost:8080/getTeamName?teamID=" + java.net.URLEncoder.encode(teamID, "UTF-8");
                java.net.URL url = new java.net.URL(urlStr);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                try (java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    String name = in.readLine();
                    if (name != null && !name.isEmpty()) {
                        javafx.application.Platform.runLater(() -> {
                            teamName = name;
                            if (teamNameLabel != null) {
                                teamNameLabel.setText(teamName);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void initialize() {
        // loadChatLog()はここで呼ばない
        if (teamNameLabel != null && teamName != null) {
            teamNameLabel.setText(teamName);
        }

        btnSend.setOnAction(e -> {
            String msg = chatInput.getText();
            if (msg != null && !msg.isEmpty()) {
                sendChatMessage(msg);
                chatInput.clear();
            }
        });

        btnBackToTeamTop.setOnAction(e -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/TeamTop.fxml"));
                javafx.scene.Parent root = loader.load();
                TeamTopController controller = loader.getController();
                controller.setUserId(userId);
                controller.setTeamID(teamID);
                controller.setTeamName(teamName);
                javafx.stage.Stage stage = (javafx.stage.Stage) btnBackToTeamTop.getScene().getWindow();
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("チームトップ");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void loadChatLog() {
        new Thread(() -> {
            try {
                URL url = new URI(chatLogUrl + "?teamID=" + URLEncoder.encode(teamID, "UTF-8") + "&limit=50").toURL();
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

    private void sendChatMessage(String message) {
        new Thread(() -> {
            try {
                URL url = new URI(serverUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                String params = "senderId=" + URLEncoder.encode(userId, "UTF-8")
                        + "&teamID=" + URLEncoder.encode(teamID, "UTF-8")
                        + "&content=" + URLEncoder.encode(message, "UTF-8");

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
}