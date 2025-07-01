package com.habit.client;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalTime;

/**
 * タスク作成画面のコントローラー
 */
public class TaskCreateController {
    /* タスク名入力フィールド */
    @FXML 
    private TextField taskNameField;
    /* タスクの詳細説明フィールド */
    @FXML 
    private TextField descriptionField;
    /* タスクの所要時間入力フィールド */
    @FXML 
    private TextField estimatedMinutesField;
    /* タスクの種類選択ボックス */
    @FXML 
    private ChoiceBox<String> taskTypeChoice;
    /* タスクの期限時刻入力フィールド */
    @FXML
    private TextField dueTimeField;
    /* タスクの期限日付入力フィールド */
    @FXML
    private TextField dueDateField;
    /* タスクの周期選択ボックス */
    @FXML
    private ChoiceBox<String> cycleTypeChoice;
    /* タスク作成ボタンとキャンセルボタン */
    @FXML 
    private Button btnCreate;
    /* キャンセルボタン */
    @FXML 
    private Button btnCancel;

    // 遷移元からセットするデータとセッター
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
     * コントローラー初期化メソッド。
     * ChoiceBoxの初期値設定や、ボタンのアクション設定を行う。
     */
    @FXML
    private void initialize() {
        // ChoiceBoxの選択肢をセット
        if (taskTypeChoice != null) {
            taskTypeChoice.getItems().setAll("共通タスク", "個人タスク");
            taskTypeChoice.getSelectionModel().selectFirst();
        }
        if (cycleTypeChoice != null) {
            cycleTypeChoice.getItems().setAll("毎日", "毎週");
            cycleTypeChoice.getSelectionModel().selectFirst();
        }
    }

