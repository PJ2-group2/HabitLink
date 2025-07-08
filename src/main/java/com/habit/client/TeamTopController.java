package com.habit.client;

import com.habit.client.util.Config;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.util.Callback;
import java.net.*;
import java.util.*;
import org.json.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * チームトップ画面のコントローラークラス。
 * チーム名の表示、タスク作成、個人ページやチャットページへの遷移を担当する。
 */
public class TeamTopController {
    private static final Logger logger = LoggerFactory.getLogger(TeamTopController.class);
    /* チーム名ラベル */
    @FXML
    private Label teamNameLabel;
    /* 戻るボタン */
    @FXML
    private Button btnBackHome;
    /* タスク作成ボタン */
    @FXML
    private Button btnCreateTask;
    /* 個人ページへ遷移するボタン */
    @FXML
    private Button btnToPersonal;
    /* チャットページへ遷移するボタン */
    @FXML
    private Button btnToChat;
    /* デバッグリセットボタン */
    @FXML
    private Button btnDebugReset;
    /* チームタスク一覧テーブル（型を汎用化） */
    @FXML
    private TableView<ObservableList<Object>> taskTable;
    /* 今日のタスクリスト */
    @FXML
    private ListView<String> todayTaskList;
    /* チャットログリストビュー */
    @FXML
    private ListView<String> chatList;
    /* チームキャラクター画像 */
    @FXML
    private ImageView teamCharView;
    /* 応援セリフ表示用ラベル */
    @FXML
    private Label cheerMessageLabel;
    /* サボりランキング表示用リストビュー */
    @FXML
    private ListView<String> sabotageRankingList;

    private final String serverUrl = Config.getServerUrl() + "/sendChatMessage";
    private final String chatLogUrl = Config.getServerUrl() + "/getChatLog";

    /*  遷移時に渡すユーザーIDとチームID, チーム名
     * これらは全てのコントローラが持つようにしてください。
     * 余裕があったら共通化します。
     */
    private String userId;
    private String teamID;
    private String teamName = "チーム名未取得";
    // ユーザIDのセッター
    public void setUserId(String userId) {
        logger.info("userId set: " + userId);
        this.userId = userId;
    }
    // チームIDのセッター
    public void setTeamID(String teamID) {
        this.teamID = teamID;
        logger.info("teamID set: " + teamID);
        // teamIDがセットされたタイミングでタスク、チャット、サボりランキングを読み込む。
        loadTeamTasksAndUserTasks();
        loadChatLog();
        loadSabotageRanking();
    }

    /**
     * 外部からサボりランキングを再読み込みするためのパブリックメソッド
     */
    public void refreshSabotageRanking() {
        loadSabotageRanking();
    }
    // チーム名のセッター
    public void setTeamName(String teamName) {
        this.teamName = teamName;
        logger.info("teamName set: " + teamName);
        if (teamNameLabel != null) {
            teamNameLabel.setText(teamName);
        }
    }

