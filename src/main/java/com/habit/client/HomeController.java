package com.habit.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;

/**
 * ホーム画面のコントローラークラス。
 * チーム一覧・キャラクターの表示や、チーム作成・検索画面への遷移を担当する。
 */
public class HomeController {
    /* キャラクター画像 */
    @FXML
    private ImageView characterView;
    /* チームリストビュー */
    @FXML
    private ListView<String> teamListView;
    /* チーム作成ボタン */
    @FXML
    private Button btnToCreateTeam;
    /* チーム検索ボタン */
    @FXML
    private Button btnToSearchTeam;

    // チーム名→IDのマップ
    private java.util.Map<String, String> teamNameToIdMap = new java.util.HashMap<>();
    // ユーザーIDを保存
    private String userId;

    /**
     * コントローラー初期化処理。
     * チーム一覧の取得や、ボタンのアクション設定を行う。
     */
    @FXML
    public void initialize() {
        // 現在ログインユーザのjoinedTeamIdsにあるチームのみ表示
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest.Builder reqBuilder = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8080/getJoinedTeamInfo"))
                .GET();
            // セッションIDをヘッダに付与
            String sessionId = LoginController.getSessionId();
            if (sessionId != null && !sessionId.isEmpty()) {
                reqBuilder.header("SESSION_ID", sessionId);
            }
            java.net.http.HttpRequest request = reqBuilder.build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            teamListView.getItems().clear();
            if (body != null && !body.trim().isEmpty()) {
                // サーバから "joinedTeamIds=... \n joinedTeamNames=..." の形式で返す
                String[] lines = body.split("\\n");
                String[] teamNames = null;
                String[] teamIds = null;
                for (String line : lines) {
                    if (line.startsWith("userId=")) {
                        String id = line.substring("userId=".length());
                        if (!id.isEmpty()) {
                            userId = id.trim();
                        }
                    }
                    if (line.startsWith("joinedTeamNames=")) {
                        String joined = line.substring("joinedTeamNames=".length());
                        if (!joined.isEmpty()) {
                            teamNames = joined.split(",");
                        }
                        // userIdをログ出力
                        if (userId != null) {
                            System.out.println("HomeController: userId=" + userId);
                        }
                    }
                    if (line.startsWith("joinedTeamIds=")) {
                        String joined = line.substring("joinedTeamIds=".length());
                        if (!joined.isEmpty()) {
                            teamIds = joined.split(",");
                        }
                    }
                }
                if (teamNames != null && teamIds != null && teamNames.length == teamIds.length) {
                    for (int i = 0; i < teamNames.length; i++) {
                        String name = teamNames[i].trim();
                        String id = teamIds[i].trim();
                        if (!name.isEmpty() && !id.isEmpty()) {
                            teamListView.getItems().add(name);
                            teamNameToIdMap.put(name, id);
                        }
                    }
                } else if (teamNames != null) {
                    for (String name : teamNames) {
                        if (!name.trim().isEmpty()) {
                            teamListView.getItems().add(name.trim());
                        }
                    }
                } else if (teamIds != null) {
                    for (String id : teamIds) {
                        if (!id.trim().isEmpty()) {
                            teamListView.getItems().add(id.trim());
                            teamNameToIdMap.put(id.trim(), id.trim());
                        }
                    }
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
                    // チーム名からIDを取得して渡す
                    String teamId = teamNameToIdMap.get(selected);
                    if (teamId != null) {
                        controller.setTeamID(teamId);
                    }
                    // userIdも渡す
                    if (userId != null) {
                        controller.setUserId(userId);
                        System.out.println("HomeController: TeamTopControllerにuserIdを渡しました: " + userId);
                    }
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