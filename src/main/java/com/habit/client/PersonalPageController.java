package com.habit.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class PersonalPageController {
    @FXML
    private TilePane taskTilePane;
    @FXML
    private Button btnBackToTeam;

    // タスク情報クラス（仮）
    static class TaskInfo {
        String name;
        LocalDateTime deadline;
        TaskInfo(String name, LocalDateTime deadline) {
            this.name = name;
            this.deadline = deadline;
        }
    }
    // 仮のタスクデータ
    private List<TaskInfo> tasks = Arrays.asList(
        new TaskInfo("タスクA", LocalDateTime.now().plusHours(5).plusMinutes(30)),
        new TaskInfo("タスクB", LocalDateTime.now().plusHours(12)),
        new TaskInfo("タスクC", LocalDateTime.now().plusDays(1).plusMinutes(15))
    );

    @FXML
    public void initialize() {
        updateTaskTiles();
        btnBackToTeam.setOnAction(e -> {
            try {
                Stage stage = (Stage) btnBackToTeam.getScene().getWindow();
                Parent root = FXMLLoader.load(getClass().getResource("/com/habit/client/gui/TeamTopPage.fxml"));
                stage.setScene(new Scene(root));
                stage.setTitle("チームトップページ");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void updateTaskTiles() {
        taskTilePane.getChildren().clear();
        for (TaskInfo task : tasks) {
            VBox tile = new VBox(15);
            tile.setStyle("-fx-border-color: #aaa; -fx-padding: 30; -fx-background-color: #f9f9f9; -fx-min-width: 320px; -fx-min-height: 150px; -fx-alignment: center;");
            Text nameText = new Text(task.name);
            nameText.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
            Text remainText = new Text("残り: " + getRemainingTimeString(task.deadline));
            remainText.setStyle("-fx-font-size: 18px;");
            tile.getChildren().addAll(nameText, remainText);
            taskTilePane.getChildren().add(tile);
        }
    }

    private String getRemainingTimeString(LocalDateTime deadline) {
        LocalDateTime now = LocalDateTime.now();
        if (deadline.isBefore(now)) return "期限切れ";
        long totalMinutes = ChronoUnit.MINUTES.between(now, deadline);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format("%d時間%d分", hours, minutes);
    }
}