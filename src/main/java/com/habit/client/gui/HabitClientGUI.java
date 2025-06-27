package com.habit.client.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * アプリケーションのメインGUIクラス。
 * ログイン画面のみを表示し、他の画面遷移はControllerクラスで行う。
 */
public class HabitClientGUI extends Application {
    /** メインウィンドウのステージ */
    private Stage primaryStage;

    /**
     * JavaFXアプリケーションのエントリーポイント。
     * 最初にログイン画面を表示する。
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        showLoginPage();
    }

    /**
     * ログイン画面を表示するメソッド。
     * Login.fxml をロードし、シーンを設定して表示する。
     */
    public void showLoginPage() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/habit/client/gui/Login.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setTitle("ログイン");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * アプリケーションのメインメソッド。
     * launch() によりJavaFXアプリケーションを起動する。
     * start() メソッドが呼び出され、ログイン画面が表示される。
     */
    public static void main(String[] args) {
        launch(args);
    }
}
