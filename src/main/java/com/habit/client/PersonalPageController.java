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
import com.habit.server.UserTaskStatusRepository;
import com.habit.domain.UserTaskStatus;

public class PersonalPageController {
    @FXML
    private TilePane taskTilePane;
    @FXML
    private Button btnBackToTeam;

    // タスク一覧（Task型で受け取る）
    private List<com.habit.domain.Task> tasks = new ArrayList<>();

    // チームトップからタスク一覧を受け取る用
    public void setUserTasks(List<com.habit.domain.Task> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
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

    @FXML
    public void initialize() {
        updateTaskTiles();
        btnBackToTeam.setOnAction(e -> {
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

    private void updateTaskTiles() {
        taskTilePane.getChildren().clear();
        for (com.habit.domain.Task task : tasks) {
            Button tileBtn = new Button();
            tileBtn.setStyle("-fx-border-color: #aaa; -fx-padding: 30; -fx-background-color: #f9f9f9; -fx-min-width: 320px; -fx-min-height: 150px; -fx-alignment: center; -fx-font-size: 22px; -fx-font-weight: bold;");
            String name = task.getTaskName();
            java.time.LocalTime dueTime = task.getDueTime();
            String remainStr = (dueTime != null) ? "残り: " + getRemainingTimeString(dueTime) : "";
            tileBtn.setText(name + (remainStr.isEmpty() ? "" : "\n" + remainStr));
            tileBtn.setOnAction(ev -> {
                // タスク完了処理
                try {
                    if (userId == null || userId.isEmpty()) {
                        System.err.println("エラー: userIdが未設定です。タスク完了処理を中止します。");
                        return;
                    }
                    String taskId = task.getTaskId();
                    LocalDate date = LocalDate.now();
                    UserTaskStatusRepository repo = new UserTaskStatusRepository();
                    Optional<UserTaskStatus> optStatus = repo.findByUserIdAndTaskIdAndDate(userId, taskId, date);
                    UserTaskStatus status = optStatus.orElseGet(() ->
                        new UserTaskStatus(userId, taskId, date, false)
                    );
                    status.setDone(true);
                    repo.save(status);
                    System.out.println("タスク完了: userId=" + userId + ", taskId=" + taskId + ", 完了時刻=" + status.getCompletionTimestamp());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("タスク選択: " + name);
            });
            taskTilePane.getChildren().add(tileBtn);
        }
    }

    // LocalTime（本日分の締切時刻）までの残り時間を表示
    private String getRemainingTimeString(java.time.LocalTime dueTime) {
        java.time.LocalTime now = java.time.LocalTime.now();
        if (dueTime.isBefore(now)) return "期限切れ";
        long totalMinutes = java.time.Duration.between(now, dueTime).toMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format("%d時間%d分", hours, minutes);
    }
}