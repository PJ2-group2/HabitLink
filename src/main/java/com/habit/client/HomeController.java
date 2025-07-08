package com.habit.client;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * ホーム画面のコントローラークラス。
 * チーム一覧・キャラクターの表示や、チーム作成・検索画面への遷移を担当する。
 */
public class HomeController {
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
    /* キャラクター画像 */
    @FXML
    private ImageView characterView;
    /* チームリストビュー */
    @FXML
    private ListView<String> teamListView;
    /* チーム作成ボタン */
    @FXML
    private Button btnToCreateTeam;
    /* チーム検索ボタン */
    @FXML
    private Button btnToSearchTeam;
    /* 応援セリフ表示用ラベル */
    @FXML
    private Label cheerMessageLabel;

    // チーム名→IDのマップ
    private java.util.Map<String, String> teamNameToIdMap = new java.util.HashMap<>();

    // 遷移元からセットする
    private String userId;

    /**
     * コントローラー初期化処理。
     * チーム一覧の取得や、ボタンのアクション設定を行う。
     */
    @FXML
    public void initialize() {
        // 現在ログインユーザのjoinedTeamIdsにあるチームのみ表示
        try {
            // HTTPリクエストを送信するためのクライアントオブジェクトを作成。
            HttpClient client = HttpClient.newHttpClient();
            // URLを作成
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/getJoinedTeamInfo"))
                .GET();
            // セッションIDをヘッダに付与
            String sessionId = LoginController.getSessionId();
            if (sessionId != null && !sessionId.isEmpty()) {
                reqBuilder.header("SESSION_ID", sessionId);
            }
            // リクエストを送信
            HttpRequest request = reqBuilder.build();
            // レスポンスを受け取り、ボディを文字列として取得
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            // レスポンスのボディを解析
            teamListView.getItems().clear();
            if (body != null && !body.trim().isEmpty()) {
                // サーバから "joinedTeamIds=... \n joinedTeamNames=..." の形式で返す
                String[] lines = body.split("\\n");
                String[] teamNames = null;
                String[] teamIds = null;
                for (String line : lines) {
                    if (line.startsWith("userId=")) {
                        String id = line.substring("userId=".length());
                        if (!id.isEmpty()) {
                            userId = id.trim();
                        }
                    }
                    if (line.startsWith("joinedTeamNames=")) {
                        String joined = line.substring("joinedTeamNames=".length());
                        if (!joined.isEmpty()) {
                            teamNames = joined.split(",");
                        }
                        // userIdをログ出力
                        if (userId != null) {
                            logger.info("HomeController: userId={}", userId);
                        }
                    }
                    if (line.startsWith("joinedTeamIds=")) {
                        String joined = line.substring("joinedTeamIds=".length());
                        if (!joined.isEmpty()) {
                            teamIds = joined.split(",");
                        }
                    }
                }
                if (teamNames != null && teamIds != null && teamNames.length == teamIds.length) {
                    for (int i = 0; i < teamNames.length; i++) {
                        String name = teamNames[i].trim();
                        String id = teamIds[i].trim();
                        if (!name.isEmpty() && !id.isEmpty()) {
                            teamListView.getItems().add(name);
                            teamNameToIdMap.put(name, id);
                        }
                    }
                } else if (teamNames != null) {
                    for (String name : teamNames) {
                        if (!name.trim().isEmpty()) {
                            teamListView.getItems().add(name.trim());
                        }
                    }
                } else if (teamIds != null) {
                    for (String id : teamIds) {
                        if (!id.trim().isEmpty()) {
                            teamListView.getItems().add(id.trim());
                            teamNameToIdMap.put(id.trim(), id.trim());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            teamListView.getItems().add("サーバ接続エラー");
        }

        int level = 0; // 初期値
        try {
            // HTTPリクエストを送信するためのクライアントオブジェクトを作成。
            HttpClient client = HttpClient.newHttpClient();
            // URLを作成
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/getSabotagePoints"))
                .GET();
            // セッションIDをヘッダに付与
            String sessionId = LoginController.getSessionId();
            if (sessionId != null && !sessionId.isEmpty()) {
                reqBuilder.header("SESSION_ID", sessionId);
            }
            // リクエストを送信
            HttpRequest request = reqBuilder.build();
            // レスポンスを受け取り、ボディを文字列として取得
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (body != null && !body.trim().isEmpty()) {
                try {
                    int sabotagePoints = Integer.parseInt(body.trim());

                    // サボりポイントに応じてレベルを計算 (0-9の範囲)
                    level = Math.max(0, 9 - sabotagePoints);

                    // サボりポイントが閾値を超えたら嫌がらせポップアップを表示
                    final int SABOTAGE_THRESHOLD = 5; // 5ポイント以上でポップアップ表示
                    if (sabotagePoints >= SABOTAGE_THRESHOLD) {
                        // レベルに応じてポップアップ数を増やす (5で1つ、6で2つ...)
                        int popupCount = 1 + (sabotagePoints - SABOTAGE_THRESHOLD);

                        Platform.runLater(() -> {
                            java.util.Random random = new java.util.Random();
                            // 画面サイズを取得して、その範囲内にポップアップを出す
                            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary()
                                    .getVisualBounds();

                            for (int i = 0; i < popupCount; i++) {
                                // 警告メッセージの候補
                                String[] warningMessages = {
                                        "タスクをサボりすぎです！もっと頑張りましょう！",
                                        "このままでは、目標達成は夢のまた夢ですよ...",
                                        "仲間は見ています。あなたのそのサボりっぷりを...",
                                        "今日のサボりは、明日の後悔。",
                                        "『明日から本気出す』って、何回言いましたか？",
                                        "何やってるんですか？サボらないでください！"
                                };
                                String randomMessage = warningMessages[random.nextInt(warningMessages.length)];

                                Alert alert = new Alert(Alert.AlertType.WARNING);
                                alert.setTitle("警告: サボりすぎです！");
                                alert.setHeaderText(randomMessage);
                                alert.setContentText("現在のサボりポイント: " + sabotagePoints);

                                // ランダムなサイズ設定 (幅: 300-500, 高さ: 200-400)
                                double randomWidth = 300 + random.nextDouble() * 200;
                                double randomHeight = 200 + random.nextDouble() * 200;
                                alert.getDialogPane().setPrefSize(randomWidth, randomHeight);

                                // ランダムな位置設定
                                double x = screenBounds.getMinX()
                                        + random.nextDouble() * (screenBounds.getWidth() - randomWidth);
                                double y = screenBounds.getMinY()
                                        + random.nextDouble() * (screenBounds.getHeight() - randomHeight);
                                alert.setX(x);
                                alert.setY(y);

                                // show() に変更して、複数のウィンドウが同時に表示されるようにする
                                alert.show();
                            }
                        });
                    }
                } catch (NumberFormatException e) {
                    logger.error("サボりポイントの解析に失敗しました: {}", body);
                    level = 0; // エラー時は最低レベル
                }
            }
        } catch (Exception ex) {
            logger.error("サボりポイントの取得に失敗しました: {}", ex.getMessage(), ex);
            level = 0; // エラー時は最低レベル
        }

        // パス組み立て
        String imagePath = "/images/TaskCharacterLv" + level + ".png";

        try {
            javafx.scene.image.Image image = new javafx.scene.image.Image(
                getClass().getResource(imagePath).toExternalForm());
            characterView.setImage(image);
        } catch (Exception e) {
            logger.error("キャラクター画像の読み込みに失敗しました: {}", imagePath, e);
        }

        String[][] cheersByLevel = {
            // Lv0
            {
                "また何もやってないの？才能だね、ダメな方の。",
                "あんたがやる気出す日は地球が止まるね。",
                "やらない理由だけは毎日天才的に思いつくね。"
            },
            // Lv1
            {
                "一日やっただけで満足？よっ、三日坊主未満！",
                "そのやる気、どこかに落としてきたの？",
                "進捗ゼロでも、言い訳は一流だね！"
            },
            // Lv2
            {
                "たった2日でドヤ顔？笑わせないで。",
                "まだその程度？やっぱ期待しなきゃよかった。",
                "奇跡的に続いてるけど、明日は期待してないよ。"
            },
            // Lv3
            {
                "へぇ…やればできるじゃん。って言うと思った？",
                "やってる姿はそこそこ様になってきたね、初心者感は抜けないけど。",
                "意外と根性あるじゃん、10年前の君よりマシかもね。"
            },
            // Lv4
            {
                "ようやく人間らしくなってきたね。",
                "5日続けただけで満足？まだ半人前以下だよ？",
                "“努力してるフリ”はもう卒業したら？"
            },
            // Lv5
            {
                "おっ、ちゃんと続いてる。奇跡って起きるんだね。",
                "ちょっとだけ期待しても…いいのかもね。",
                "君にしてはよくやってる。あくまで“君にしては”ね。"
            },
            // Lv6
            {
                "……思ってたより、ちゃんとやるんだね。",
                "認めたくないけど、ちょっとカッコいいかも。",
                "まあ、君なりに頑張ってるってことは分かるよ。"
            },
            // Lv7
            {
                "ここまで続けられるなんて、尊敬する。",
                "もう君の努力は本物だよ。堂々と胸張っていい。",
                "信じて見ててよかったよ、本当に。"
            },
            // Lv8
            {
                "ここまで来た君を誰もバカにできない。",
                "地道な積み重ねがここまで美しいものだなんて、思わなかったよ。",
                "“継続できる人”って、君のことなんだね。"
            },
            // Lv9
            {
                "君は誰よりも強く、誰よりも誠実な人だ。",
                "今の君なら、何だって叶えられるよ。",
                "君が君であることに、世界中が感謝するレベルだよ。"
            }
        };

        String[] cheers = cheersByLevel[level];
        String selectedCheer = cheers[new java.util.Random().nextInt(cheers.length)];
        cheerMessageLabel.setText(selectedCheer);

        // チームリストビューのクリックイベント設定
        // チーム名を選択したらチームトップへ遷移
        teamListView.setOnMouseClicked(unused -> {
            String selected = teamListView.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.equals("サーバ接続エラー")) {
                try {
                    // チーム名をパラメータとして渡す
                    javafx.stage.Stage stage = (javafx.stage.Stage) teamListView.getScene().getWindow();
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/TeamTop.fxml"));
                    javafx.scene.Parent root = loader.load();
                    com.habit.client.TeamTopController controller = loader.getController();
                    controller.setTeamName(selected);
                    // チーム名からIDを取得して渡す
                    String teamId = teamNameToIdMap.get(selected);
                    if (teamId != null) {
                        controller.setTeamID(teamId);
                    } 
                    // userIdも渡す
                    if (userId != null) {
                        controller.setUserId(userId);
                        logger.info("HomeController: TeamTopControllerにuserIdを渡しました: {}", userId);
                    }
                    stage.setScene(new javafx.scene.Scene(root));
                    stage.setTitle("チームトップ");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // チーム作成画面遷移ボタンのアクション設定
        btnToCreateTeam.setOnAction(unused -> {
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) btnToCreateTeam.getScene().getWindow();
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/CreateTeam.fxml"));
                javafx.scene.Parent root = loader.load();
                com.habit.client.CreateTeamController controller = loader.getController();
                if (userId != null) {
                    controller.setUserId(userId);
                }
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("チーム作成");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // チーム検索画面遷移ボタンのアクション設定
        btnToSearchTeam.setOnAction(unused -> {
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) btnToSearchTeam.getScene().getWindow();
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/SearchTeam.fxml"));
                javafx.scene.Parent root = loader.load();
                com.habit.client.SearchTeamController controller = loader.getController();
                if (userId != null) {
                    controller.setUserId(userId);
                }
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("チーム検索");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}