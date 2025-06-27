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
        this.userId = userId;
    }
    // チームIDのセッター
    public void setTeamID(String teamID) {
        this.teamID = teamID;
        // teamIDがセットされたタイミングでタスク、チャットを読み込む。
        loadTeamTasksAndUserTasks();
        loadChatLog();
    }
    // チーム名のセッター
    public void setTeamName(String teamName) {
        this.teamName = teamName;
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
                // HTTPリクエストを送信するためのクライアントオブジェクトを作成
                HttpClient client = HttpClient.newHttpClient();

                // 1. サーバAPIからチームのタスクID→タスク名マップを取得
                java.util.Map<String, String> idToName = new java.util.HashMap<>(); // タスクID→タスク名のマップ
                // URLを作成
                String mapUrl = "http://localhost:8080/getTaskIdNameMap?id=" + URLEncoder.encode(teamID, "UTF-8");
                // リクエストを送信
                HttpRequest mapRequest = HttpRequest.newBuilder()
                        .uri(URI.create(mapUrl))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .GET()
                        .build();
                // レスポンスを取得
                HttpResponse<String> mapResponse = client.send(mapRequest, HttpResponse.BodyHandlers.ofString());
                // レスポンスのボディをJSONとして解析し、タスクID→タスク名マップを作成
                String json = mapResponse.body();
                if (!json.isEmpty() && json.startsWith("{")) {
                    org.json.JSONObject obj = new org.json.JSONObject(json);
                    for (String key : obj.keySet()) {
                        idToName.put(key, obj.getString(key));
                    }
                }

                // 2. サーバからチームタスクID一覧取得
                // URLを作成
                String tasksUrl = "http://localhost:8080/getTasks?id=" + URLEncoder.encode(teamID, "UTF-8");
                // リクエストを送信
                HttpRequest tasksRequest = HttpRequest.newBuilder()
                        .uri(URI.create(tasksUrl))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .GET()
                        .build();
                // レスポンスを取得
                HttpResponse<String> tasksResponse = client.send(tasksRequest, HttpResponse.BodyHandlers.ofString());
                // レスポンスのボディをタスクIDのリストに変換
                java.util.List<String> teamTasks = new java.util.ArrayList<>();
                for (String line : tasksResponse.body().split("\n")) {
                    if (!line.trim().isEmpty()) {
                        teamTasks.add(line.trim());
                    }
                }

                // 3. ユーザのタスクID一覧取得
                // LoginControllerからセッションIDを取得
                String sessionId = LoginController.getSessionId();
                // URLを作成
                String userTaskUrl = "http://localhost:8080/getUserTaskIds";
                // リクエストを送信
                HttpRequest userTaskRequest = HttpRequest.newBuilder()
                        .uri(URI.create(userTaskUrl))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .header("SESSION_ID", sessionId)
                        .GET()
                        .build();
                // レスポンスを取得
                HttpResponse<String> userTaskResponse = client.send(userTaskRequest, HttpResponse.BodyHandlers.ofString());
                // レスポンスのボディをタスクIDのセットに変換
                java.util.Set<String> userTaskIds = new java.util.HashSet<>();
                String response2 = userTaskResponse.body();
                if (response2 != null && response2.startsWith("taskIds=")) {
                    String[] ids = response2.substring(8).split(",");
                    for (String id : ids) {
                        if (!id.trim().isEmpty()) {
                            userTaskIds.add(id.trim());
                        }
                    }
                }

                // 4. 両方に含まれるタスクIDのみ抽出
                java.util.List<String> filteredTasks = new java.util.ArrayList<>();
                for (String task : teamTasks) {
                    if (userTaskIds.contains(task)) {
                        // 未完了タスクのみ追加
                        com.habit.server.UserTaskStatusRepository statusRepo = new com.habit.server.UserTaskStatusRepository();
                        java.time.LocalDate today = java.time.LocalDate.now();
                        java.util.Optional<com.habit.domain.UserTaskStatus> statusOpt = statusRepo.findByUserIdAndTaskIdAndDate(userId, task, today);
                        boolean isDone = statusOpt.map(com.habit.domain.UserTaskStatus::isDone).orElse(false);
                        if (!isDone) {
                            filteredTasks.add(task);
                        }
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
            com.habit.server.TaskRepository repo = new com.habit.server.TaskRepository();
            java.util.List<com.habit.domain.Task> teamTaskObjs = repo.findTeamTasksByTeamID(teamID);
            String sessionId = LoginController.getSessionId();

            // HTTPリクエストを送信するためのクライアントオブジェクトを作成
            HttpClient client = HttpClient.newHttpClient();
            // URLを作成
            String userTaskUrl = "http://localhost:8080/getUserTaskIds";
            // リクエストを送信
            HttpRequest userTaskRequest = HttpRequest.newBuilder()
                    .uri(URI.create(userTaskUrl))
                    .timeout(java.time.Duration.ofSeconds(3))
                    .header("SESSION_ID", sessionId)
                    .GET()
                    .build();
            // レスポンスを取得
            HttpResponse<String> userTaskResponse = client.send(userTaskRequest, HttpResponse.BodyHandlers.ofString());
            // レスポンスのボディをタスクIDのセットに変換
            java.util.Set<String> userTaskIds = new java.util.HashSet<>();
            String response2 = userTaskResponse.body();
            if (response2 != null && response2.startsWith("taskIds=")) {
                String[] ids = response2.substring(8).split(",");
                for (String id : ids) {
                    if (!id.trim().isEmpty()) {
                        userTaskIds.add(id.trim());
                    }
                }
            }

            java.util.List<com.habit.domain.Task> filteredTasks = new java.util.ArrayList<>();
            for (com.habit.domain.Task t : teamTaskObjs) {
                if (userTaskIds.contains(t.getTaskId())) {
                    com.habit.server.UserTaskStatusRepository statusRepo = new com.habit.server.UserTaskStatusRepository();
                    java.time.LocalDate today = java.time.LocalDate.now();
                    java.util.Optional<com.habit.domain.UserTaskStatus> statusOpt = statusRepo.findByUserIdAndTaskIdAndDate(userId, t.getTaskId(), today);
                    boolean isDone = statusOpt.map(com.habit.domain.UserTaskStatus::isDone).orElse(false);
                    if (!isDone) {
                        filteredTasks.add(t);
                    }
                }
            }
            return filteredTasks;
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