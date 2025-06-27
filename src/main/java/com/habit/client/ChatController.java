package com.habit.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import java.net.*;
import java.util.*;
import org.json.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ChatController {
    /* チーム名ラベル */
    @FXML
    private Label teamNameLabel;
    /* チャットリスト */
    @FXML
    private ListView<String> chatList;
    /* チャット入力フィールド */
    @FXML
    private TextField chatInput;
    /* チャット送信ボタン */
    @FXML
    private Button btnSend;
    /* チームトップに戻るボタン */
    @FXML
    private Button btnBackToTeamTop;

    private final String serverUrl = "http://localhost:8080/sendChatMessage";
    private final String chatLogUrl = "http://localhost:8080/getChatLog";

    // 遷移時に渡すデータとセッター
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


    /**
     * チームIDに基づいてサーバーからチーム名を取得し、ラベルに設定するメソッド。
     * チームIDがセットされたタイミングで呼び出される。
     */
    private void fetchAndSetTeamName(String teamID) {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                String urlStr = "http://localhost:8080/getTeamName?teamID=" + URLEncoder.encode(teamID, "UTF-8");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlStr))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String name = response.body();
                if (name != null && !name.isEmpty()) {
                    javafx.application.Platform.runLater(() -> {
                        teamName = name;
                        if (teamNameLabel != null) {
                            teamNameLabel.setText(teamName);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * コントローラー初期化処理。
     * チーム名の設定やボタンのアクション設定を行う。
     */
    @FXML
    public void initialize() {
        // loadChatLog()はここで呼ばない
        // チーム名がセットされている場合はラベルに表示
        if (teamNameLabel != null && teamName != null) {
            teamNameLabel.setText(teamName);
        }

        // チャット送信ボタンのアクション設定
        btnSend.setOnAction(_ -> {
            String msg = chatInput.getText();
            if (msg != null && !msg.isEmpty()) {
                sendChatMessage(msg);
                chatInput.clear();
            }
        });

        // チームトップに戻るボタンのアクション設定
        btnBackToTeamTop.setOnAction(_ -> {
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

    /**
     * チャットログをサーバーから取得し、リストに表示するメソッド。
     * 新しいスレッドで実行される。
     */
    private void loadChatLog() {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                String url = chatLogUrl + "?teamID=" + URLEncoder.encode(teamID, "UTF-8") + "&limit=50";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                List<String> messages = new ArrayList<>();
                JSONArray arr = new JSONArray(response.body());
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

    /**
     * チャットメッセージをサーバーに送信するメソッド。
     * 新しいスレッドで実行される。
     *
     * @param message 送信するチャットメッセージ
     */
    private void sendChatMessage(String message) {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                String params = "senderId=" + URLEncoder.encode(userId, "UTF-8")
                        + "&teamID=" + URLEncoder.encode(teamID, "UTF-8")
                        + "&content=" + URLEncoder.encode(message, "UTF-8");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(serverUrl))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(params))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    loadChatLog();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}