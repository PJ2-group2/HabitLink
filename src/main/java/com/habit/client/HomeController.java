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
        // 公開チーム一覧をサーバから取得
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8080/publicTeams"))
                .GET()
                .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            teamListView.getItems().clear();
            if (body != null && !body.trim().isEmpty()) {
                String[] teams = body.split("\\n");
                for (String t : teams) {
                    if (!t.trim().isEmpty()) teamListView.getItems().add(t.trim());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            teamListView.getItems().add("サーバ接続エラー");
        }

        // アイコン画像セット例
        characterView.setImage(new javafx.scene.image.Image(
            "https://raw.githubusercontent.com/google/material-design-icons/master/png/social/mood/materialicons/48dp/2x/baseline_mood_black_48dp.png", true));

        // チーム選択でチームトップ画面へ遷移
        teamListView.setOnMouseClicked(e -> {
            String selected = teamListView.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.equals("サーバ接続エラー")) {
                try {
                    // チーム名をパラメータとして渡す
                    javafx.stage.Stage stage = (javafx.stage.Stage) teamListView.getScene().getWindow();
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/TeamTop.fxml"));
                    javafx.scene.Parent root = loader.load();
                    com.habit.client.TeamTopController controller = loader.getController();
                    controller.setTeamName(selected);
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

        // チーム検索画面への遷移
        btnToSearchTeam.setOnAction(e -> {
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) btnToSearchTeam.getScene().getWindow();
                javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/com/habit/client/gui/SearchTeam.fxml"));
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("チーム検索");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}