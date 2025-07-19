package com.habit.client;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.habit.domain.Config;

/**
 * タスク作成画面のコントローラー
 */
public class TaskCreateController {
    private static final Logger logger = LoggerFactory.getLogger(TaskCreateController.class);
    /* タスク名入力フィールド */
    @FXML 
    private TextField taskNameField;
    /* タスクの詳細説明フィールド */
    @FXML 
    private TextField descriptionField;
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
     * コントローラー初期化メソッド。
     * ChoiceBoxの初期値設定や、ボタンのアクション設定を行う。
     */
    @FXML
    private void initialize() {
        // ChoiceBoxの選択肢をセット
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
        String dueDateStr = dueDateField != null ? dueDateField.getText() : "";
        String cycle = cycleTypeChoice.getValue();

        // 入力バリデーション（簡易）
        if (name == null || name.isEmpty()) {
            showAlert("タスク名を入力してください");
            return;
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
            // デイリータスクの場合は当日、ウィークリータスクの場合は来週の同じ曜日を設定
            if ("毎日".equals(cycle)) {
                dueDate = java.time.LocalDate.now();
            } else {
                dueDate = java.time.LocalDate.now().plusWeeks(1);
            }
        }

        // 保存処理
        String cycleType = "毎日".equals(cycle) ? "daily" : "weekly";
        com.habit.domain.Task task = new com.habit.domain.Task(
            java.util.UUID.randomUUID().toString(),
            name,
            teamID,
            description,
            dueDate,
            cycleType
        );
        // チームIDを利用して保存
        if (teamID == null || teamID.isEmpty()) {
            showAlert("チームIDが取得できません");
            return;
        }
        // ログ出力
        logger.info("タスク作成: " +
            "taskId=" + task.getTaskId() +
            ", name=" + task.getTaskName() +
            ", description=" + task.getDescription() +
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
                + "&dueDate=" + java.net.URLEncoder.encode(task.getDueDate() != null ? task.getDueDate().toString() : "", "UTF-8")
                + "&cycleType=" + java.net.URLEncoder.encode(task.getCycleType(), "UTF-8")
                + "&teamID=" + java.net.URLEncoder.encode(teamID, "UTF-8");
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Config.getServerUrl() + "/saveTask"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("タスク保存APIレスポンス: " + response.body());

            if (response.statusCode() == 200) {
                // 成功時の処理
            } else if (response.statusCode() == 409) {
                showAlert("エラー: 同じ名前のタスクが既に存在します。");
                return;
            } else {
                showAlert("タスク保存APIエラー: " + response.statusCode() + " - " + response.body());
                return;
            }
        } catch (Exception e) {
            showAlert("タスク保存中にエラーが発生しました: " + e.getMessage());
            return;
        }

        // UserTaskStatusの生成はサーバー側（TeamTaskService）で自動的に行われるため、
        // クライアント側での処理は不要

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
            controller.setCreatorId(creatorId);
            controller.setTeam(team);
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
            controller.setCreatorId(creatorId);
            controller.setTeam(team);
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