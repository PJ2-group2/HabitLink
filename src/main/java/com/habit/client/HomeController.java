package com.habit.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;

public class HomeController {
    @FXML
    private ImageView characterView;
    @FXML
    private ListView<String> teamListView;
    @FXML
    private Button btnToCreateTeam;
    @FXML
    private Button btnToSearchTeam;

    @FXML
    public void initialize() {
        // 仮データ
        teamListView.getItems().addAll("チームA", "チームB", "チームC");
        // アイコン画像セット例
        characterView.setImage(new javafx.scene.image.Image(
            "https://raw.githubusercontent.com/google/material-design-icons/master/png/social/mood/materialicons/48dp/2x/baseline_mood_black_48dp.png", true));

        // チーム選択でチームトップ画面へ遷移
        teamListView.setOnMouseClicked(e -> {
            String selected = teamListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    javafx.stage.Stage stage = (javafx.stage.Stage) teamListView.getScene().getWindow();
                    javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/com/habit/client/gui/TeamTop.fxml"));
                    stage.setScene(new javafx.scene.Scene(root));
                    stage.setTitle("チームトップ");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // チーム作成画面への遷移
        btnToCreateTeam.setOnAction(e -> {
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) btnToCreateTeam.getScene().getWindow();
                javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/com/habit/client/gui/CreateTeam.fxml"));
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("チーム作成");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}