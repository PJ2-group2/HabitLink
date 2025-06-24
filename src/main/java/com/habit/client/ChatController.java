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
    private ListView<String> chatList;
    @FXML
    private TextField chatInput;
    @FXML
    private Button btnSend;
    @FXML
    private Button btnBackToTeamTop; // チームトップに戻るボタン

    private final String serverUrl = "http://localhost:8080/sendChatMessage";
    private final String chatLogUrl = "http://localhost:8080/getChatLog";
    private final String roomId = "team1"; // 実際は動的に設定
    private final String userId = "user1"; // 実際はログインユーザーID

    @FXML
    public void initialize() {
        loadChatLog();

        btnSend.setOnAction(e -> {
            String msg = chatInput.getText();
            if (msg != null && !msg.isEmpty()) {
                sendChatMessage(msg);
                chatInput.clear();
            }
        });

        btnBackToTeamTop.setOnAction(e -> {
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) btnBackToTeamTop.getScene().getWindow();
                javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/com/habit/client/gui/TeamTop.fxml"));
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
                URL url = new URL(chatLogUrl + "?roomId=" + URLEncoder.encode(roomId, "UTF-8") + "&limit=50");
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

    private void sendChatMessage(String message) {
        new Thread(() -> {
            try {
                URL url = new URL(serverUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                String params = "senderId=" + URLEncoder.encode(userId, "UTF-8")
                        + "&roomId=" + URLEncoder.encode(roomId, "UTF-8")
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