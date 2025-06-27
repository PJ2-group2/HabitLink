package com.habit.client;

/**
 * ログイン画面のコントローラークラス。
 * ログイン・新規登録の切り替えや、サーバーとの認証処理を担当する。
 */
public class LoginController {
    /** ユーザー名入力フィールド */
    @javafx.fxml.FXML
    private javafx.scene.control.TextField usernameField;
    /** パスワード入力フィールド */
    @javafx.fxml.FXML
    private javafx.scene.control.PasswordField passwordField;
    /** ログイン/新規登録ボタン */
    @javafx.fxml.FXML
    private javafx.scene.control.Button btnLogin;
    /** モード切替ボタン */
    @javafx.fxml.FXML
    private javafx.scene.control.Button btnSwitchMode;
    /** ステータスメッセージ表示用ラベル */
    @javafx.fxml.FXML
    private javafx.scene.control.Label loginStatusLabel;

    /** 新規登録モードかどうかのフラグ */
    private boolean isRegisterMode = false;

    /** サーバから返却されたセッションID */
    private static String sessionId = null;

    /** 現在のセッションIDを取得 */
    public static String getSessionId() {
        return sessionId;
    }

    /**
     * コントローラー初期化処理。
     * モード切替ボタン押下時の動作を設定する。
     */
    @javafx.fxml.FXML
    public void initialize() {
        // モード切替ボタンのアクション設定
        // 初期状態はログインモード
        btnSwitchMode.setOnAction(_ -> {
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
    @javafx.fxml.FXML
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
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

            // モードに応じてURLを切り替える。
            // 送信先URLを組み立てる。
            String url = isRegisterMode
                ? "http://localhost:8080/register?username=" + java.net.URLEncoder.encode(username, "UTF-8") + "&password=" + java.net.URLEncoder.encode(password, "UTF-8")
                : "http://localhost:8080/login?username=" + java.net.URLEncoder.encode(username, "UTF-8") + "&password=" + java.net.URLEncoder.encode(password, "UTF-8");

            // リクエストビルダーを使用してPOSTリクエストを作成
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder() // リクエスト作成を開始
                .uri(java.net.URI.create(url)) // リクエストの送信先URLを設定
                .POST(java.net.http.HttpRequest.BodyPublishers.noBody()) // POSTメソッドを指定, bodyは空
                .build(); // リクエストをビルドして完成

            // リクエストを送信し、レスポンスを受け取る。
            // レスポンスのボディは文字列として取得する。    
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            // 登録・認証が成功したかどうかをチェック
            String body = response.body();
            if ((isRegisterMode && body.contains("登録成功")) || (!isRegisterMode && body.contains("ログイン成功"))) {
                // セッションIDを取得して保存
                // LoginController.getSessionId()を使うと、staticなsessionIdが取得できる
                if (body.contains("SESSION_ID:")) {
                    int idx = body.indexOf("SESSION_ID:");
                    sessionId = body.substring(idx + "SESSION_ID:".length()).trim();
                }
                // ホーム画面へ遷移
                javafx.application.Platform.runLater(() -> {
                    try {
                        javafx.stage.Stage stage = (javafx.stage.Stage) btnLogin.getScene().getWindow();
                        javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/com/habit/client/gui/Home.fxml"));
                        stage.setScene(new javafx.scene.Scene(root));
                        stage.setTitle("ホーム");
                    } catch (Exception ex) {
                        loginStatusLabel.setText("画面遷移エラー: " + ex.getMessage());
                    }
                });
            } else {
                // サーバーからのメッセージを表示
                loginStatusLabel.setText(body);
            }
        } catch (Exception ex) {
            // サーバー接続エラー時の処理
            loginStatusLabel.setText("サーバ接続エラー: " + ex.getMessage());
        }
    }
}