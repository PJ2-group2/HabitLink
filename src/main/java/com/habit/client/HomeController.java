package com.habit.client;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    /* 応援セリフ表示用ラベル */
    @FXML
    private Label cheerMessageLabel;

    // チーム名→IDのマップ
    private java.util.Map<String, String> teamNameToIdMap = new java.util.HashMap<>();

    // 遷移元からセットする
    private String userId;

    /**
     * コントローラー初期化処理。
     * チーム一覧の取得や、ボタンのアクション設定を行う。
     */
    @FXML
    public void initialize() {
        // 現在ログインユーザのjoinedTeamIdsにあるチームのみ表示
        try {
            // HTTPリクエストを送信するためのクライアントオブジェクトを作成。
            HttpClient client = HttpClient.newHttpClient();
            // URLを作成
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/getJoinedTeamInfo"))
                .GET();
            // セッションIDをヘッダに付与
            String sessionId = LoginController.getSessionId();
            if (sessionId != null && !sessionId.isEmpty()) {
                reqBuilder.header("SESSION_ID", sessionId);
            }
            // リクエストを送信
            HttpRequest request = reqBuilder.build();
            // レスポンスを受け取り、ボディを文字列として取得
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            // レスポンスのボディを解析
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
        // ここではGoogleのマテリアルアイコンを使用
        characterView.setImage(new javafx.scene.image.Image(
            "https://raw.githubusercontent.com/google/material-design-icons/master/png/social/mood/materialicons/48dp/2x/baseline_mood_black_48dp.png", true));

        // チームリストビューのクリックイベント設定
        // チーム名を選択したらチームトップへ遷移
        teamListView.setOnMouseClicked(unused -> {
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

        // チーム作成画面遷移ボタンのアクション設定
        btnToCreateTeam.setOnAction(unused -> {
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) btnToCreateTeam.getScene().getWindow();
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/CreateTeam.fxml"));
                javafx.scene.Parent root = loader.load();
                com.habit.client.CreateTeamController controller = loader.getController();
                if (userId != null) {
                    controller.setUserId(userId);
                }
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("チーム作成");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // チーム検索画面遷移ボタンのアクション設定
        btnToSearchTeam.setOnAction(unused -> {
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) btnToSearchTeam.getScene().getWindow();
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/SearchTeam.fxml"));
                javafx.scene.Parent root = loader.load();
                com.habit.client.SearchTeamController controller = loader.getController();
                if (userId != null) {
                    controller.setUserId(userId);
                }
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("チーム検索");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // 応援セリフリスト
        String[] cheers = {
            "今日も一歩前進だね！",
            "君なら絶対できるよ！",
            "小さな積み重ねが大きな力になるよ！",
            "焦らずコツコツいこう！",
            "昨日の自分を超えよう！",
            "休むのも大事、無理しないでね！",
            "一緒に頑張ろう！応援してるよ！",
            "できたことをしっかり褒めてあげて！",
            "継続は力なり、君はすごい！",
            "どんな日も君の味方だよ！"
        };
        java.util.Random rand = new java.util.Random();
        if (cheerMessageLabel != null) {
            cheerMessageLabel.setText(cheers[rand.nextInt(cheers.length)]);
        }
    }
}