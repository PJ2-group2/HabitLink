package com.habit.client;

import com.habit.domain.util.Config;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ホーム画面のコントローラークラス。
 * チーム一覧・キャラクターの表示や、チーム作成・検索画面への遷移を担当する。
 */
public class HomeController {
  private static final Logger logger =
      LoggerFactory.getLogger(HomeController.class);
  /* キャラクター画像 */
  @FXML private ImageView characterView;
  /* チームリストビュー */
  @FXML private ListView<com.habit.domain.Team> teamListView;
  /* チーム作成ボタン */
  @FXML private Button btnToCreateTeam;
  /* チーム検索ボタン */
  @FXML private Button btnToSearchTeam;
  /* 応援セリフ表示用ラベル */
  @FXML private Label cheerMessageLabel;
  @FXML
  /* チームキャラクター画像 */
  private ImageView teamCharView;

  

  // 遷移元からセットする
  private String userId;

  /**
   * コントローラー初期化処理。
   * チーム一覧の取得や、ボタンのアクション設定を行う。
   * 削除機能実装時に簡易化した。
   */
  @FXML
  public void initialize() {
    loadJoinedTeams();
    setupCharacterAnimationAndCheer();
    setupButtonActions();
  }

  /**
   * 参加中のチーム情報をサーバから取得し、ListViewに表示する。
   */
  private void loadJoinedTeams() {
    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
          .uri(URI.create(Config.getServerUrl() + "/getJoinedTeamInfo"))
          .GET();

      String sessionId = LoginController.getSessionId();
      if (sessionId != null && !sessionId.isEmpty()) {
        reqBuilder.header("SESSION_ID", sessionId);
      }

      HttpRequest request = reqBuilder.build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      String body = response.body();

      teamListView.getItems().clear();
      if (body != null && !body.trim().isEmpty()) {
        org.json.JSONObject responseObject = new org.json.JSONObject(body);
        this.userId = responseObject.optString("userId", null);

        org.json.JSONArray teamsArray = responseObject.getJSONArray("teams");
        List<com.habit.domain.Team> teams = new ArrayList<>();
        for (int i = 0; i < teamsArray.length(); i++) {
          org.json.JSONObject teamJson = teamsArray.getJSONObject(i);
          com.habit.domain.Team team = new com.habit.domain.Team(
              teamJson.getString("teamId"),
              teamJson.getString("teamName"),
              teamJson.getString("creatorId"),
              com.habit.domain.TeamMode.FIXED_TASK_MODE // Default value
          );
          teams.add(team);
        }
        teamListView.getItems().setAll(teams);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      // In case of an error, you might want to show an error message in the ListView
    }

    // チームリストビューのセルファクトリを設定
    // 右クリックで削除メニューを表示するための設定
    teamListView.setCellFactory(lv -> new javafx.scene.control.ListCell<com.habit.domain.Team>() {
      private final javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
      private final javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("削除");

      {
        deleteItem.setOnAction(event -> {
          com.habit.domain.Team selectedTeam = getItem();
          if (selectedTeam != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("確認");
            alert.setHeaderText("チームの削除");
            alert.setContentText("本当にこのチームを削除しますか？\n関連するすべてのデータ（タスク、メッセージなど）が削除されます。");

            java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
              deleteTeam(selectedTeam.getTeamID());
            }
          }
        });
        contextMenu.getItems().add(deleteItem);
      }

      // セルの更新処理
      @Override
      protected void updateItem(com.habit.domain.Team team, boolean empty) {
        super.updateItem(team, empty);
        if (empty || team == null) {
          setText(null);
          setContextMenu(null);
        } else {
          setText(team.getteamName());
          if (userId != null && userId.equals(team.getCreatorId())) {
            setContextMenu(contextMenu);
          }
        }
      }
    });
  }

  /**
   * キャラクターのアニメーションと応援メッセージを設定する。
   * サーバからサボりポイントを取得し、レベルに応じたアニメーションとメッセージを表示する。
   */
  private void setupCharacterAnimationAndCheer() {
    int level = 0; // 初期値
    try {
      // HTTPリクエストを送信するためのクライアントオブジェクトを作成。
      HttpClient client = HttpClient.newHttpClient();
      // URLを作成
      HttpRequest.Builder reqBuilder =
          HttpRequest.newBuilder()
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
      HttpResponse<String> response =
          client.send(request, HttpResponse.BodyHandlers.ofString());
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
              javafx.geometry.Rectangle2D screenBounds =
                  javafx.stage.Screen.getPrimary().getVisualBounds();

              for (int i = 0; i < popupCount; i++) {
                // 警告メッセージの候補
                String[] warningMessages = {
                    "タスクをサボりすぎです！もっと頑張りましょう！",
                    "このままでは、目標達成は夢のまた夢ですよ...",
                    "仲間は見ています。あなたのそのサボりっぷりを...",
                    "今日のサボりは、明日の後悔。",
                    "『明日から本気出す』って、何回言いましたか？",
                    "何やってるんですか？サボらないでください！"};
                String randomMessage =
                    warningMessages[random.nextInt(warningMessages.length)];

                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("警告: サボりすぎです！");
                alert.setHeaderText(randomMessage);
                alert.setContentText("現在のサボりポイント: " + sabotagePoints);

                // ランダムなサイズ設定 (幅: 300-500, 高さ: 200-400)
                double randomWidth = 300 + random.nextDouble() * 200;
                double randomHeight = 200 + random.nextDouble() * 200;
                alert.getDialogPane().setPrefSize(randomWidth, randomHeight);

                // ランダムな位置設定
                double x = screenBounds.getMinX() +
                           random.nextDouble() *
                               (screenBounds.getWidth() - randomWidth);
                double y = screenBounds.getMinY() +
                           random.nextDouble() *
                               (screenBounds.getHeight() - randomHeight);
                alert.setX(x);
                alert.setY(y);

                // show()
                // に変更して、複数のウィンドウが同時に表示されるようにする
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
      logger.error("サボりポイントの取得に失敗しました: {}", ex.getMessage(),
                   ex);
      level = 0; // エラー時は最低レベル
    }

    // パス組み立て
    List<Image> animationFrames = new ArrayList<>();

    for (int i = 1; i <= 3; i++) {
      String framePath = "/images/TaskCharacterLv" + level + "-" + i + ".png";
      try {
        Image frameImage =
            new Image(getClass().getResource(framePath).toExternalForm());
        animationFrames.add(frameImage);
      } catch (Exception e) {
        logger.error("アニメーションフレームの読み込み失敗: " + framePath);
      }
    }

    if (!animationFrames.isEmpty()) {
      final int[] frameIndex = {0};

      Timeline animationTimeline =
          new Timeline(new KeyFrame(Duration.seconds(1.0), event -> {
            teamCharView.setImage(animationFrames.get(frameIndex[0]));
            frameIndex[0] = (frameIndex[0] + 1) % animationFrames.size();
          }));
      animationTimeline.setCycleCount(Timeline.INDEFINITE);
      animationTimeline.play();
    } else {
      // 画像がない場合は1枚のみにフォールバック
      String fallbackPath = "/images/TaskCharacterLv" + level + ".png";
      try {
        Image fallbackImage =
            new Image(getClass().getResource(fallbackPath).toExternalForm());
        teamCharView.setImage(fallbackImage);
      } catch (NullPointerException e) {
        logger.error("フォールバック画像も見つかりません: " + fallbackPath);
      }
    }

    String[][] cheersByLevel = { // レベルごとの応援メッセージ
        // Lv0
        {"また何もやってないの？才能だね、ダメな方の。",
         "あんたがやる気出す日は地球が止まるね。",
         "やらない理由だけは毎日天才的に思いつくね。"},
        // Lv1
        {"一日やっただけで満足？よっ、三日坊主未満！",
         "そのやる気、どこかに落としてきたの？",
         "進捗ゼロでも、言い訳は一流だね！"},
        // Lv2
        {"たった2日でドヤ顔？笑わせないで。",
         "まだその程度？やっぱ期待しなきゃよかった。",
         "奇跡的に続いてるけど、明日は期待してないよ。"},
        // Lv3
        {"へぇ…やればできるじゃん。って言うと思った？",
         "やってる姿はそこそこ様になってきたね、初心者感は抜けないけど。",
         "意外と根性あるじゃん、10年前の君よりマシかもね。"},
        // Lv4
        {"ようやく人間らしくなってきたね。",
         "5日続けただけで満足？まだ半人前以下だよ？",
         "“努力してるフリ”はもう卒業したら？"},
        // Lv5
        {"おっ、ちゃんと続いてる。奇跡って起きるんだね。",
         "ちょっとだけ期待しても…いいのかもね。",
         "君にしてはよくやってる。あくまで“君にしては”ね。"},
        // Lv6
        {"……思ってたより、ちゃんとやるんだね。",
         "認めたくないけど、ちょっとカッコいいかも。",
         "まあ、君なりに頑張ってるってことは分かるよ。"},
        // Lv7
        {"ここまで続けられるなんて、尊敬する。",
         "もう君の努力は本物だよ。堂々と胸張っていい。",
         "信じて見ててよかったよ、本当に。"},
        // Lv8
        {"ここまで来た君を誰もバカにできない。",
         "地道な積み重ねがここまで美しいものだなんて、思わなかったよ。",
         "“継続できる人”って、君のことなんだね。"},
        // Lv9
        {"君は誰よりも強く、誰よりも誠実な人だ。",
         "今の君なら、何だって叶えられるよ。",
         "君が君であることに、世界中が感謝するレベルだよ。"}};

    String[] cheers = cheersByLevel[level];
    String selectedCheer =
        cheers[new java.util.Random().nextInt(cheers.length)];
    cheerMessageLabel.setText(selectedCheer);
  }

  /**
   * 各ボタンのアクションを設定する。
   * チームリストのクリックイベントや、チーム作成・検索画面への遷移を設定する。
   */
  private void setupButtonActions() {
    // チームリストビューのクリックイベント設定
    // チーム名を選択したらチームトップへ遷移
    teamListView.setOnMouseClicked(event -> {
      if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
        com.habit.domain.Team selectedTeam = teamListView.getSelectionModel().getSelectedItem();
        if (selectedTeam != null) {
          try {
            javafx.stage.Stage stage = (javafx.stage.Stage) teamListView.getScene().getWindow();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/habit/client/gui/TeamTop.fxml"));
            javafx.scene.Parent root = loader.load();
            com.habit.client.TeamTopController controller = loader.getController();
            controller.setTeamName(selectedTeam.getteamName());
            controller.setTeamID(selectedTeam.getTeamID());
            if (userId != null) {
              controller.setUserId(userId);
            }
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("チームトップ");
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      }
    });

    // チーム作成画面遷移ボタンのアクション設定
    btnToCreateTeam.setOnAction(unused -> {
      try {
        javafx.stage.Stage stage =
            (javafx.stage.Stage)btnToCreateTeam.getScene().getWindow();
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
            getClass().getResource("/com/habit/client/gui/CreateTeam.fxml"));
        javafx.scene.Parent root = loader.load();
        com.habit.client.CreateTeamController controller =
            loader.getController();
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
        javafx.stage.Stage stage =
            (javafx.stage.Stage)btnToSearchTeam.getScene().getWindow();
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
            getClass().getResource("/com/habit/client/gui/SearchTeam.fxml"));
        javafx.scene.Parent root = loader.load();
        com.habit.client.SearchTeamController controller =
            loader.getController();
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

  /**
   * 指定されたチームIDのチームを削除する。
   * サーバにDELETEリクエストを送信し、成功したらチーム一覧を再読み込みする。
   *
   * @param teamId 削除するチームのID
   */
  private void deleteTeam(String teamId) {
    new Thread(() -> {
      try {
        HttpClient client = HttpClient.newHttpClient();
        String deleteUrl = Config.getServerUrl() + "/deleteTeam?team_id=" + java.net.URLEncoder.encode(teamId, "UTF-8");
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
            .uri(URI.create(deleteUrl))
            .timeout(java.time.Duration.ofSeconds(3))
            .DELETE();

        String sessionId = LoginController.getSessionId();
        if (sessionId != null && !sessionId.isEmpty()) {
          reqBuilder.header("SESSION_ID", sessionId);
        }

        HttpRequest request = reqBuilder.build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
          Platform.runLater(this::loadJoinedTeams);
        } else {
          Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("エラー");
            alert.setHeaderText("チームの削除に失敗しました");
            alert.setContentText(response.body());
            alert.showAndWait();
          });
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }).start();
  }
}
