package com.habit.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.time.*;
import java.util.*;

/**
 * 個人ページのコントローラークラス。
 * ユーザーのタスク一覧を表示し、タスクの完了処理を行う。
 */
public class PersonalPageController {
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

    // チームトップからタスク一覧を受け取る用
    public void setUserTasks(List<com.habit.domain.Task> tasks) {
        List<com.habit.domain.Task> filtered = new ArrayList<>();
        if (tasks != null) {
            try {
                String sessionId = com.habit.client.LoginController.getSessionId();
                java.time.LocalDate today = java.time.LocalDate.now();
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                String url = "http://localhost:8080/getUserTaskStatusList?teamID=" + java.net.URLEncoder.encode(teamID, "UTF-8")
                        + "&date=" + today.toString();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(10))
                        .header("SESSION_ID", sessionId)
                        .GET()
                        .build();
                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                String json = response.body();
                java.util.Set<String> doneTaskIds = new java.util.HashSet<>();
                if (json != null && json.startsWith("[")) {
                    org.json.JSONArray arr = new org.json.JSONArray(json);
                    for (int i = 0; i < arr.length(); i++) {
                        org.json.JSONObject obj = arr.getJSONObject(i);
                        String taskId = obj.optString("taskId", null);
                        boolean isDone = obj.optBoolean("isDone", false);
                        if (isDone && taskId != null) {
                            doneTaskIds.add(taskId);
                        }
                    }
                }
                for (com.habit.domain.Task t : tasks) {
                    if (!doneTaskIds.contains(t.getTaskId())) {
                        filtered.add(t);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 失敗時は全件表示
                filtered.addAll(tasks);
            }
        }
        this.tasks = filtered;
        updateTaskTiles();
    }

    // 遷移元からセットする
    private String userId;
    private String teamID;
    private String teamName = "チーム名未取得";

    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setTeamID(String teamID) {
        this.teamID = teamID;
    }
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    /**
     * コントローラー初期化処理。
     */
    @FXML
    public void initialize() { 
        updateTaskTiles();
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
            Button tileBtn = new Button();
            tileBtn.setStyle("-fx-border-color: #aaa; -fx-padding: 30; -fx-background-color: #f9f9f9; -fx-min-width: 320px; -fx-min-height: 150px; -fx-alignment: center; -fx-font-size: 22px; -fx-font-weight: bold;");
            String name = task.getTaskName();
            java.time.LocalTime dueTime = task.getDueTime();
            java.time.LocalDate dueDate = task.getDueDate();
            String remainStr = getRemainingTimeAndDaysString(dueTime, dueDate);
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
                            System.err.println("エラー: userIdが未設定です。タスク完了処理を中止します。");
                            return;
                        }
                        String taskId = task.getTaskId();
                        LocalDate date = LocalDate.now();
                        String sessionId = com.habit.client.LoginController.getSessionId();
                        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                        String url = "http://localhost:8080/completeUserTask";
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
                        System.out.println("タスク完了APIレスポンス: " + response.body());
                        // 個人ページのタイル一覧を再読み込み
                        this.tasks = fetchUserTasksForPersonalPage();
                        updateTaskTiles();
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("タスク選択: " + name);
                }
            });
            taskTilePane.getChildren().add(tileBtn);
        }
    }

    // LocalTime（本日分の締切時刻）までの残り時間を表示（従来版）
    private String getRemainingTimeString(java.time.LocalTime dueTime) {
        java.time.LocalTime now = java.time.LocalTime.now();
        if (dueTime.isBefore(now)) return "期限切れ";
        long totalMinutes = java.time.Duration.between(now, dueTime).toMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format("%d時間%d分", hours, minutes);
    }

    // 期限日付と時刻の両方を考慮した残り時間・日数表示
    private String getRemainingTimeAndDaysString(java.time.LocalTime dueTime, java.time.LocalDate dueDate) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        
        // 期限日付が設定されていない場合は従来通り時間のみ表示
        if (dueDate == null) {
            if (dueTime == null) return "";
            return "残り: " + getRemainingTimeString(dueTime);
        }
        
        // 期限日付が設定されている場合
        java.time.LocalDateTime deadline = dueDate.atTime(dueTime != null ? dueTime : java.time.LocalTime.of(23, 59));
        java.time.LocalDateTime nowDateTime = today.atTime(now);
        
        if (nowDateTime.isAfter(deadline)) {
            return "期限切れ";
        }
        
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate);
        
        if (totalDays > 0) {
            // 1日以上残っている場合は日数を表示
            return String.format("残り: %d日", totalDays);
        } else if (totalDays == 0) {
            // 当日の場合は時間を表示
            if (dueTime != null) {
                return "残り: " + getRemainingTimeString(dueTime);
            } else {
                return "残り: 本日中";
            }
        } else {
            return "期限切れ";
        }
    }
    // タスク一覧をAPIから取得する（TeamTopControllerのgetUserTasksForPersonalPage()相当）
    private List<com.habit.domain.Task> fetchUserTasksForPersonalPage() {
        try {
            String sessionId = com.habit.client.LoginController.getSessionId();
            java.time.LocalDate today = java.time.LocalDate.now();
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String url = "http://localhost:8080/getUserIncompleteTasks?teamID=" + java.net.URLEncoder.encode(teamID, "UTF-8")
                       + "&date=" + today.toString();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .header("SESSION_ID", sessionId)
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
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
                    if (dueTimeStr != null && !dueTimeStr.isEmpty() && !"null".equals(dueTimeStr)) {
                        try {
                            dueTime = java.time.LocalTime.parse(dueTimeStr);
                        } catch (Exception ignore) {}
                    }
                    String dueDateStr = obj.optString("dueDate", null);
                    java.time.LocalDate dueDate = null;
                    if (dueDateStr != null && !dueDateStr.isEmpty() && !"null".equals(dueDateStr)) {
                        try {
                            dueDate = java.time.LocalDate.parse(dueDateStr);
                        } catch (Exception ignore) {}
                    }
                    
                    if (taskId != null && taskName != null) {
                        com.habit.domain.Task t = new com.habit.domain.Task(taskId, taskName);
                        // dueTimeとdueDateをリフレクションでセット
                        try {
                            java.lang.reflect.Field f = t.getClass().getDeclaredField("dueTime");
                            f.setAccessible(true);
                            f.set(t, dueTime);
                            
                            java.lang.reflect.Field f2 = t.getClass().getDeclaredField("dueDate");
                            f2.setAccessible(true);
                            f2.set(t, dueDate);
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
}