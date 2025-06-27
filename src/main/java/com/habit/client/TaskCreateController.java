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
        try {
            dueTime = LocalTime.parse(dueTimeStr);
        } catch (Exception e) {
            showAlert("期限時刻はHH:mm形式で入力してください");
            return;
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
            ", cycleType=" + task.getCycleType() +
            ", teamID=" + teamID
        );
        new com.habit.server.TaskRepository().saveTask(task, teamID);

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
                    new com.habit.server.UserTaskStatusRepository().save(status);
                    System.out.println("UserTaskStatus保存: userId=" + userId + ", taskId=" + task.getTaskId());
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