package com.habit.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CreateTeamController {
    @FXML private TextField teamNameField;
    @FXML private TextField passcodeField;
    @FXML private Spinner<Integer> maxMembersSpinner;
    @FXML private ChoiceBox<String> editPermissionChoice;
    @FXML private CheckBox catStudy, catWorkout, catReading, catDiet, catHobby, catOther;
    @FXML private RadioButton publicRadio, privateRadio;
    @FXML private TextField inviteMemberField;
    @FXML private Button btnAddMember;
    @FXML private ListView<String> inviteList;
    @FXML private Button btnCreateTeam;
    @FXML private Button btnBackHome;

    private ToggleGroup scopeGroup = new ToggleGroup();
    private ObservableList<String> invitedMembers = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        maxMembersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 5));
        editPermissionChoice.setItems(FXCollections.observableArrayList("自分だけ", "自由"));
        editPermissionChoice.getSelectionModel().selectFirst();
        publicRadio.setToggleGroup(scopeGroup);
        privateRadio.setToggleGroup(scopeGroup);
        publicRadio.setSelected(true);
        inviteList.setItems(invitedMembers);

        btnAddMember.setOnAction(e -> {
            String id = inviteMemberField.getText().trim();
            if (!id.isEmpty() && !invitedMembers.contains(id)) {
                invitedMembers.add(id);
                inviteMemberField.clear();
            }
        });

        btnBackHome.setOnAction(e -> {
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) btnBackHome.getScene().getWindow();
                javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/com/habit/client/gui/Home.fxml"));
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("ホーム");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        btnCreateTeam.setOnAction(e -> {
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

                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest.Builder reqBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/createTeam"))
                    .header("Content-Type", "application/x-www-form-urlencoded");
                // セッションIDをヘッダに付与
                String sessionId = LoginController.getSessionId();
                if (sessionId != null && !sessionId.isEmpty()) {
                    reqBuilder.header("SESSION_ID", sessionId);
                }
                java.net.http.HttpRequest request = reqBuilder
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(sb.toString()))
                    .build();
                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
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