package com.habit.client;

import com.habit.domain.util.Config;
import com.habit.domain.Message;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import org.json.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatController {
  private static final Logger logger =
      LoggerFactory.getLogger(ChatController.class);

  private ScheduledExecutorService scheduler;
  private ScheduledFuture<?> chatUpdateFuture;
  private boolean isInitialLoad = true;
  private HttpClient httpClient;
  /* チーム名ラベル */
  @FXML private Label teamNameLabel;
  /* チャットリスト */
  @FXML private ListView<Message> chatList;
  /* チャット入力フィールド */
  @FXML private TextArea chatInput;
  /* チャット送信ボタン */
  @FXML private Button btnSend;
  /* チームトップに戻るボタン */
  @FXML private Button btnBackToTeamTop;

  private final String serverUrl = Config.getServerUrl() +
     "/sendChatMessage";
  private final String chatLogUrl = Config.getServerUrl() +
     "/getChatLog";

  // 遷移時に渡すデータとセッター
  private String userId;
  private String teamID;
  private String teamName = "チーム名未取得";
  private String creatorId;
  private com.habit.domain.Team team;

  // creatorIdのセッター
  public void setCreatorId(String creatorId) {
    logger.info("creatorId set: " + creatorId);
    this.creatorId = creatorId;
  }

  public void setUserId(String userId) { this.userId = userId; }

  public void setTeamID(String teamID) {
    this.teamID = teamID;
    fetchAndSetTeamName(teamID);
    loadChatLog(true); // teamIDがセットされた後に履歴を取得 (初回呼び出し)

    // ポーリングを開始
    if (scheduler != null && (chatUpdateFuture == null || chatUpdateFuture.isDone())) {
      chatUpdateFuture = scheduler.scheduleAtFixedRate(() -> loadChatLog(false), 0, 3, TimeUnit.SECONDS);
    }

    }

  public void setTeamName(String teamName) {
    this.teamName = teamName;
    if (teamNameLabel != null) {
      teamNameLabel.setText(teamName);
    }
  }

  public void setTeam(com.habit.domain.Team team) {
    this.team = team;
  }

  /**
   * チームIDに基づいてサーバーからチーム名を取得し、ラベルに設定するメソッド。
   * チームIDがセットされたタイミングで呼び出される。
   */
  private void fetchAndSetTeamName(String teamID) {
    scheduler.execute(() -> {
      try {
        String urlStr = Config.getServerUrl() + "/getTeamName?teamID=" +
            URLEncoder.encode(teamID, "UTF-8");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(urlStr))
            .timeout(java.time.Duration.ofSeconds(3))
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String name = response.body();
        if (name != null && !name.isEmpty()) {
          javafx.application.Platform.runLater(() -> {
            teamName = name;
            if (teamNameLabel != null) {
              teamNameLabel.setText(teamName);
            }
          });
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  /**
   * コントローラー初期化処理。
   * チーム名の設定やボタンのアクション設定を行う。
   */
  @FXML
  public void initialize() {
    scheduler = Executors.newSingleThreadScheduledExecutor();
    httpClient = HttpClient.newHttpClient();

    // loadChatLog()はここで呼ばない
    // チーム名がセットされている場合はラベルに表示
    if (teamNameLabel != null && teamName != null) {
      teamNameLabel.setText(teamName);
    }

    // ListViewのセルファリを設定
    chatList.setCellFactory(lv -> new ListCell<Message>() {
      private final ContextMenu contextMenu = new ContextMenu();
      private final MenuItem deleteItem = new MenuItem("削除");

      {
        deleteItem.setOnAction(event -> {
          Message selectedMessage = getItem();
          if (selectedMessage != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("確認");
            alert.setHeaderText("メッセージの削除");
            alert.setContentText("本当にこのメッセージを削除しますか？");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
              deleteChatMessage(selectedMessage.getMessageId());
            }
          }
        });
        deleteItem.getStyleClass().add("delete-menu-item");
        contextMenu.getItems().add(deleteItem);
      }

      @Override
      protected void updateItem(Message message, boolean empty) {
        super.updateItem(message, empty);
        if (empty || message == null) {
          setText(null);
          setGraphic(null);
          setContextMenu(null);
        } else {
          // VBox for layout (the message bubble itself)
          VBox messageBubble = new VBox(2);
          Label senderLabel = new Label(message.getSender().getUsername());
          Label contentLabel = new Label(message.getContent());
          contentLabel.setWrapText(true);
          Label timestampLabel = new Label(
              message.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

          senderLabel.getStyleClass().add("sender-label");
          contentLabel.getStyleClass().add("content-label");
          timestampLabel.getStyleClass().add("timestamp-label");

          messageBubble.getChildren().addAll(senderLabel, contentLabel, timestampLabel);
          messageBubble.setMaxWidth(300); // Adjust as needed for wrapping

          // HBox for alignment (left or right)
          HBox messageContainer = new HBox();
          messageContainer.getChildren().add(messageBubble);

          if (message.getSender().getUserId().equals(userId)) {
            messageContainer.getStyleClass().add("sent-message-container");
            messageBubble.getStyleClass().add("my-message-bubble");
            setContextMenu(contextMenu);
          } else {
            messageContainer.getStyleClass().add("received-message-container");
            messageBubble.getStyleClass().add("other-message-bubble");
            setContextMenu(null);
          }
          setGraphic(messageContainer);
          setText(null);
        }
      }
    });

    // チャット送信ボタンのアクション設定
    btnSend.setOnAction(unused -> {
      String msg = chatInput.getText();
      if (msg != null && !msg.isEmpty()) {
        sendChatMessage(msg);
        chatInput.clear();
      }
    });

    // チームトップに戻るボタンのアクション設定
    btnBackToTeamTop.setOnAction(unused -> {
      try {
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
            getClass().getResource("/com/habit/client/gui/TeamTop.fxml"));
        javafx.scene.Parent root = loader.load();
        TeamTopController controller = loader.getController();
        controller.setUserId(userId);
        controller.setTeamID(teamID);
        controller.setTeamName(teamName);
        controller.setCreatorId(creatorId);
        controller.setTeam(team);
        javafx.stage.Stage stage = (javafx.stage.Stage) btnBackToTeamTop.getScene().getWindow();
        stage.setScene(new javafx.scene.Scene(root));
        stage.setTitle("チームトップ");
        // ポーリングを停止
        if (chatUpdateFuture != null) {
          chatUpdateFuture.cancel(true);
        }
        // schedulerをシャットダウン
        if (scheduler != null && !scheduler.isShutdown()) {
          scheduler.shutdownNow();
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    });

    // アプリケーション終了時にschedulerをシャットダウン
    Platform.runLater(() -> {
      btnBackToTeamTop.getScene().getWindow().setOnHidden(event -> {
        if (scheduler != null && !scheduler.isShutdown()) {
          scheduler.shutdownNow();
        }
      });
    });
  }

  /**
   * チャットログをサーバーから取得し、リストに表示するメソッド。
   * 新しいスレッドで実行される。
   */
  private void loadChatLog(boolean isInitialCall) {
    scheduler.execute(() -> {
      try {
        String url = chatLogUrl +
                     "?teamID=" + URLEncoder.encode(teamID, "UTF-8") +
                     "&limit=50";
        HttpRequest request = HttpRequest.newBuilder()
                                  .uri(URI.create(url))
                                  .timeout(java.time.Duration.ofSeconds(3))
                                  .GET()
                                  .build();
        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        List<Message> messages = new ArrayList<>();
        JSONArray arr = new JSONArray(response.body());
        for (int i = 0; i < arr.length(); i++) {
          JSONObject obj = arr.getJSONObject(i);
          messages.add(Message.fromJson(obj));
        }

        // sort according to time stamp
        messages.sort(Comparator.comparing(Message::getTimestamp));

        Platform.runLater(() -> {
          chatList.getItems().setAll(messages);
          if (isInitialCall && !messages.isEmpty()) {
            chatList.scrollTo(messages.size() - 1);
          }
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  /**
   * チャットメッセージをサーバーに送信するメソッド。
   * 新しいスレッドで実行される。
   *
   * @param message 送信するチャットメッセージ
   */
  private void sendChatMessage(String message) {
    scheduler.execute(() -> {
      try {
        String params = "senderId=" + URLEncoder.encode(userId, "UTF-8") +
                        "&teamID=" + URLEncoder.encode(teamID, "UTF-8") +
                        "&content=" + URLEncoder.encode(message, "UTF-8");
        HttpRequest request =
            HttpRequest.newBuilder()
                .uri(URI.create(serverUrl))
                .timeout(java.time.Duration.ofSeconds(3))
                .header("Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(params))
                .build();
        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
          loadChatLog(false);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  /**
   * チャットメッセージをサーバーから削除するメソッド。 
   * 新しいスレッドで実行される。
   *
   * @param messageId 削除するメッセージのID
   */
  private void deleteChatMessage(String messageId) {
    scheduler.execute(() -> {
      try {
        String deleteUrl = Config.getServerUrl() + "/deleteChatMessage?message_id=" + URLEncoder.encode(messageId, "UTF-8");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(deleteUrl))
            .timeout(java.time.Duration.ofSeconds(3))
            .DELETE()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
          loadChatLog(false);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }
}
