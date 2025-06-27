package com.habit.client;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

public class SearchTeamController {
    /** 合言葉入力フィールド */
    @FXML 
    private TextField passcodeField;
    /** チーム検索ボタン */
    @FXML 
    private Button btnSearch;
    /** 検索結果表示ラベル */
    @FXML 
    private Label resultLabel;
    /** チーム参加ボタン */
    @FXML 
    private Button btnJoin;
    /** ホームに戻るボタン */
    @FXML 
    private Button btnBackHome;

    private String foundTeamName = null;

    /**
     * コントローラー初期化メソッド。
     * 合言葉入力フィールドの初期化や、ボタンのアクション設定を行う。
     */
    @FXML
    public void initialize() {
        // 初期化
        btnJoin.setDisable(true);
        resultLabel.setText("");

        // 検索ボタンのアクション設定
        btnSearch.setOnAction(_ -> {
            String passcode = passcodeField.getText().trim();
            // 合言葉が空の場合はエラーメッセージを表示
            if (passcode.isEmpty()) {
                resultLabel.setText("合言葉を入力してください");
                btnJoin.setDisable(true);
                foundTeamName = null;
                return;
            }
            try {
                // 合言葉をサーバに送信してチームを検索
                // HTTPクライアントを作成
                HttpClient client = HttpClient.newHttpClient();
                // リクエストを構築
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/findTeamByPasscode?passcode=" + java.net.URLEncoder.encode(passcode, "UTF-8")))
                    .GET()
                    .build();
                // レスポンスを取得
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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

        // 参加ボタンのアクション設定
        btnJoin.setOnAction(_ -> {
            if (foundTeamName == null) return;
            // 仮のユーザID: 実際はログインユーザ名等を使う
            try {
                // 合言葉を使ってチームに参加
                // HTTPクライアントを作成
                HttpClient client = HttpClient.newHttpClient();
                // リクエストURLを組み立て
                String url = "http://localhost:8080/joinTeam?teamName=" + java.net.URLEncoder.encode(foundTeamName, "UTF-8");
                String sessionId = com.habit.client.LoginController.getSessionId();
                // リクエストビルダーを使用してリクエストを構築
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET();
                // セッションIDをヘッダに付与
                if (sessionId != null && !sessionId.isEmpty()) {
                    reqBuilder.header("SESSION_ID", sessionId);
                }
                // リクエストをGETメソッドで送信
                HttpRequest request = reqBuilder.build();
                // レスポンスを受け取る
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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

        // ホームに戻るボタンのアクション設定
        btnBackHome.setOnAction(_ -> {
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