    /**
     * タスク作成ボタンのアクションハンドラ。
     */
    @FXML
    private void handleBtnCreate() {
        String name = taskNameField.getText();
        String description = descriptionField != null ? descriptionField.getText() : "";
        String estimatedStr = estimatedMinutesField != null ? estimatedMinutesField.getText() : "0";
        String type = taskTypeChoice.getValue();
        String dueTimeStr = dueTimeField.getText();
        String dueDateStr = dueDateField != null ? dueDateField.getText() : "";
        String cycle = cycleTypeChoice.getValue();

        // 入力バリデーション（簡易）
        if (name == null || name.isEmpty()) {
            showAlert("タスク名を入力してください");
            return;
        }
        int estimatedMinutes = 0;
        try {
            estimatedMinutes = Integer.parseInt(estimatedStr);
        } catch (Exception e) {
            showAlert("所要時間は数字で入力してください");
            return;
        }
        LocalTime dueTime = null;
        if (dueTimeStr != null && !dueTimeStr.isEmpty()) {
            try {
                dueTime = LocalTime.parse(dueTimeStr);
            } catch (Exception e) {
                showAlert("期限時刻はHH:mm形式で入力してください");
                return;
            }
        }
        
        java.time.LocalDate dueDate = null;
        if (dueDateStr != null && !dueDateStr.isEmpty()) {
            try {
                dueDate = java.time.LocalDate.parse(dueDateStr);
            } catch (Exception e) {
                showAlert("期限日付はyyyy-MM-dd形式で入力してください");
                return;
            }
        }
        
        // デフォルトの期限日付設定（入力されていない場合）
        if (dueDate == null) {
            // デイリータスクの場合は明日、ウィークリータスクの場合は来週の同じ曜日を設定
            if ("毎日".equals(cycle)) {
                dueDate = java.time.LocalDate.now().plusDays(1);
            } else {
                dueDate = java.time.LocalDate.now().plusWeeks(1);
            }
        }

        // 保存処理
        boolean isTeamTask = "共通タスク".equals(type);
        String cycleType = "毎日".equals(cycle) ? "daily" : "weekly";
        com.habit.domain.Task task = new com.habit.domain.Task(
            java.util.UUID.randomUUID().toString(),
            name,
            description,
            estimatedMinutes,
            java.util.Collections.emptyList(), // repeatDays未入力
            isTeamTask,
            dueTime,
            dueDate,
            cycleType
        );
        // チームIDを利用して保存
        if (teamID == null || teamID.isEmpty()) {
            showAlert("チームIDが取得できません");
            return;
        }
        // ログ出力
        System.out.println("タスク作成: " +
            "taskId=" + task.getTaskId() +
            ", name=" + task.getTaskName() +
            ", description=" + task.getDescription() +
            ", estimatedMinutes=" + task.getEstimatedMinutes() +
            ", isTeamTask=" + task.isTeamTask() +
            ", dueTime=" + task.getDueTime() +
            ", dueDate=" + task.getDueDate() +
            ", cycleType=" + task.getCycleType() +
            ", teamID=" + teamID
        );
        // --- API経由でタスク保存 ---
        try {
            HttpClient client = HttpClient.newHttpClient();
            String body = "taskId=" + java.net.URLEncoder.encode(task.getTaskId(), "UTF-8")
                + "&taskName=" + java.net.URLEncoder.encode(task.getTaskName(), "UTF-8")
                + "&description=" + java.net.URLEncoder.encode(task.getDescription(), "UTF-8")
                + "&estimatedMinutes=" + task.getEstimatedMinutes()
                + "&isTeamTask=" + task.isTeamTask()
                + "&dueTime=" + java.net.URLEncoder.encode(task.getDueTime() != null ? task.getDueTime().toString() : "", "UTF-8")
                + "&dueDate=" + java.net.URLEncoder.encode(task.getDueDate() != null ? task.getDueDate().toString() : "", "UTF-8")
                + "&cycleType=" + java.net.URLEncoder.encode(task.getCycleType(), "UTF-8")
                + "&teamID=" + java.net.URLEncoder.encode(teamID, "UTF-8");
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/saveTask"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("タスク保存APIレスポンス: " + response.body());
        } catch (Exception e) {
            showAlert("タスク保存APIエラー: " + e.getMessage());
            return;
        }

        // --- ここからUserTaskStatus保存処理 ---
        try {
            String sessionId = com.habit.client.LoginController.getSessionId();
            if (sessionId != null && !sessionId.isEmpty()) {
                // HTTPクライアントを作成
                HttpClient client = HttpClient.newHttpClient();
                // リクエストを構築
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/getJoinedTeamInfo"))
                    .header("SESSION_ID", sessionId)
                    .GET()
                    .build();
                // レスポンスを受け取る
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String body = response.body();
                // サーバから "userId=..." の形式で返ることを想定
                String userId = null;
                if (body != null) {
                    for (String line : body.split("\\n")) {
                        if (line.startsWith("userId=")) {
                            userId = line.substring("userId=".length()).trim();
                        }
                    }
                }
                if (userId != null && !userId.isEmpty()) {
                    com.habit.domain.UserTaskStatus status = new com.habit.domain.UserTaskStatus(
                        userId,
                        task.getTaskId(),
                        java.time.LocalDate.now(),
                        false
                    );
                    // DB保存
                    // --- API経由でUserTaskStatus保存 ---
                    try {
                        HttpClient client2 = HttpClient.newHttpClient();
                        String body2 = "userId=" + java.net.URLEncoder.encode(userId, "UTF-8")
                            + "&taskId=" + java.net.URLEncoder.encode(task.getTaskId(), "UTF-8")
                            + "&date=" + java.net.URLEncoder.encode(java.time.LocalDate.now().toString(), "UTF-8")
                            + "&isDone=false";
                        HttpRequest request2 = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/saveUserTaskStatus"))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(body2))
                            .build();
                        HttpResponse<String> response2 = client2.send(request2, HttpResponse.BodyHandlers.ofString());
                        System.out.println("UserTaskStatus保存APIレスポンス: " + response2.body());
                    } catch (Exception ex) {
                        System.out.println("UserTaskStatus保存APIエラー: " + ex.getMessage());
                    }
                } else {
                    System.out.println("ユーザーID取得失敗: " + body);
                }
            }
        } catch (Exception e) {
            System.out.println("UserTaskStatus保存時エラー: " + e.getMessage());
        }

        // チームトップ画面に戻る
        try {
            javafx.stage.Stage stage = (Stage) btnCreate.getScene().getWindow();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/TeamTop.fxml"));
            javafx.scene.Parent root = loader.load();
            // IDを再セット
            TeamTopController controller = loader.getController();
            controller.setUserId(userId);
            controller.setTeamID(teamID);
            controller.setTeamName(teamName);
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("チームトップ");
            stage.show();
        } catch (Exception e) {
            showAlert("画面遷移に失敗しました: " + e.getMessage());
        }
    }

    @FXML
    // キャンセルボタンのハンドラ
    private void handleBtnCancel() {
        try {
            Stage stage = (Stage) btnCancel.getScene().getWindow();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/TeamTop.fxml"));
            javafx.scene.Parent root = loader.load();
            // IDを再セット
            TeamTopController controller = loader.getController();
            controller.setUserId(userId);
            controller.setTeamID(teamID);
            controller.setTeamName(teamName);
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("チームトップ");
            stage.show();
        } catch (Exception e) {
            showAlert("画面遷移に失敗しました: " + e.getMessage());
        }
    }

    /**
     * アラートダイアログを表示するヘルパーメソッド。
     * @param msg 表示するメッセージ
     */
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}