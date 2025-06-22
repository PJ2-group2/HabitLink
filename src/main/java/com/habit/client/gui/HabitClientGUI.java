package com.habit.client.gui;
// JavaFXのGUI雛形
// このプログラムは、習慣化共有アプリのクライアント側です。
// サーバとHTTP通信し、タスクやルームの情報をやりとりします。
// 画面はJavaFXで作成しています。

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class HabitClientGUI extends Application {
    @Override
    public void start(Stage primaryStage) {
        // --- ログイン画面 ---
        Label loginTitle = new Label("ログイン / 新規登録");
        javafx.scene.control.TextField usernameField = new javafx.scene.control.TextField();
        usernameField.setPromptText("ユーザ名");
        javafx.scene.control.PasswordField passwordField = new javafx.scene.control.PasswordField();
        passwordField.setPromptText("パスワード");
        Button btnSwitchMode = new Button("新規登録モードへ");
        Button btnLoginOrRegister = new Button("ログイン");
        Label loginStatusLabel = new Label("");
        VBox loginRoot = new VBox(10, loginTitle, usernameField, passwordField, btnLoginOrRegister, btnSwitchMode, loginStatusLabel);
        Scene loginScene = new Scene(loginRoot, 300, 220);

        // --- メインメニュー（loginSceneより前に宣言しfinalで参照可能に） ---
        final Button btnPersonal = new Button("個人ページ");
        final Button btnTeam = new Button("チームページ");
        final VBox root = new VBox(10, btnPersonal, btnTeam);
        final Scene mainScene = new Scene(root, 300, 200);

        // ログイン/新規登録モード切替用フラグ
        final boolean[] isRegisterMode = {false};
        btnSwitchMode.setOnAction(e -> {
            isRegisterMode[0] = !isRegisterMode[0];
            if (isRegisterMode[0]) {
                btnLoginOrRegister.setText("新規登録");
                btnSwitchMode.setText("ログインモードへ");
                loginTitle.setText("新規登録");
            } else {
                btnLoginOrRegister.setText("ログイン");
                btnSwitchMode.setText("新規登録モードへ");
                loginTitle.setText("ログイン");
            }
            loginStatusLabel.setText("");
        });

        btnLoginOrRegister.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            if (username.isEmpty() || password.isEmpty()) {
                loginStatusLabel.setText("ユーザ名とパスワードを入力してください");
                return;
            }
            try {
                HttpClient client = HttpClient.newHttpClient();
                String url = isRegisterMode[0]
                    ? "http://localhost:8080/register?username=" + java.net.URLEncoder.encode(username, "UTF-8") + "&password=" + java.net.URLEncoder.encode(password, "UTF-8")
                    : "http://localhost:8080/login?username=" + java.net.URLEncoder.encode(username, "UTF-8") + "&password=" + java.net.URLEncoder.encode(password, "UTF-8");
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String body = response.body();
                if ((isRegisterMode[0] && body.contains("登録成功")) || (!isRegisterMode[0] && body.contains("ログイン成功"))) {
                    // 認証成功時はメイン画面へ
                    usernameField.clear();
                    passwordField.clear();
                    loginStatusLabel.setText("");
                    primaryStage.setScene(mainScene);
                } else {
                    loginStatusLabel.setText(body);
                }
            } catch (Exception ex) {
                loginStatusLabel.setText("サーバ接続エラー: " + ex.getMessage());
            }
        });

        // --- 個人ページ ---
        // 自分のタスクを管理する画面
        Button btnBack1 = new Button("戻る");
        VBox personalRoot = new VBox(10);
        Label personalLabel = new Label("個人ページ（タスク管理）");
        javafx.scene.control.ListView<String> taskList = new javafx.scene.control.ListView<>();
        javafx.scene.control.TextField taskInput = new javafx.scene.control.TextField();
        taskInput.setPromptText("新しいタスクを入力");
        Button btnAddTask = new Button("タスク追加");
        Button btnCompleteTask = new Button("選択タスク完了");
        personalRoot.getChildren().addAll(personalLabel, taskList, taskInput, btnAddTask, btnCompleteTask, btnBack1);
        Scene personalScene = new Scene(personalRoot, 350, 350);

        // タスク追加ボタンの処理
        btnAddTask.setOnAction(e -> {
            String task = taskInput.getText().trim();
            if (!task.isEmpty()) {
                taskList.getItems().add(task);
                taskInput.clear();
            }
        });
        // タスク完了ボタンの処理
        btnCompleteTask.setOnAction(e -> {
            int idx = taskList.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                taskList.getItems().remove(idx);
            }
        });

        // --- チームページ（ルーム選択画面） ---
        VBox teamRoot = new VBox(10);
        Label teamLabel = new Label("チームページ（ルーム管理）");
        // ルームID入力欄
        javafx.scene.control.TextField roomIdInput = new javafx.scene.control.TextField();
        roomIdInput.setPromptText("ルームIDを入力");
        // ルーム作成・参加ボタン
        Button btnCreateRoom = new Button("ルーム作成");
        Button btnJoinRoom = new Button("ルーム参加");
        // サーバからの応答表示用ラベル
        Label roomStatusLabel = new Label("");
        // --- ルームページ（ルームに入った後の画面） ---
        VBox roomRoot = new VBox(10);
        // ルームIDを表示するラベル
        Label roomPageLabel = new Label();
        // チームタスク入力欄
        javafx.scene.control.TextField teamTaskInput = new javafx.scene.control.TextField();
        teamTaskInput.setPromptText("チームタスクを入力");
        // チームタスク追加ボタン
        Button btnAddTeamTask = new Button("チームタスク追加");
        // チームタスク一覧表示
        javafx.scene.control.ListView<String> teamTaskList = new javafx.scene.control.ListView<>();
        // タスク一覧更新ボタン
        Button btnRefreshTasks = new Button("タスク一覧更新");
        // ルーム選択画面に戻るボタン
        Button btnRoomBack = new Button("ルーム選択に戻る");
        // 選択タスク削除ボタン
        Button btnDeleteTask = new Button("選択タスク削除");
        // ルームページのレイアウトに各部品を追加
        roomRoot.getChildren().addAll(roomPageLabel, teamTaskInput, btnAddTeamTask, btnRefreshTasks, btnDeleteTask, teamTaskList, btnRoomBack);
        Scene roomScene = new Scene(roomRoot, 400, 400);

        // チームページのレイアウトに各部品を追加
        teamRoot.getChildren().addAll(teamLabel, roomIdInput, btnCreateRoom, btnJoinRoom, roomStatusLabel);
        Scene teamScene = new Scene(teamRoot, 350, 200);

        // --- ルームページのタスク追加処理 ---
        btnAddTeamTask.setOnAction(e -> {
            String roomId = roomPageLabel.getText();
            String task = teamTaskInput.getText().trim();
            if (!roomId.isEmpty() && !task.isEmpty()) {
                try {
                    // サーバにタスク追加リクエストを送信
                    HttpClient client = HttpClient.newHttpClient();
                    String url = "http://localhost:8080/addTask?id=" + java.net.URLEncoder.encode(roomId, "UTF-8") + "&task=" + java.net.URLEncoder.encode(task, "UTF-8");
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    teamTaskInput.clear();
                    btnRefreshTasks.fire(); // タスク一覧を自動更新
                } catch (Exception ex) {
                    // エラー時はタスク一覧下部に表示
                    teamTaskList.getItems().add("サーバ接続エラー: " + ex.getMessage());
                }
            }
        });
        // --- ルームページのタスク一覧取得処理 ---
        btnRefreshTasks.setOnAction(e -> {
            String roomId = roomPageLabel.getText();
            if (!roomId.isEmpty()) {
                try {
                    // サーバにタスク一覧取得リクエストを送信
                    HttpClient client = HttpClient.newHttpClient();
                    String url = "http://localhost:8080/getTasks?id=" + java.net.URLEncoder.encode(roomId, "UTF-8");
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    String[] tasks = response.body().split("\\n");
                    teamTaskList.getItems().setAll(tasks);
                } catch (Exception ex) {
                    teamTaskList.getItems().add("サーバ接続エラー: " + ex.getMessage());
                }
            }
        });
        // --- ルームページのタスク削除処理（現状はUIのみ）---
        btnDeleteTask.setOnAction(e -> {
            String roomId = roomPageLabel.getText();
            int idx = teamTaskList.getSelectionModel().getSelectedIndex();
            if (!roomId.isEmpty() && idx >= 0) {
                try {
                    // 現状はUI上でリストから削除のみ（サーバ側API拡張は次ステップで対応）
                    java.util.List<String> tasks = new java.util.ArrayList<>(teamTaskList.getItems());
                    tasks.remove(idx);
                    teamTaskList.getItems().setAll(tasks);
                } catch (Exception ex) {
                    teamTaskList.getItems().add("削除エラー: " + ex.getMessage());
                }
            }
        });
        // --- ルーム選択画面に戻る処理 ---
        btnRoomBack.setOnAction(e -> {
            primaryStage.setScene(teamScene);
            roomPageLabel.setText("");
            teamTaskList.getItems().clear();
        });

        // --- ルーム作成・参加時にルームページへ遷移 ---
        btnCreateRoom.setOnAction(e -> {
            String roomId = roomIdInput.getText().trim();
            if (!roomId.isEmpty()) {
                try {
                    // サーバにルーム作成リクエストを送信
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/createRoom?id=" + java.net.URLEncoder.encode(roomId, "UTF-8")))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.body().contains("作成しました")) {
                        roomPageLabel.setText(roomId);
                        primaryStage.setScene(roomScene);
                        btnRefreshTasks.fire();
                    } else {
                        roomStatusLabel.setText(response.body());
                    }
                } catch (Exception ex) {
                    roomStatusLabel.setText("サーバ接続エラー: " + ex.getMessage());
                }
            }
        });
        btnJoinRoom.setOnAction(e -> {
            String roomId = roomIdInput.getText().trim();
            if (!roomId.isEmpty()) {
                try {
                    // サーバにルーム参加リクエストを送信
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/joinRoom?id=" + java.net.URLEncoder.encode(roomId, "UTF-8")))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.body().contains("参加しました")) {
                        roomPageLabel.setText(roomId);
                        primaryStage.setScene(roomScene);
                        btnRefreshTasks.fire();
                    } else {
                        roomStatusLabel.setText(response.body());
                    }
                } catch (Exception ex) {
                    roomStatusLabel.setText("サーバ接続エラー: " + ex.getMessage());
                }
            }
        });
        // --- 画面遷移処理 ---
        btnPersonal.setOnAction(e -> primaryStage.setScene(personalScene));
        btnTeam.setOnAction(e -> primaryStage.setScene(teamScene));
        btnBack1.setOnAction(e -> primaryStage.setScene(mainScene));

        primaryStage.setTitle("習慣化共有クライアント");
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