    /**
     * コントローラー初期化処理。
     * UI部品の初期化や、ボタンのアクション設定を行う。
     */
    @FXML
    public void initialize() {
        // UI部品の初期化のみ行う（データ取得はsetTeamIDで行う）

        int level = 0; // 初期値
        try {
            // HTTPリクエストを送信するためのクライアントオブジェクトを作成。
            HttpClient client = HttpClient.newHttpClient();
            // URLを作成
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(Config.getServerUrl() + "/getSabotagePoints"))
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
                            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();

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
                                double x = screenBounds.getMinX() + random.nextDouble() * (screenBounds.getWidth() - randomWidth);
                                double y = screenBounds.getMinY() + random.nextDouble() * (screenBounds.getHeight() - randomHeight);
                                alert.setX(x);
                                alert.setY(y);
                                
                                // show() に変更して、複数のウィンドウが同時に表示されるようにする
                                alert.show(); 
                            }
                        });
                    }

                } catch (NumberFormatException e) {
                    logger.error("サボりポイントの解析に失敗しました: " + body);
                    level = 0; // エラー時は最低レベル
                }
            }
        } catch (Exception ex) {
            logger.error("サボりポイントの取得に失敗しました: " + ex.getMessage());
            level = 0; // エラー時は最低レベル
        }

        String imagePath = "/images/TaskCharacterLv" + level + ".png";

        try {
            Image characterImage = new Image(getClass().getResource(imagePath).toExternalForm());
            teamCharView.setImage(characterImage);
        } catch (NullPointerException e) {
            logger.error("画像が見つかりません: " + imagePath);
            e.printStackTrace();
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

        // ホームへ戻るボタンのアクション設定
        btnBackHome.setOnAction(unused -> {
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) btnBackHome.getScene().getWindow();
                javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/com/habit/client/gui/Home.fxml"));
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("ホーム");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // 個人ページへ遷移するボタンのアクション設定
        btnToPersonal.setOnAction(unused -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/PersonalPage.fxml"));
                javafx.scene.Parent root = loader.load();
                PersonalPageController controller = loader.getController();
                // 各データを渡す
                controller.setUserId(userId);
                controller.setTeamID(teamID);
                controller.setTeamName(teamName);
                // ★修正：空のリストを渡してAPIから最新データを取得させる
                controller.setUserTasks(new java.util.ArrayList<>());
                javafx.stage.Stage stage = (javafx.stage.Stage) btnToPersonal.getScene().getWindow();
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("個人ページ");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // チャットページへ遷移するボタンのアクション設定
        btnToChat.setOnAction(unused -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/Chat.fxml"));
                javafx.scene.Parent root = loader.load();
                // ChatControllerを取得
                ChatController controller = loader.getController();
                // 各データを渡す(この処理を全ての画面遷移で行ってください。)
                controller.setUserId(userId);
                controller.setTeamID(teamID);
                controller.setTeamName(teamName);
                javafx.stage.Stage stage = (javafx.stage.Stage) btnToChat.getScene().getWindow();
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("チームチャット");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // タスク作成ボタンのアクション設定
        btnCreateTask.setOnAction(unused -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/TaskCreate.fxml"));
                javafx.scene.Parent root = loader.load();
                // TaskCreateControllerを取得
                TaskCreateController controller = loader.getController();
                // 各データを渡す
                controller.setUserId(userId);
                controller.setTeamID(teamID);
                controller.setTeamName(teamName);
                javafx.stage.Stage stage = (javafx.stage.Stage) btnCreateTask.getScene().getWindow();
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("タスク作成");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

	final boolean is_debug = true;
	if(is_debug){
          // デバッグリセットボタンのアクション設定
          btnDebugReset.setOnAction(unused -> { executeDebugReset(); });
        }
	btnDebugReset.setVisible(is_debug);
	btnDebugReset.setManaged(is_debug);

        // タスク進捗表の表示
        loadTaskStatusTable();

        //未消化タスクを横並びにする
        todayTaskList.setOrientation(Orientation.HORIZONTAL);
    }

    /**
     * デバッグ用リセットボタンが押されたときの処理
     * サーバーの手動タスクリセットAPIを呼び出し、0時と同じ日付切り替わり処理を実行する
     */
    private void executeDebugReset() {
        new Thread(() -> {
            try {
                // 確認ダイアログを表示
                Platform.runLater(() -> {
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("デバッグリセット確認");
                    confirmAlert.setHeaderText("タスク自動リセットを実行しますか？");
                    confirmAlert.setContentText("この操作により、0時と同じ日付切り替わり処理が実行されます。\n" +
                                               "・未完了タスクはサボりポイントが増加\n" +
                                               "・完了タスクはサボりポイントが減少\n" +
                                               "・新しい日付のタスクが生成されます");
                    
                    Optional<ButtonType> result = confirmAlert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        // 実際のリセット処理を別スレッドで実行
                        new Thread(() -> performDebugReset()).start();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 実際のデバッグリセット処理を実行
     * 1. 通常のタスクリセット処理（昨日の未消化タスク処理）
     * 2. 今日の未消化タスクのサボり報告送信
     */
    private void performDebugReset() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            
            Platform.runLater(() -> {
                Alert processingAlert = new Alert(Alert.AlertType.INFORMATION);
                processingAlert.setTitle("処理中");
                processingAlert.setHeaderText("デバッグリセット実行中...");
                processingAlert.setContentText("タスクリセットとサボり報告を実行しています...");
                processingAlert.show();
            });
            
            // 1. 通常のタスクリセット処理を実行
            String resetUrl = "http://localhost:8080/manualTaskReset";
            HttpRequest resetRequest = HttpRequest.newBuilder()
                    .uri(URI.create(resetUrl))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .GET()
                    .build();
            
            HttpResponse<String> resetResponse = client.send(resetRequest, HttpResponse.BodyHandlers.ofString());
            
            // 2. 今日の未消化タスクのサボり報告を実行
            String sabotageReportUrl = "http://localhost:8080/debugSabotageReport";
            HttpRequest sabotageRequest = HttpRequest.newBuilder()
                    .uri(URI.create(sabotageReportUrl))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .GET()
                    .build();
            
            HttpResponse<String> sabotageResponse = client.send(sabotageRequest, HttpResponse.BodyHandlers.ofString());
            
            Platform.runLater(() -> {
                Alert resultAlert;
                if (resetResponse.statusCode() == 200 && sabotageResponse.statusCode() == 200) {
                    resultAlert = new Alert(Alert.AlertType.INFORMATION);
                    resultAlert.setTitle("デバッグリセット完了");
                    resultAlert.setHeaderText("タスクリセットとサボり報告が正常に実行されました");
                    resultAlert.setContentText("タスクリセット結果:\n" + resetResponse.body() +
                                             "\n\nサボり報告結果:\n" + sabotageResponse.body());
                    
                    // 画面を更新
                    loadTeamTasksAndUserTasks();
                    loadTaskStatusTable();
                    loadSabotageRanking();
                    loadChatLog();
                } else {
                    resultAlert = new Alert(Alert.AlertType.ERROR);
                    resultAlert.setTitle("デバッグリセット失敗");
                    resultAlert.setHeaderText("処理に失敗しました");
                    String errorContent = "";
                    if (resetResponse.statusCode() != 200) {
                        errorContent += "タスクリセット失敗 (HTTP " + resetResponse.statusCode() + "): " + resetResponse.body() + "\n";
                    }
                    if (sabotageResponse.statusCode() != 200) {
                        errorContent += "サボり報告失敗 (HTTP " + sabotageResponse.statusCode() + "): " + sabotageResponse.body();
                    }
                    resultAlert.setContentText(errorContent);
                }
                resultAlert.showAndWait();
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("通信エラー");
                errorAlert.setHeaderText("サーバーとの通信に失敗しました");
                errorAlert.setContentText("エラー詳細: " + e.getMessage());
                errorAlert.showAndWait();
            });
        }
    }

    /**
     * チームタスク・ユーザタスク取得メソッド
     * チームIDがセットされたタイミングで呼び出される。
     * PersonalPageと同じAPIを使用して同じタスクを表示する。
     */
    private void loadTeamTasksAndUserTasks() {
        new Thread(() -> {
            try {
                // teamIDがnullの場合は処理をスキップ
                if (teamID == null) {
                    logger.error("teamID is null, skipping loadTeamTasksAndUserTasks");
                    return;
                }
                String sessionId = LoginController.getSessionId();
                HttpClient client = HttpClient.newHttpClient();
                // 新しいAPIを呼び出す
                String url = Config.getServerUrl() + "/getIncompleteUserTaskStatus?teamID=" + URLEncoder.encode(teamID, "UTF-8");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(10))
                        .header("SESSION_ID", sessionId)
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                // レスポンスは [{"taskId":"...","taskName":"...","dueDate":"...","isDone":false}] のJSON配列
                java.util.List<String> taskNames = new java.util.ArrayList<>();
                String json = response.body();
                logger.info("[TeamTopController] API response: " + json);
                if (json != null && json.startsWith("[")) {
                    org.json.JSONArray arr = new org.json.JSONArray(json);
                    for (int i = 0; i < arr.length(); i++) {
                        org.json.JSONObject obj = arr.getJSONObject(i);
                        String taskName = obj.optString("taskName", null);
                        if (taskName != null) {
                            taskNames.add(taskName);
                        }
                    }
                }
                logger.info("[TeamTopController] Total tasks to display: " + taskNames.size());
                Platform.runLater(() -> {
                    Callback<ListView<String>,ListCell<String>> cellFactory = p ->
                    {
                        ListCell<String> cell = new ListCell<String>()
                        {
                            @Override
                            public void updateItem(String item,boolean empty)
                            {
                                super.updateItem(item,empty);

                                if(item == null){
                                    setText("");
                                    setCursor(Cursor.DEFAULT);
                                    setOnMouseClicked(null);
                                    return;
                                }
                                setCursor(Cursor.CLOSED_HAND);
                                setOnMouseClicked(event ->{
                                    try {
                                        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/habit/client/gui/PersonalPage.fxml"));
                                        javafx.scene.Parent root = loader.load();
                                        PersonalPageController controller = loader.getController();
                                        // 各データを渡す
                                        controller.setUserId(userId);
                                        controller.setTeamID(teamID);
                                        controller.setTeamName(teamName);
                                        // ユーザーのタスク一覧を渡す
                                        controller.setUserTasks(getUserTasksForPersonalPage());
                                        javafx.stage.Stage stage = (javafx.stage.Stage) btnToPersonal.getScene().getWindow();
                                        stage.setScene(new javafx.scene.Scene(root));
                                        stage.setTitle("個人ページ");
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                    });
                                setText(item);
                                String white = "#FF00FF";
                                setStyle("-fx-background-color: " + white + "; -fx-background-radius: 10;-fx-alignment: center;-fx-background-insets:5 5 5 5;");
                            }
                        };
                        int cellSize=80;
                        cell.setPrefWidth(cellSize+20);
                        cell.setPrefHeight(cellSize);
                        return cell;
                    }; 
                    todayTaskList.setCellFactory(cellFactory);
                    todayTaskList.getItems().setAll(taskNames);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * ユーザーのタスク一覧を取得するメソッド。
     * 今日のタスクリストからユーザーのタスクオブジェクトを取得する例。
     * 必要に応じてキャッシュやフィールドに保持しておくことも可能。
     * ここでは簡易的に再取得する例を示す。
     *
     * @return ユーザーのタスク一覧
     */
    private java.util.List<com.habit.domain.Task> getUserTasksForPersonalPage() {
        try {
            // teamIDがnullの場合は空のリストを返す
            if (teamID == null) {
                logger.error("teamID is null, returning empty task list");
                return new java.util.ArrayList<>();
            }
            String sessionId = LoginController.getSessionId();
            HttpClient client = HttpClient.newHttpClient();
            // 新しいAPIを呼び出す
            String url = Config.getServerUrl() + "/getIncompleteUserTaskStatus?teamID=" + URLEncoder.encode(teamID, "UTF-8");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .header("SESSION_ID", sessionId)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String json = response.body();
            java.util.List<com.habit.domain.Task> tasks = new java.util.ArrayList<>();
            if (json != null && json.startsWith("[")) {
                org.json.JSONArray arr = new org.json.JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject obj = arr.getJSONObject(i);
                    String taskId = obj.optString("taskId", null);
                    String taskName = obj.optString("taskName", null);
                    String dueDateStr = obj.optString("dueDate", null);
                    java.time.LocalDate dueDate = null;
                    if (dueDateStr != null && !dueDateStr.isEmpty() && !"null".equals(dueDateStr)) {
                        try {
                            dueDate = java.time.LocalDate.parse(dueDateStr);
                        } catch (Exception ignore) {}
                    }
                    
                    if (taskId != null && taskName != null) {
                        com.habit.domain.Task t = new com.habit.domain.Task(taskId, taskName);
                        
                        // dueDateを設定（setterメソッドを使用）
                        if (dueDate != null) {
                            t.setDueDate(dueDate);
                        }
                        tasks.add(t);
                    }
                }
            }
            return tasks;
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    /**
     * チャットログをサーバーから取得し、最新3件を表示するメソッド。
     * チームIDがセットされたタイミングで呼び出される。
     */
    private void loadChatLog() {
        new Thread(() -> {
            try {
                // teamIDがnullの場合は処理をスキップ
                if (teamID == null) {
                    logger.error("teamID is null, skipping loadChatLog");
                    return;
                }
                // HTTPリクエストを送信するためのクライアントオブジェクトを作成
                HttpClient client = HttpClient.newHttpClient();
                // チャットログのURLを作成
                String url = chatLogUrl + "?teamID=" + URLEncoder.encode(teamID, "UTF-8") + "&limit=3";
                // リクエストを送信
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .GET()
                        .build();
                // レスポンスを取得
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                // レスポンスのボディをJSONとして解析し、メッセージリストを作成
                List<com.habit.domain.Message> messages = new ArrayList<>();
                JSONArray arr = new JSONArray(response.body());
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    messages.add(com.habit.domain.Message.fromJson(obj));
                }
                
                // タイムスタンプでソート
                messages.sort(java.util.Comparator.comparing(com.habit.domain.Message::getTimestamp));
                
                // ユーザー名とメッセージ内容のみの表示形式に整形
                List<String> chatItems = new ArrayList<>();
                for (var msg : messages) {
                    StringBuilder sb = new StringBuilder();
                    sb.append('[' + msg.getSender().getUsername() + ']');
                    sb.append(": " + msg.getContent());
                    chatItems.add(sb.toString());
                }

                Platform.runLater(() -> {
                    chatList.getItems().setAll(chatItems);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * チームタスク×メンバーの進捗表を作成し表示する
     */
    private void loadTaskStatusTable() {
        new Thread(() -> {
            try {
                String sessionId = LoginController.getSessionId();
                HttpClient client = HttpClient.newHttpClient();
                // チームメンバー一覧取得
                String membersUrl = Config.getServerUrl() + "/getTeamMembers?teamID=" + URLEncoder.encode(teamID, "UTF-8");
                HttpRequest membersReq = HttpRequest.newBuilder()
                        .uri(URI.create(membersUrl))
                        .timeout(java.time.Duration.ofSeconds(5))
                        .header("SESSION_ID", sessionId)
                        .GET().build();
                HttpResponse<String> membersRes = client.send(membersReq, HttpResponse.BodyHandlers.ofString());
                String membersBody = membersRes.body();
                JSONArray membersArr;
                if (membersBody != null && membersBody.trim().startsWith("[")) {
                    membersArr = new JSONArray(membersBody);
                } else {
                    logger.info("[loadTaskStatusTable] getTeamMembers APIレスポンスが配列形式ではありません: " + membersBody);
                    membersArr = new JSONArray();
                }
                List<String> memberIds = new ArrayList<>();
                List<String> memberNames = new ArrayList<>();
                for (int i = 0; i < membersArr.length(); i++) {
                    JSONObject obj = membersArr.getJSONObject(i);
                    memberIds.add(obj.optString("userId"));
                    memberNames.add(obj.optString("username"));
                }

                // タスク一覧取得
                String tasksUrl = Config.getServerUrl() + "/getTeamTasks?teamID=" + URLEncoder.encode(teamID, "UTF-8");
                HttpRequest tasksReq = HttpRequest.newBuilder()
                        .uri(URI.create(tasksUrl))
                        .timeout(java.time.Duration.ofSeconds(5))
                        .header("SESSION_ID", sessionId)
                        .GET().build();
                HttpResponse<String> tasksRes = client.send(tasksReq, HttpResponse.BodyHandlers.ofString());
                String tasksBody = tasksRes.body();
                JSONArray tasksArr;
                if (tasksBody != null && tasksBody.trim().startsWith("[")) {
                    tasksArr = new JSONArray(tasksBody);
                } else {
                    logger.info("[loadTaskStatusTable] getTeamTasks APIレスポンスが配列形式ではありません: " + tasksBody);
                    tasksArr = new JSONArray();
                }
                List<String> taskNames = new ArrayList<>();
                List<String> taskIds = new ArrayList<>();
                Map<String, String> taskCycleTypeMap = new HashMap<>(); // taskId→cycleType
                Map<String, String> taskNameMap = new HashMap<>(); // taskId -> taskName
                for (int i = 0; i < tasksArr.length(); i++) {
                    JSONObject obj = tasksArr.getJSONObject(i);
                    String taskId = obj.optString("taskId");
                    String taskName = obj.optString("taskName");
                    String cycleType = obj.optString("cycleType", "");
                    taskIds.add(taskId);
                    taskNames.add(taskName);
                    taskCycleTypeMap.put(taskId, cycleType);
                    taskNameMap.put(taskId, taskName);
                }

                // 進捗一覧取得（全メンバー×タスク×過去7日）
                String date = java.time.LocalDate.now().toString();
                int days = 7;
                String statusUrl = Config.getServerUrl() + "/getTeamTaskStatusList?teamID=" + URLEncoder.encode(teamID, "UTF-8") + "&date=" + date + "&days=" + days;
                HttpRequest statusReq = HttpRequest.newBuilder()
                        .uri(URI.create(statusUrl))
                        .timeout(java.time.Duration.ofSeconds(10))
                        .header("SESSION_ID", sessionId)
                        .GET().build();
                HttpResponse<String> statusRes = client.send(statusReq, HttpResponse.BodyHandlers.ofString());
                String statusBody = statusRes.body();
                JSONArray statusArr;
                if (statusBody != null && statusBody.trim().startsWith("[")) {
                    statusArr = new JSONArray(statusBody);
                } else {
                    logger.info("[loadTaskStatusTable] getTeamTaskStatusList APIレスポンスが配列形式ではありません: " + statusBody);
                    statusArr = new JSONArray();
                }
                // Map<userId+taskId, List<isDone>>
                Map<String, List<Boolean>> statusMap = new HashMap<>();
                for (int i = 0; i < statusArr.length(); i++) {
                    JSONObject obj = statusArr.getJSONObject(i);
                    String uid = obj.optString("userId");
                    String tid = obj.optString("taskId");
                    boolean isDone = obj.optBoolean("isDone", false);
                    String key = uid + "_" + tid;
                    statusMap.computeIfAbsent(key, k -> new ArrayList<>()).add(isDone);
                }

                // TableViewのカラム生成
                Platform.runLater(() -> {
                    taskTable.getColumns().clear();
                    taskTable.setFixedCellSize(32); // セルの高さを狭く
                    // 1列目: タスク名
                    TableColumn<ObservableList<Object>, String> taskCol = new TableColumn<>("タスク名");
                    taskCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty((String)data.getValue().get(0)));
                    taskCol.setStyle("-fx-font-size: 15px; -fx-alignment: center; -fx-padding: 4 0;");
                    taskCol.setPrefWidth(90); // 横幅を半分に
                    taskTable.getColumns().add(taskCol);
                    // 2列目以降: メンバーごと
                    for (int i = 0; i < memberNames.size(); i++) {
                        final int colIdx = i + 1;
                        TableColumn<ObservableList<Object>, Integer> memCol = new TableColumn<>(memberNames.get(i));
                        memCol.setCellValueFactory(data -> {
                            Object v = data.getValue().get(colIdx);
                            return new javafx.beans.property.SimpleIntegerProperty((Integer)v).asObject();
                        });
                        memCol.setPrefWidth(45);
                        memCol.setCellFactory(tc -> new TableCell<>() {
                            @Override
                            protected void updateItem(Integer daysDone, boolean empty) {
                                super.updateItem(daysDone, empty);
                                if (empty || daysDone == null) {
                                    setText("");
                                    setStyle("");
                                } else {
                                    // ユーザーIDを取得
                                    int rowIdx = getIndex();
                                    if (rowIdx < 0 || rowIdx >= taskIds.size()) {
                                        setText(""); setStyle(""); return;
                                    }
                                    String tid = taskIds.get(rowIdx);
                                    String cycleType = taskCycleTypeMap.getOrDefault(tid, "");
                                    String key = memberIds.get(colIdx-1) + "_" + tid;
                                    List<Boolean> doneList = statusMap.getOrDefault(key, Collections.emptyList());

                                    // daysDoneは0または1のみを使用（週次タスク）または実際の達成日数（通常タスク）
                                    if ("weekly".equals(cycleType)) {
                                        boolean anyDone = daysDone > 0;
                                        setText(anyDone ? "✓" : "");
                                        setStyle("-fx-background-color: " + (anyDone ? "#b2e5b2" : "#ffffff") +
                                                "; -fx-alignment: center; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 4 0;");
                                    } else {
                                        setText(String.valueOf(daysDone));
                                        String color;
                                        switch (daysDone) {
                                            case 0: color = "#ffffff"; break;
                                            case 1: color = "#e0f8e0"; break;
                                            case 2: color = "#b2e5b2"; break;
                                            case 3: color = "#7fd87f"; break;
                                            case 4: color = "#4fc24f"; break;
                                            case 5: color = "#2e9e2e"; break;
                                            case 6: color = "#176b17"; break;
                                            case 7: color = "#0a2d0a"; break;
                                            default: color = "#ffffff";
                                        }
                                        setStyle("-fx-background-color: " + color +
                                                "; -fx-alignment: center; -fx-font-size: 15px; -fx-padding: 4 0; " +
                                                "-fx-text-fill: " + (daysDone > 4 ? "white" : "black") + ";");
                                    }
                                }
                            }
                        });
                        memCol.setEditable(false);
                        taskTable.getColumns().add(memCol);
                    }
                    // データ行生成（行＝タスク、列＝[タスク名, 各メンバーの7日間達成日数]）
                    javafx.collections.ObservableList<ObservableList<Object>> rows = javafx.collections.FXCollections.observableArrayList();
                    for (int t = 0; t < taskIds.size(); t++) {
                        ObservableList<Object> row = javafx.collections.FXCollections.observableArrayList();
                        row.add(taskNames.get(t)); // 1列目: タスク名
                        String tid = taskIds.get(t);
                        for (String uid : memberIds) {
                            String key = uid + "_" + tid;
                            List<Boolean> doneList = statusMap.getOrDefault(key, Collections.emptyList());
                            String cycleType = taskCycleTypeMap.getOrDefault(tid, "");
                            
                            if ("weekly".equals(cycleType)) {
                                // 週次タスクの場合は、いずれかが達成されているかどうかを1か0で表現
                                boolean anyDone = doneList.stream().anyMatch(b -> b);
                                row.add(anyDone ? 1 : 0);
                            } else {
                                // 通常タスクの場合は達成日数をカウント
                                int daysDone = (int) doneList.stream().filter(b -> b).count();
                                row.add(daysDone);
                            }
                        }
                        rows.add(row);
                    }
                    taskTable.setItems(rows);
                    taskTable.setEditable(false); // 編集不可
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // サーバーにチャットメッセージを送信(未使用)
    private void sendChatMessage(String message) {
        new Thread(() -> {
            try {
                // teamIDがnullの場合は処理をスキップ
                if (teamID == null) {
                    logger.error("teamID is null, skipping sendChatMessage");
                    return;
                }
                HttpClient client = HttpClient.newHttpClient();
                String params = "senderId=user1&teamID=" + URLEncoder.encode(teamID, "UTF-8") + "&content=" + URLEncoder.encode(message, "UTF-8");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(serverUrl))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(params))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    loadChatLog();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * チーム内サボりランキングをサーバーから取得し、表示するメソッド。
     * チームIDがセットされたタイミングで呼び出される。
     */
    private void loadSabotageRanking() {
        new Thread(() -> {
            try {
                // teamIDがnullの場合は処理をスキップ
                if (teamID == null) {
                    logger.error("teamID is null, skipping loadSabotageRanking");
                    return;
                }
                
                // HTTPリクエストを送信するためのクライアントオブジェクトを作成
                HttpClient client = HttpClient.newHttpClient();
                // サボりランキングのURLを作成
                String url = Config.getServerUrl() + "/getTeamSabotageRanking?teamID=" + URLEncoder.encode(teamID, "UTF-8");
                // リクエストを送信
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(5))
                        .GET()
                        .build();
                // レスポンスを取得
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                // レスポンスのボディをJSONとして解析し、ランキングリストを作成
                List<String> rankingItems = new ArrayList<>();
                String json = response.body();
                if (json != null && json.startsWith("[")) {
                    JSONArray arr = new JSONArray(json);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        int rank = obj.optInt("rank", i + 1);
                        String username = obj.optString("username", "Unknown");
                        int sabotagePoints = obj.optInt("sabotagePoints", 0);
                        
                        // ランキング表示用の文字列を作成
                        String rankEmoji = getRankEmoji(rank);
                        String rankingText = String.format("%s %d位: %s (%dpt)", rankEmoji, rank, username, sabotagePoints);
                        rankingItems.add(rankingText);
                    }
                }
                
                Platform.runLater(() -> {
                    sabotageRankingList.getItems().setAll(rankingItems);
                    // ランキングリストのスタイル設定
                    sabotageRankingList.setCellFactory(listView -> new ListCell<String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                                setStyle("");
                            } else {
                                setText(item);
                                // 順位に応じて背景色を変更
                                if (item.contains("1位")) {
                                    setStyle("-fx-background-color: #ffecb3; -fx-text-fill: #e65100; -fx-font-weight: bold;");
                                } else if (item.contains("2位")) {
                                    setStyle("-fx-background-color: #f3e5f5; -fx-text-fill: #4a148c; -fx-font-weight: bold;");
                                } else if (item.contains("3位")) {
                                    setStyle("-fx-background-color: #e8f5e8; -fx-text-fill: #1b5e20; -fx-font-weight: bold;");
                                } else {
                                    setStyle("-fx-background-color: #fafafa; -fx-text-fill: #424242;");
                                }
                            }
                        }
                    });
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    List<String> errorMessage = List.of("ランキング取得エラー");
                    sabotageRankingList.getItems().setAll(errorMessage);
                });
            }
        }).start();
    }

    /**
     * 順位に応じた絵文字を返すヘルパーメソッド
     */
    private String getRankEmoji(int rank) {
        switch (rank) {
            case 1: return "🥇";
            case 2: return "🥈";
            case 3: return "🥉";
            case 4: return "4️⃣";
            case 5: return "5️⃣";
            default: return "🔸";
        }
    }
}
