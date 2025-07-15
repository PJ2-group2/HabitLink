package com.habit.client;

import com.habit.domain.util.Config;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.time.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 個人ページのコントローラークラス。
 * ユーザーのタスク一覧を表示し、タスクの完了処理を行う。
 */
public class PersonalPageController {
    private static final Logger logger = LoggerFactory.getLogger(PersonalPageController.class);
    /** ユーザーIDラベル */
    @FXML
    private Label lblUserId;
    /** タスク一覧を表示するタイルペイン */
    @FXML
    private TilePane taskTilePane;
    /** チームトップに戻るボタン */
    @FXML
    private Button btnBackToTeam;

    // タスク一覧（Task型で受け取る）
    private List<com.habit.domain.Task> tasks = new ArrayList<>();

    // チームトップからタスク一覧を受け取る用（廃止予定 - 常に最新データを取得）
    public void setUserTasks(List<com.habit.domain.Task> tasks) {
        // 渡されたタスク一覧は無視して、常に最新データをAPIから取得
        logger.info("[PersonalPageController] fetching latest data from API");
        this.tasks = fetchUserTasksForPersonalPage();
        updateTaskTiles();
    }

    // 遷移元からセットする
    private String userId;
    private String teamID;
    private String teamName = "チーム名未取得";
    private String creatorId;
    private com.habit.domain.Team team;

    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setTeamID(String teamID) {
        this.teamID = teamID;
    }
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
    public void setCreatorId(String creatorId) {
        logger.info("creatorId set: " + creatorId);
        this.creatorId = creatorId;
    }
    public void setTeam(com.habit.domain.Team team) {
        this.team = team;
    }

    /**
     * コントローラー初期化処理。
     */
    @FXML
    public void initialize() {
        // 初期化時はタイルは空のまま（setUserTasks or setTeamIDで設定される）
        // 戻るボタンのアクション設定
        btnBackToTeam.setOnAction(unused -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/habit/client/gui/TeamTop.fxml"));
                Parent root = loader.load();
                TeamTopController controller = loader.getController();
                // 保持しているteamIDを渡す
                controller.setUserId(userId);
                controller.setTeamID(teamID);
                controller.setTeamName(teamName);
                controller.setCreatorId(creatorId);
                controller.setTeam(team);
                Stage stage = (Stage) btnBackToTeam.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("チームトップページ");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    /**
     * タスクのタイルを更新するメソッド。
     * タスク一覧をクリアし、各タスクに対してボタンを生成して表示する。
     */
    private void updateTaskTiles() {
        taskTilePane.getChildren().clear();
        for (com.habit.domain.Task task : tasks) {
            java.time.LocalDate dueDate = task.getDueDate();
            java.time.LocalDate today = LocalDate.now();

            

            Button tileBtn = new Button();
            tileBtn.setStyle("-fx-border-color: #aaa; -fx-padding: 30; -fx-background-color: #f9f9f9; -fx-min-width: 320px; -fx-min-height: 150px; -fx-alignment: center; -fx-font-size: 22px; -fx-font-weight: bold;");
            String name = task.getTaskName();
            String remainStr = getRemainingTimeString(dueDate);
            tileBtn.setText(name + (remainStr.isEmpty() ? "" : "\n" + remainStr));
            tileBtn.setOnAction(unused -> {
                // 確認ダイアログを表示
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("タスク消化の確認");
                alert.setHeaderText(null);
                alert.setContentText("このタスクを消化しますか？");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    // タスク完了処理（API経由）
                    try {
                        if (userId == null || userId.isEmpty()) {
                            logger.error("エラー: userIdが未設定です。タスク完了処理を中止します。");
                            return;
                        }
                        String taskId = task.getTaskId();
                        LocalDate date = LocalDate.now();
                        String sessionId = com.habit.client.LoginController.getSessionId();
                        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                        String url = Config.getServerUrl() + "/completeUserTask";
                        String params = "userId=" + java.net.URLEncoder.encode(userId, "UTF-8") +
                                       "&taskId=" + java.net.URLEncoder.encode(taskId, "UTF-8") +
                                       "&date=" + java.net.URLEncoder.encode(date.toString(), "UTF-8");
                        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                                .uri(java.net.URI.create(url))
                                .timeout(java.time.Duration.ofSeconds(10))
                                .header("SESSION_ID", sessionId)
                                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(params))
                                .build();
                        java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                        logger.info("タスク完了APIレスポンス: {}", response.body());
                        
                        // タスク完了成功の場合のみタイル一覧を再読み込み
                        if (response.statusCode() == 200) {
                            // 少し待機してから再読み込み（即座のタスク再設定処理完了を待つ）
                            new Thread(() -> {
                                try {
                                    Thread.sleep(500); // 0.5秒待機
                                    javafx.application.Platform.runLater(() -> {
                                        try {
                                            this.tasks = fetchUserTasksForPersonalPage();
                                            updateTaskTiles();
                                            logger.info("個人ページのタスク一覧を更新しました");
                                        } catch (Exception ex) {
                                            logger.error("タスク一覧更新エラー: {}", ex.getMessage(), ex);
                                        }
                                    });
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                }
                            }).start();
                        } else {
                            logger.error("タスク完了APIエラー: statusCode={}", response.statusCode());
                        }
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    logger.info("タスク選択: {}", name);
                }
            });
            taskTilePane.getChildren().add(tileBtn);
        }
    }

    // 日付が変わるまでの残り時間を表示
    private String getRemainingTimeString(java.time.LocalDate dueDate) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        
        // 期限日付が指定されていない場合は今日として扱う
        if (dueDate == null) {
            logger.info("期限日がnullです。");
            return null;
        }
        
        LocalDateTime deadline = dueDate.atTime(LocalTime.MAX); // 期限はその日の23:59:59とする
        LocalDateTime nowDateTime = today.atTime(now);
        
        if (nowDateTime.isAfter(deadline)) return "期限切れ";
        
        long totalMinutes = java.time.Duration.between(nowDateTime, deadline).toMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format("%d時間%d分", hours, minutes);
    }

