package com.habit.client.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HabitClientGUI extends Application {
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        showLoginPage();
    }

    public void showLoginPage() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/habit/client/gui/Login.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setTitle("ログイン");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void showHomePage() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/habit/client/gui/Home.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setTitle("ホーム");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void showPersonalPage() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/habit/client/gui/PersonalPage.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setTitle("個人ページ");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void showTeamTopPage() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/habit/client/gui/TeamTop.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setTitle("チームトップ");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
