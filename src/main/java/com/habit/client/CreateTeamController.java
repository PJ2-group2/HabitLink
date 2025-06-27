package com.habit.client;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CreateTeamController {
    /** チーム名入力フィールド */
    @FXML
    private TextField teamNameField;
    /** パスコード入力フィールド */
    @FXML
    private TextField passcodeField;
    /** チームメンバー上限数スピナー */
    @FXML
    private Spinner<Integer> maxMembersSpinner;
    /** 編集権限選択ボックス */
    @FXML
    private ChoiceBox<String> editPermissionChoice;
    /** カテゴリチェックボックス */
    @FXML
    private CheckBox catStudy, catWorkout, catReading, catDiet, catHobby, catOther;
    /** チームの公開範囲ラジオボタン */
    @FXML
    private RadioButton publicRadio, privateRadio;
    /** チームの公開範囲ラベル */
    @FXML
    private TextField inviteMemberField;
    /** チームメンバー追加ボタン */
    @FXML
    private Button btnAddMember;
    /** チームメンバー追加フィールド */
    @FXML
    private ListView<String> inviteList;
    /** チーム作成ボタン */
    @FXML
    private Button btnCreateTeam;
    /** 戻るボタン */
    @FXML
    private Button btnBackHome;

    private ToggleGroup scopeGroup = new ToggleGroup(); // チームの公開範囲を選択するためのトグルグループ
    private ObservableList<String> invitedMembers = FXCollections.observableArrayList(); // 招待されたメンバーのリスト

    /* コントローラー初期化メソッド
     * チーム名、パスコード、メンバー上限数、編集権限の初期値設定や、
     * 招待メンバーリストの初期化を行う。
     */
    @FXML
    public void initialize() {
        // 初期値設定
        maxMembersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 5));
        editPermissionChoice.setItems(FXCollections.observableArrayList("自分だけ", "自由"));
        editPermissionChoice.getSelectionModel().selectFirst();
        publicRadio.setToggleGroup(scopeGroup);
        privateRadio.setToggleGroup(scopeGroup);
        publicRadio.setSelected(true);
        inviteList.setItems(invitedMembers);

        // メンバー追加ボタンのアクション設定
        btnAddMember.setOnAction(_ -> {
            String id = inviteMemberField.getText().trim();
            if (!id.isEmpty() && !invitedMembers.contains(id)) {
                invitedMembers.add(id);
                inviteMemberField.clear();
            }
        });

        // 戻るボタンのアクション設定
        btnBackHome.setOnAction(_ -> {
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) btnBackHome.getScene().getWindow();
                javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/com/habit/client/gui/Home.fxml"));
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("ホーム");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // チーム作成ボタンのアクション設定
        btnCreateTeam.setOnAction(_ -> {
            // 入力値取得
            String teamName = teamNameField.getText().trim();
            String passcode = passcodeField.getText().trim();
            int maxMembers = maxMembersSpinner.getValue();
            String editPerm = editPermissionChoice.getValue();
            String category = getSelectedCategories();
            String scope = publicRadio.isSelected() ? "public" : "private";
            ObservableList<String> members = FXCollections.observableArrayList(invitedMembers);

            if (teamName.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "チーム名を入力してください").showAndWait();
                return;
            }

            // サーバ連携: key=value&...形式でPOST
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("teamName=").append(java.net.URLEncoder.encode(teamName, "UTF-8"));
                sb.append("&passcode=").append(java.net.URLEncoder.encode(passcode, "UTF-8"));
                sb.append("&maxMembers=").append(maxMembers);
                sb.append("&editPermission=").append(java.net.URLEncoder.encode(editPerm, "UTF-8"));
                sb.append("&category=").append(java.net.URLEncoder.encode(category, "UTF-8"));
                sb.append("&scope=").append(java.net.URLEncoder.encode(scope, "UTF-8"));
                sb.append("&members=").append(java.net.URLEncoder.encode(String.join(",", members), "UTF-8"));

                // HTTPクライアントを作成
                HttpClient client = HttpClient.newHttpClient();
                // リクエストビルダーを使用してリクエストを構築
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/createTeam"))
                    .header("Content-Type", "application/x-www-form-urlencoded");
                // セッションIDをヘッダに付与
                String sessionId = LoginController.getSessionId();
                if (sessionId != null && !sessionId.isEmpty()) {
                    reqBuilder.header("SESSION_ID", sessionId);
                }
                // リクエストをPOSTメソッドで送信
                HttpRequest request = reqBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(sb.toString()))
                    .build();
                // レスポンスを受け取る
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String body = response.body();
                if (body.contains("チーム作成成功")) {
                    javafx.stage.Stage stage = (javafx.stage.Stage) btnCreateTeam.getScene().getWindow();
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/TeamTop.fxml"));
                    javafx.scene.Parent root = loader.load();
                    TeamTopController controller = loader.getController();
                    controller.setTeamID(passcode);
                    controller.setTeamName(teamName);
                    stage.setScene(new javafx.scene.Scene(root));
                    stage.setTitle("チームトップ");

                    // チーム作成後すぐタスク作成画面に遷移する場合はこちら
                    /*
                    javafx.fxml.FXMLLoader taskLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/TaskCreate.fxml"));
                    javafx.scene.Parent taskRoot = taskLoader.load();
                    TaskCreateController taskController = taskLoader.getController();
                    taskController.setTeamID(passcode);
                    stage.setScene(new javafx.scene.Scene(taskRoot));
                    stage.setTitle("タスク作成");
                    */
                } else {
                    new Alert(Alert.AlertType.ERROR, body).showAndWait();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "サーバ接続エラー: " + ex.getMessage()).showAndWait();
            }
        });
    }

    /**
     * 選択されたカテゴリのチェックボックスから、選択されたカテゴリをカンマ区切りの文字列で取得するメソッド
     * @return 選択されたカテゴリの文字列
     */
    private String getSelectedCategories() {
        StringBuilder sb = new StringBuilder();
        if (catStudy.isSelected()) sb.append("勉強,");
        if (catWorkout.isSelected()) sb.append("筋トレ,");
        if (catReading.isSelected()) sb.append("読書,");
        if (catDiet.isSelected()) sb.append("ダイエット,");
        if (catHobby.isSelected()) sb.append("趣味,");
        if (catOther.isSelected()) sb.append("その他,");
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}