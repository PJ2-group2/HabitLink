package com.habit.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.application.Platform;
import java.net.*;
import java.io.*;
import java.util.*;
import org.json.*;

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

        // ホームへ戻るボタンのイベントハンドラ
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

        // 個人ページへ遷移するボタンのイベントハンドラ
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

        // チャットページへ遷移するボタンのイベントハンドラ
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

        // タスク作成ボタンのイベントハンドラ
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


    // チームタスク・ユーザタスク取得＆フィルタ処理
    // setTeamIDで呼び出される
    private void loadTeamTasksAndUserTasks() {
        new Thread(() -> {
            try {
                // 1. チームのタスク一覧取得（taskId→taskNameマップ作成）
                // Repositoryを設定
                com.habit.server.TaskRepository repo = new com.habit.server.TaskRepository();
                // RepositoryのメソッドによりチームIDからチーム内タスク一覧を取得
                java.util.List<com.habit.domain.Task> teamTaskObjs = repo.findTeamTasksByTeamID(teamID);
                // タスクID→タスク名のマップを作成
                java.util.Map<String, String> idToName = new java.util.HashMap<>();
                for (com.habit.domain.Task t : teamTaskObjs) {
                    idToName.put(t.getTaskId(), t.getTaskName());
                }

                // 2. サーバからチームタスクID一覧取得
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
    public String getTeamID() {
        return teamID;
    }
    // ユーザーのタスク一覧を返す（todayTaskListの内容からTaskオブジェクトを取得する例）
    // 必要に応じてキャッシュやフィールドに保持しておく
    // ここでは簡易的に再取得する例
    private java.util.List<com.habit.domain.Task> getUserTasksForPersonalPage() {
        try {
            com.habit.server.TaskRepository repo = new com.habit.server.TaskRepository();
            java.util.List<com.habit.domain.Task> teamTaskObjs = repo.findTeamTasksByTeamID(teamID);
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
}