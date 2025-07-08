package com.habit.client;

import com.habit.domain.util.Config;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * ログイン画面のコントローラークラス。
 * ログイン・新規登録の切り替えや、サーバーとの認証処理を担当する。
 */
public class LoginController {
  /** ユーザー名入力フィールド */
  @FXML private TextField usernameField;
  /** パスワード入力フィールド */
  @FXML private PasswordField passwordField;
  /** ログイン/新規登録ボタン */
  @FXML private Button btnLogin;
  /** モード切替ボタン */
  @FXML private Button btnSwitchMode;
  /** ステータスメッセージ表示用ラベル */
  @FXML private Label loginStatusLabel;

  /** 新規登録モードかどうかのフラグ */
  private boolean isRegisterMode = false;

  /** サーバから返却されたセッションID */
  private static String sessionId = null;

  /** 現在のセッションIDを取得するメソッド */
  public static String getSessionId() { return sessionId; }

  /**
   * コントローラー初期化処理。
   * モード切替ボタン押下時の動作を設定する。
   */
  @FXML
  public void initialize() {
    // モード切替ボタンのアクション設定
    // 初期状態はログインモード
    btnSwitchMode.setOnAction(event -> {
      isRegisterMode = !isRegisterMode;
      if (isRegisterMode) {
        btnLogin.setText("新規登録");
        btnSwitchMode.setText("ログインモードへ");
      } else {
        btnLogin.setText("ログイン");
        btnSwitchMode.setText("新規登録モードへ");
      }
      loginStatusLabel.setText("");
    });
  }

  /**
   * ログイン/新規登録ボタンのアクションハンドラ。
   * ユーザー名とパスワードを取得し、サーバーへ認証リクエストを送信する。
   * 認証結果に応じて、ホーム画面へ遷移する。
   */
  @FXML
  public void handleLoginButtonAction(javafx.event.ActionEvent event) {
    // ユーザー名とパスワードの取得
    String username = usernameField.getText().trim();
    String password = passwordField.getText().trim();
    // 未入力ならエラーメッセージを表示
    if (username.isEmpty() || password.isEmpty()) {
      loginStatusLabel.setText("ユーザ名とパスワードを入力してください");
      return;
    }
    try {
      // HTTPリクエストを送信するためのクライアントオブジェクトを作成。
      HttpClient client = HttpClient.newHttpClient();

      // モードに応じてURLを切り替え、送信先URLを組み立てる。
      String url = isRegisterMode ? Config.getServerUrl() + "/register"
                                  : Config.getServerUrl() + "/login";

      // username, passwordをbodyに含める
      String body =
          "username=" + java.net.URLEncoder.encode(username, "UTF-8") +
          "&password=" + java.net.URLEncoder.encode(password, "UTF-8");

      // リクエストを構築する。
      HttpRequest request =
          HttpRequest
              .newBuilder()         // ビルド開始
              .uri(URI.create(url)) // 送信先URLを指定
              .header("Content-Type",
                      "application/x-www-form-urlencoded") // ヘッダ情報を追加
              .POST(HttpRequest.BodyPublishers.ofString(
                  body)) // POSTメソッドを指定、ボディを設定
              .build();  // 生成

      // リクエストを送信し、レスポンスを受け取る。
      // レスポンスのボディは文字列として取得する。
      HttpResponse<String> response =
          client.send(request, HttpResponse.BodyHandlers.ofString());

      // 登録・認証が成功したかどうかをチェック
      String responseBody = response.body();
      if ((isRegisterMode && responseBody.contains("登録成功")) ||
          (!isRegisterMode && responseBody.contains("ログイン成功"))) {
        // セッションIDを取得して保存
        // LoginController.getSessionId()を使うと、staticなsessionIdが取得できる
        if (responseBody.contains("SESSION_ID:")) {
          int idx = responseBody.indexOf("SESSION_ID:");
          sessionId =
              responseBody.substring(idx + "SESSION_ID:".length()).trim();
        }
        // ホーム画面へ遷移
        javafx.application.Platform.runLater(() -> {
          try {
            javafx.stage.Stage stage =
                (javafx.stage.Stage)btnLogin.getScene().getWindow();
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(
                getClass().getResource("/com/habit/client/gui/Home.fxml"));
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("ホーム");
          } catch (Exception ex) {
            loginStatusLabel.setText("画面遷移エラー: " + ex.getMessage());
          }
        });
      } else {
        // サーバーからのメッセージを表示
        loginStatusLabel.setText(responseBody);
      }
    } catch (Exception ex) {
      // サーバー接続エラー時の処理
      loginStatusLabel.setText("サーバ接続エラー: " + ex.getMessage());
    }
  }
}
