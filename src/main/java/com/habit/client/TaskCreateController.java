package com.habit.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalTime;

public class TaskCreateController {

    private String teamID;

    public void setTeamID(String teamID) {
        this.teamID = teamID;
    }

    @FXML private TextField taskNameField;
    @FXML private TextField descriptionField;
    @FXML private TextField estimatedMinutesField;
    @FXML private ChoiceBox<String> taskTypeChoice;
    @FXML private TextField dueTimeField;
    @FXML private ChoiceBox<String> cycleTypeChoice;
    @FXML private Button btnCreate;

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

        // チームトップ画面に戻る
        try {
            javafx.stage.Stage stage = (Stage) btnCreate.getScene().getWindow();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/TeamTop.fxml"));
            javafx.scene.Parent root = loader.load();
            // チームIDを再セット
            TeamTopController controller = loader.getController();
            controller.setTeamID(teamID);
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("チームトップ");
            stage.show();
        } catch (Exception e) {
            showAlert("画面遷移に失敗しました: " + e.getMessage());
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}