package com.habit.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;

public class TeamTopController {
    @FXML
    private Label teamNameLabel;
    @FXML
    private Button btnBackHome;
    @FXML
    private Button btnToPersonal;
    @FXML
    private TableView<?> taskTable;
    @FXML
    private ListView<String> todayTaskList;
    @FXML
    private ListView<String> chatList;
    @FXML
    private ImageView teamCharView;

    @FXML
    public void initialize() {
        // 仮データ
        todayTaskList.getItems().addAll("タスク1", "タスク2", "タスク3");
        chatList.getItems().addAll("Alice: お疲れ様！", "Bob: 今日も頑張ろう", "Carol: タスク完了！");
        teamCharView.setImage(new Image(
            "https://raw.githubusercontent.com/google/material-design-icons/master/png/social/mood/materialicons/48dp/2x/baseline_mood_black_48dp.png", true));
        // TableViewのカラムやデータは必要に応じて追加

        btnBackHome.setOnAction(e -> {
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) btnBackHome.getScene().getWindow();
                javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/com/habit/client/gui/Home.fxml"));
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("ホーム");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    // チーム名を外部からセット
    public void setTeamName(String name) {
        if (teamNameLabel != null) {
            teamNameLabel.setText(name);
        }
    }
}