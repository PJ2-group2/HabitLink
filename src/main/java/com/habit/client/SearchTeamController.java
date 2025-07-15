package com.habit.client;

import com.habit.domain.util.Config;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchTeamController {
    private static final Logger logger = LoggerFactory.getLogger(SearchTeamController.class);
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

    // 遷移元からセットする
    private String userId;
    private String teamID;
    private String teamName = "チーム名未取得";

    public void setUserId(String userId) {
        this.userId = userId;
        logger.info("userId set: {}", userId);
    }

    public void setTeamID(String teamID) {
        logger.info("teamID set: {}", teamID);
        this.teamID = teamID;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

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
        btnSearch.setOnAction(unused -> {
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
                    .uri(URI.create(Config.getServerUrl() + "/findTeamByPasscode?passcode=" + java.net.URLEncoder.encode(passcode, "UTF-8")))
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
        btnJoin.setOnAction(unused -> {
            if (foundTeamName == null) return;
            // 仮のユーザID: 実際はログインユーザ名等を使う
            try {
                // 合言葉を使ってチームに参加
                // HTTPクライアントを作成
                HttpClient client = HttpClient.newHttpClient();
                // リクエストURLを組み立て
                String url = Config.getServerUrl() + "/joinTeam?teamName=" + java.net.URLEncoder.encode(foundTeamName, "UTF-8");
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
                    // パスコードから正しいチームIDを取得
                    String passcode = passcodeField.getText().trim();
                    String actualTeamId = getTeamIdByPasscode(passcode);
                    
                    // チームトップ画面へ遷移
                    Stage stage = (Stage) btnJoin.getScene().getWindow();
                    // ホーム画面に遷移
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/habit/client/gui/Home.fxml"));
                    Parent root = loader.load();

                    // HomeControllerにデータを渡す必要があればここで渡します
                    // HomeController homeController = loader.getController();
                    // homeController.setUserId(userId);
                    
                    stage.setScene(new Scene(root));
                    stage.setTitle("ホーム");
                } else {
                    resultLabel.setText("参加失敗: " + body);
                }
            } catch (Exception ex) {
                resultLabel.setText("サーバ接続エラー: " + ex.getMessage());
            }
        });

        // ホームに戻るボタンのアクション設定
        btnBackHome.setOnAction(unused -> {
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

    /**
     * パスコードから正しいチームIDを取得するメソッド
     * @param passcode パスコード
     * @return チームID（取得できない場合はパスコードを返す）
     */
    private String getTeamIdByPasscode(String passcode) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String url = Config.getServerUrl() + "/getTeamIdByPasscode?passcode=" + java.net.URLEncoder.encode(passcode, "UTF-8");
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String teamId = response.body();
            if (teamId != null && !teamId.trim().isEmpty()) {
                return teamId.trim();
            }
        } catch (Exception ex) {
            logger.error("チームID取得エラー: {}", ex.getMessage(), ex);
        }
        // 取得できない場合はパスコードをフォールバックとして使用
        return passcode;
    }
}