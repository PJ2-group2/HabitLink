package com.habit.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.application.Platform;
import java.net.*;
import java.util.*;
import org.json.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * チームトップ画面のコントローラークラス。
 * チーム名の表示、タスク作成、個人ページやチャットページへの遷移を担当する。
 */
public class TeamTopController {
    /* チーム名ラベル */
    @FXML
    private Label teamNameLabel;
    /* 戻るボタン */
    @FXML
    private Button btnBackHome;
    /* タスク作成ボタン */
    @FXML
    private Button btnCreateTask;
    /* 個人ページへ遷移するボタン */
    @FXML
    private Button btnToPersonal;
    /* チャットページへ遷移するボタン */
    @FXML
    private Button btnToChat;
    /* チームタスク一覧テーブル */
    @FXML
    private TableView<?> taskTable;
    /* 今日のタスクリスト */
    @FXML
    private ListView<String> todayTaskList;
    /* チャットログリストビュー */
    @FXML
    private ListView<String> chatList;
    /* チームキャラクター画像 */
    @FXML
    private ImageView teamCharView;

    private final String serverUrl = "http://localhost:8080/sendChatMessage";
    private final String chatLogUrl = "http://localhost:8080/getChatLog";

    /*  遷移時に渡すユーザーIDとチームID, チーム名
     * これらは全てのコントローラが持つようにしてください。
     * 余裕があったら共通化します。
     */
    private String userId;
    private String teamID;
    private String teamName = "チーム名未取得";
    // ユーザIDのセッター
    public void setUserId(String userId) {
        System.out.println("userId set: " + userId);
        this.userId = userId;
    }
    // チームIDのセッター
    public void setTeamID(String teamID) {
        this.teamID = teamID;
        System.out.println("teamID set: " + teamID);
        // teamIDがセットされたタイミングでタスク、チャットを読み込む。
        loadTeamTasksAndUserTasks();
        loadChatLog();
    }
    // チーム名のセッター
    public void setTeamName(String teamName) {
        this.teamName = teamName;
        System.out.println("teamName set: " + teamName);
        if (teamNameLabel != null) {
            teamNameLabel.setText(teamName);
        }
    }

