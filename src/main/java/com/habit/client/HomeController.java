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
    }
}