package com.habit.client;

public class LoginController {
    @javafx.fxml.FXML
    private javafx.scene.control.TextField usernameField;
    @javafx.fxml.FXML
    private javafx.scene.control.PasswordField passwordField;
    @javafx.fxml.FXML
    private javafx.scene.control.Button btnLogin;
    @javafx.fxml.FXML
    private javafx.scene.control.Button btnSwitchMode;
    @javafx.fxml.FXML
    private javafx.scene.control.Label loginStatusLabel;

    private boolean isRegisterMode = false;

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
                // 画面遷移（ホーム画面へ）
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
                loginStatusLabel.setText(body);
            }
        } catch (Exception ex) {
            loginStatusLabel.setText("サーバ接続エラー: " + ex.getMessage());
        }
    }
}