    /**
     * コントローラー初期化処理。
     * UI部品の初期化や、ボタンのアクション設定を行う。
     */
    @FXML
    public void initialize() {
        // UI部品の初期化のみ行う（データ取得はsetTeamIDで行う）
        // googleの適当絵文字、画像を用意してPathを指定してください。
        teamCharView.setImage(new Image(
            "https://raw.githubusercontent.com/google/material-design-icons/master/png/social/mood/materialicons/48dp/2x/baseline_mood_black_48dp.png", true));

        // ホームへ戻るボタンのアクション設定
        btnBackHome.setOnAction(_ -> {
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) btnBackHome.getScene().getWindow();
                javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/com/habit/client/gui/Home.fxml"));
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("ホーム");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // 個人ページへ遷移するボタンのアクション設定
        btnToPersonal.setOnAction(_ -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/PersonalPage.fxml"));
                javafx.scene.Parent root = loader.load();
                PersonalPageController controller = loader.getController();
                // 各データを渡す
                controller.setUserId(userId);
                controller.setTeamID(teamID);
                controller.setTeamName(teamName);
                // ユーザーのタスク一覧を渡す
                controller.setUserTasks(getUserTasksForPersonalPage());
                javafx.stage.Stage stage = (javafx.stage.Stage) btnToPersonal.getScene().getWindow();
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("個人ページ");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // チャットページへ遷移するボタンのアクション設定
        btnToChat.setOnAction(_ -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/Chat.fxml"));
                javafx.scene.Parent root = loader.load();
                // ChatControllerを取得
                ChatController controller = loader.getController();
                // 各データを渡す(この処理を全ての画面遷移で行ってください。)
                controller.setUserId(userId);
                controller.setTeamID(teamID);
                controller.setTeamName(teamName);
                javafx.stage.Stage stage = (javafx.stage.Stage) btnToChat.getScene().getWindow();
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("チームチャット");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // タスク作成ボタンのアクション設定
        btnCreateTask.setOnAction(_ -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/TaskCreate.fxml"));
                javafx.scene.Parent root = loader.load();
                // TaskCreateControllerを取得
                TaskCreateController controller = loader.getController();
                // 各データを渡す
                controller.setUserId(userId);
                controller.setTeamID(teamID);
                controller.setTeamName(teamName);
                javafx.stage.Stage stage = (javafx.stage.Stage) btnCreateTask.getScene().getWindow();
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("タスク作成");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }


    /**
     * チームタスク・ユーザタスク取得メソッド
     * チームIDがセットされたタイミングで呼び出される。
     */
    private void loadTeamTasksAndUserTasks() {
        new Thread(() -> {
            try {
                // teamIDがnullの場合は処理をスキップ
                if (teamID == null) {
                    System.err.println("teamID is null, skipping loadTeamTasksAndUserTasks");
                    return;
                }
                String sessionId = LoginController.getSessionId();
                HttpClient client = HttpClient.newHttpClient();
                String url = "http://localhost:8080/getUserTeamTasks?teamID=" + URLEncoder.encode(teamID, "UTF-8");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .header("SESSION_ID", sessionId)
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                // レスポンスは [{"taskId":"...","taskName":"..."}] のJSON配列
                java.util.List<String> taskNames = new java.util.ArrayList<>();
                String json = response.body();
                if (json != null && json.startsWith("[")) {
                    org.json.JSONArray arr = new org.json.JSONArray(json);
                    for (int i = 0; i < arr.length(); i++) {
                        org.json.JSONObject obj = arr.getJSONObject(i);
                        String name = obj.optString("taskName", null);
                        if (name != null) {
                            taskNames.add(name);
                        }
                    }
                }
                Platform.runLater(() -> {
                    todayTaskList.getItems().setAll(taskNames);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * ユーザーのタスク一覧を取得するメソッド。
     * 今日のタスクリストからユーザーのタスクオブジェクトを取得する例。
     * 必要に応じてキャッシュやフィールドに保持しておくことも可能。
     * ここでは簡易的に再取得する例を示す。
     *
     * @return ユーザーのタスク一覧
     */
    private java.util.List<com.habit.domain.Task> getUserTasksForPersonalPage() {
        try {
            // teamIDがnullの場合は空のリストを返す
            if (teamID == null) {
                System.err.println("teamID is null, returning empty task list");
                return new java.util.ArrayList<>();
            }
            String sessionId = LoginController.getSessionId();
            java.time.LocalDate today = java.time.LocalDate.now();
            HttpClient client = HttpClient.newHttpClient();
            String url = "http://localhost:8080/getUserIncompleteTasks?teamID=" + URLEncoder.encode(teamID, "UTF-8")
                       + "&date=" + today.toString();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .header("SESSION_ID", sessionId)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String json = response.body();
            java.util.List<com.habit.domain.Task> tasks = new java.util.ArrayList<>();
            if (json != null && json.startsWith("[")) {
                org.json.JSONArray arr = new org.json.JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject obj = arr.getJSONObject(i);
                    String taskId = obj.optString("taskId", null);
                    String taskName = obj.optString("taskName", null);
                    String dueTimeStr = obj.optString("dueTime", null);
                    java.time.LocalTime dueTime = null;
                    if (dueTimeStr != null && !dueTimeStr.isEmpty() && !dueTimeStr.equals("null")) {
                        try {
                            dueTime = java.time.LocalTime.parse(dueTimeStr);
                        } catch (Exception ignore) {}
                    }
                    if (taskId != null && taskName != null) {
                        com.habit.domain.Task t = new com.habit.domain.Task(taskId, taskName);
                        // dueTimeをリフレクションでセット（コンストラクタが2引数しかない場合）
                        try {
                            java.lang.reflect.Field f = t.getClass().getDeclaredField("dueTime");
                            f.setAccessible(true);
                            f.set(t, dueTime);
                        } catch (Exception ignore) {}
                        tasks.add(t);
                    }
                }
            }
            return tasks;
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    /**
     * チャットログをサーバーから取得し、最新3件を表示するメソッド。
     * チームIDがセットされたタイミングで呼び出される。
     */
    private void loadChatLog() {
        new Thread(() -> {
            try {
                // teamIDがnullの場合は処理をスキップ
                if (teamID == null) {
                    System.err.println("teamID is null, skipping loadChatLog");
                    return;
                }
                // HTTPリクエストを送信するためのクライアントオブジェクトを作成
                HttpClient client = HttpClient.newHttpClient();
                // チャットログのURLを作成
                String url = chatLogUrl + "?teamID=" + URLEncoder.encode(teamID, "UTF-8") + "&limit=3";
                // リクエストを送信
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .GET()
                        .build();
                // レスポンスを取得
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                // レスポンスのボディをJSONとして解析し、メッセージリストを作成
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

    // サーバーにチャットメッセージを送信(未使用)
    private void sendChatMessage(String message) {
        new Thread(() -> {
            try {
                // teamIDがnullの場合は処理をスキップ
                if (teamID == null) {
                    System.err.println("teamID is null, skipping sendChatMessage");
                    return;
                }
                HttpClient client = HttpClient.newHttpClient();
                String params = "senderId=user1&teamID=" + URLEncoder.encode(teamID, "UTF-8") + "&content=" + URLEncoder.encode(message, "UTF-8");
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