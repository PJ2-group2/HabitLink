package com.habit.client.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * アプリケーションのメインGUIクラス。
 * 各画面（ログイン、ホーム、個人ページ、チームトップ）の表示を管理する。
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
     * ログイン画面を表示する。
     */
    public void showLoginPage() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/habit/client/gui/Login.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setTitle("ログイン");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * ホーム画面を表示する。
     */
    public void showHomePage() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/habit/client/gui/Home.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setTitle("ホーム");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * 個人ページ画面を表示する。
     */
    public void showPersonalPage() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/habit/client/gui/PersonalPage.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setTitle("個人ページ");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * チームトップ画面を表示する。
     */
    public void showTeamTopPage() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/habit/client/gui/TeamTop.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setTitle("チームトップ");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * アプリケーションのメインメソッド。
     */
    public static void main(String[] args) {
        launch(args);
    }
}
