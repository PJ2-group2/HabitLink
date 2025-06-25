package com.habit.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

public class SearchTeamController {
    @FXML private TextField passcodeField;
    @FXML private Button btnSearch;
    @FXML private Label resultLabel;
    @FXML private Button btnJoin;
    @FXML private Button btnBackHome;

    private String foundTeamName = null;

    @FXML
    public void initialize() {
        btnJoin.setDisable(true);
        resultLabel.setText("");

        btnSearch.setOnAction(e -> {
            String passcode = passcodeField.getText().trim();
            if (passcode.isEmpty()) {
                resultLabel.setText("合言葉を入力してください");
                btnJoin.setDisable(true);
                foundTeamName = null;
                return;
            }
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/findTeamByPasscode?passcode=" + java.net.URLEncoder.encode(passcode, "UTF-8")))
                    .GET()
                    .build();
                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                String body = response.body();
                if (body != null && !body.equals("該当チームなし") && !body.contains("合言葉が指定されていません")) {
                    resultLabel.setText("見つかったチーム: " + body);
                    foundTeamName = body;
                    btnJoin.setDisable(false);
                } else {
                    resultLabel.setText("チームが見つかりません");
                    foundTeamName = null;
                    btnJoin.setDisable(true);
                }
            } catch (Exception ex) {
                resultLabel.setText("サーバ接続エラー: " + ex.getMessage());
                foundTeamName = null;
                btnJoin.setDisable(true);
            }
        });

        btnJoin.setOnAction(e -> {
            if (foundTeamName == null) return;
            // 仮のユーザID: 実際はログインユーザ名等を使う
            String memberId = System.getProperty("user.name");
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                String url = "http://localhost:8080/joinTeam?teamName=" + java.net.URLEncoder.encode(foundTeamName, "UTF-8")
                        + "&memberId=" + java.net.URLEncoder.encode(memberId, "UTF-8");
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .GET()
                    .build();
                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                String body = response.body();
                if (body.contains("参加成功")) {
                    // チームトップ画面へ遷移
                    Stage stage = (Stage) btnJoin.getScene().getWindow();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/habit/client/gui/TeamTop.fxml"));
                    Parent root = loader.load();
                    TeamTopController controller = loader.getController();
                    controller.setTeamName(foundTeamName);
                    // passcodeをteamIDとして渡す（本来はサーバからIDを取得するのが理想だが、現状はpasscodeで代用）
                    controller.setTeamID(passcodeField.getText().trim());
                    stage.setScene(new Scene(root));
                    stage.setTitle("チームトップ");
                } else {
                    resultLabel.setText("参加失敗: " + body);
                }
            } catch (Exception ex) {
                resultLabel.setText("サーバ接続エラー: " + ex.getMessage());
            }
        });

        btnBackHome.setOnAction(e -> {
            try {
                Stage stage = (Stage) btnBackHome.getScene().getWindow();
                Parent root = FXMLLoader.load(getClass().getResource("/com/habit/client/gui/Home.fxml"));
                stage.setScene(new Scene(root));
                stage.setTitle("ホーム");
            } catch (Exception ex) {
                resultLabel.setText("画面遷移エラー: " + ex.getMessage());
            }
        });
    }
}