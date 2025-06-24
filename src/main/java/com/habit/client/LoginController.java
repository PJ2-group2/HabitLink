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

    /** サーバから返却されたセッションID（認証済みユーザー用） */
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
        btnSwitchMode.setOnAction(e -> {
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
     * ログイン/新規登録ボタン押下時の処理。
     * サーバーへ認証リクエストを送り、結果に応じて画面遷移やエラーメッセージ表示を行う。
     */
    @javafx.fxml.FXML
    public void handleLoginButtonAction(javafx.event.ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        if (username.isEmpty() || password.isEmpty()) {
            loginStatusLabel.setText("ユーザ名とパスワードを入力してください");
            return;
        }
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String url = isRegisterMode
                ? "http://localhost:8080/register?username=" + java.net.URLEncoder.encode(username, "UTF-8") + "&password=" + java.net.URLEncoder.encode(password, "UTF-8")
                : "http://localhost:8080/login?username=" + java.net.URLEncoder.encode(username, "UTF-8") + "&password=" + java.net.URLEncoder.encode(password, "UTF-8");
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
                .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if ((isRegisterMode && body.contains("登録成功")) || (!isRegisterMode && body.contains("ログイン成功"))) {
                // セッションIDを取得して保存
                if (!isRegisterMode && body.contains("SESSION_ID:")) {
                    int idx = body.indexOf("SESSION_ID:");
                    sessionId = body.substring(idx + "SESSION_ID:".length()).trim();
                }
                // 認証成功時はホーム画面へ遷移
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