    // タスク一覧をAPIから取得する（TeamTopControllerのgetUserTasksForPersonalPage()相当）
    private List<com.habit.domain.Task> fetchUserTasksForPersonalPage() {
        try {
            String sessionId = com.habit.client.LoginController.getSessionId();
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            
            // 新しいAPIを使用
            String url = Config.getServerUrl() + "/getIncompleteUserTaskStatus?teamID=" + java.net.URLEncoder.encode(teamID, "UTF-8");
            logger.info("[PersonalPageController] Fetching user tasks from: {}", url);
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .header("SESSION_ID", sessionId)
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            String json = response.body();
            logger.info("[PersonalPageController] API response: {}", json);
            
            java.util.List<com.habit.domain.Task> tasks = new java.util.ArrayList<>();
            if (json != null && json.startsWith("[")) {
                org.json.JSONArray arr = new org.json.JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject obj = arr.getJSONObject(i);
                    String taskId = obj.optString("taskId", null);
                    String taskName = obj.optString("taskName", null);
                    String dueDateStr = obj.optString("dueDate", null);
                    java.time.LocalDate dueDate = null;
                    if (dueDateStr != null && !dueDateStr.isEmpty() && !"null".equals(dueDateStr)) {
                        try {
                            dueDate = java.time.LocalDate.parse(dueDateStr);
                        } catch (Exception ignore) {}
                    }
                    
                    if (taskId != null && taskName != null) {
                        com.habit.domain.Task t = new com.habit.domain.Task(taskId, taskName);
                        
                        // cycleTypeを設定
                        String cycleType = obj.optString("cycleType", null);
                        if (cycleType != null) {
                            try {
                                java.lang.reflect.Field f = t.getClass().getDeclaredField("cycleType");
                                f.setAccessible(true);
                                f.set(t, cycleType);
                            } catch (Exception ignore) {}
                        }
                        
                        // dueDateを設定（setterメソッドを使用）
                        if (dueDate != null) {
                            t.setDueDate(dueDate);
                        }
                        
                        logger.info("[PersonalPageController] Adding task: {} (ID: {} , dueDate: {} , cycleType: {})", taskName, taskId, dueDate, cycleType);
                        tasks.add(t);
                    }
                }
            }
            logger.info("[PersonalPageController] Total tasks returned: {}", tasks.size());
            return tasks;
        } catch (Exception e) {
            logger.error("[PersonalPageController] Error fetching tasks: {}", e.getMessage(), e);
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
}
