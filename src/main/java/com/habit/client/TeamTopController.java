package com.habit.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.CheckBoxTableCell;
import java.net.*;
import java.util.*;
import org.json.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * チームトップ画面のコントローラークラス。
 * チーム名の表示、タスク作成、個人ページやチャットページへの遷移を担当する。
 */
public class TeamTopController {
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

    private final String serverUrl = "http://localhost:8080/sendChatMessage";
    private final String chatLogUrl = "http://localhost:8080/getChatLog";

    /*  遷移時に渡すユーザーIDとチームID, チーム名
     * これらは全てのコントローラが持つようにしてください。
     * 余裕があったら共通化します。
     */
    private String userId;
    private String teamID;
    private String teamName = "チーム名未取得";
    // ユーザIDのセッター
    public void setUserId(String userId) {
        System.out.println("userId set: " + userId);
        this.userId = userId;
    }
    // チームIDのセッター
    public void setTeamID(String teamID) {
        this.teamID = teamID;
        System.out.println("teamID set: " + teamID);
        // teamIDがセットされたタイミングでタスク、チャットを読み込む。
        loadTeamTasksAndUserTasks();
        loadChatLog();
    }
    // チーム名のセッター
    public void setTeamName(String teamName) {
        this.teamName = teamName;
        System.out.println("teamName set: " + teamName);
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
        // googleの適当絵文字、画像を用意してPathを指定してください。
        teamCharView.setImage(new Image(
            "https://raw.githubusercontent.com/google/material-design-icons/master/png/social/mood/materialicons/48dp/2x/baseline_mood_black_48dp.png", true));

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
                // ユーザーのタスク一覧を渡す
                controller.setUserTasks(getUserTasksForPersonalPage());
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

        // タスク進捗表の表示
        loadTaskStatusTable();

        // 応援セリフリスト
        String[] cheers = {
            "今日も一歩前進だね！",
            "君なら絶対できるよ！",
            "小さな積み重ねが大きな力になるよ！",
            "焦らずコツコツいこう！",
            "昨日の自分を超えよう！",
            "休むのも大事、無理しないでね！",
            "一緒に頑張ろう！応援してるよ！",
            "できたことをしっかり褒めてあげて！",
            "継続は力なり、君はすごい！",
            "どんな日も君の味方だよ！"
        };
        java.util.Random rand = new java.util.Random();
        if (cheerMessageLabel != null) {
            cheerMessageLabel.setText(cheers[rand.nextInt(cheers.length)]);
        }
    }


    /**
     * チームタスク・ユーザタスク取得メソッド
     * チームIDがセットされたタイミングで呼び出される。
     */
    private void loadTeamTasksAndUserTasks() {
        new Thread(() -> {
            try {
                // teamIDがnullの場合は処理をスキップ
                if (teamID == null) {
                    System.err.println("teamID is null, skipping loadTeamTasksAndUserTasks");
                    return;
                }
                String sessionId = LoginController.getSessionId();
                HttpClient client = HttpClient.newHttpClient();
                String url = "http://localhost:8080/getUserTeamTasks?teamID=" + URLEncoder.encode(teamID, "UTF-8");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .header("SESSION_ID", sessionId)
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                // レスポンスは [{"taskId":"...","taskName":"..."}] のJSON配列
                java.util.List<String> taskNames = new java.util.ArrayList<>();
                String json = response.body();
                if (json != null && json.startsWith("[")) {
                    org.json.JSONArray arr = new org.json.JSONArray(json);
                    for (int i = 0; i < arr.length(); i++) {
                        org.json.JSONObject obj = arr.getJSONObject(i);
                        String name = obj.optString("taskName", null);
                        if (name != null) {
                            taskNames.add(name);
                        }
                    }
                }
                Platform.runLater(() -> {
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
                System.err.println("teamID is null, returning empty task list");
                return new java.util.ArrayList<>();
            }
            String sessionId = LoginController.getSessionId();
            java.time.LocalDate today = java.time.LocalDate.now();
            HttpClient client = HttpClient.newHttpClient();
            String url = "http://localhost:8080/getUserIncompleteTasks?teamID=" + URLEncoder.encode(teamID, "UTF-8")
                       + "&date=" + today.toString();
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
                    String dueTimeStr = obj.optString("dueTime", null);
                    java.time.LocalTime dueTime = null;
                    if (dueTimeStr != null && !dueTimeStr.isEmpty() && !dueTimeStr.equals("null")) {
                        try {
                            dueTime = java.time.LocalTime.parse(dueTimeStr);
                        } catch (Exception ignore) {}
                    }
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
                        
                        // dueTimeはリフレクションで設定（setterがないため）
                        if (dueTime != null) {
                            try {
                                java.lang.reflect.Field f = t.getClass().getDeclaredField("dueTime");
                                f.setAccessible(true);
                                f.set(t, dueTime);
                            } catch (Exception ignore) {}
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
                    System.err.println("teamID is null, skipping loadChatLog");
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
                List<String> messages = new ArrayList<>();
                JSONArray arr = new JSONArray(response.body());
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String username = obj.optString("username", null);
                    String sender = username != null && !username.isEmpty() ? username : obj.optString("senderId", "unknown");
                    String content = obj.optString("content", "");
                    messages.add(sender + ": " + content);
                }

                Platform.runLater(() -> {
                    chatList.getItems().setAll(messages);
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
                String membersUrl = "http://localhost:8080/getTeamMembers?teamID=" + URLEncoder.encode(teamID, "UTF-8");
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
                    System.out.println("[loadTaskStatusTable] getTeamMembers APIレスポンスが配列形式ではありません: " + membersBody);
                    membersArr = new JSONArray();
                }
                List<String> memberIds = new ArrayList<>();
                List<String> memberNames = new ArrayList<>();
                for (int i = 0; i < membersArr.length(); i++) {
                    JSONObject obj = membersArr.getJSONObject(i);
                    memberIds.add(obj.optString("userId"));
                    memberNames.add(obj.optString("username"));
                }

                // タスク一覧取得（originalTaskIdでグループ化）
                String tasksUrl = "http://localhost:8080/getTeamTasksGrouped?teamID=" + URLEncoder.encode(teamID, "UTF-8");
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
                    System.out.println("[loadTaskStatusTable] getTeamTasksGrouped APIレスポンスが配列形式ではありません: " + tasksBody);
                    tasksArr = new JSONArray();
                }
                List<String> originalTaskIds = new ArrayList<>(); // originalTaskIdを使用
                List<String> taskNames = new ArrayList<>();
                Map<String, String> taskPeriodMap = new HashMap<>(); // originalTaskId→period
                for (int i = 0; i < tasksArr.length(); i++) {
                    JSONObject obj = tasksArr.getJSONObject(i);
                    String originalTaskId = obj.optString("originalTaskId", obj.optString("taskId")); // originalTaskIdがない場合はtaskIdを使用
                    String taskName = obj.optString("taskName");
                    String period = obj.optString("period", "");
                    originalTaskIds.add(originalTaskId);
                    taskNames.add(taskName);
                    taskPeriodMap.put(originalTaskId, period);
                }

                // 進捗一覧取得（全メンバー×タスク×過去7日）
                String date = java.time.LocalDate.now().toString();
                int days = 7;
                String statusUrl = "http://localhost:8080/getTeamTaskStatusList?teamID=" + URLEncoder.encode(teamID, "UTF-8") + "&date=" + date + "&days=" + days;
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
                    System.out.println("[loadTaskStatusTable] getTeamTaskStatusList APIレスポンスが配列形式ではありません: " + statusBody);
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
                        memCol.setPrefWidth(45); // 横幅を半分に
                        // カスタムセル: 日数に応じて色を変化
                        memCol.setCellFactory(tc -> new TableCell<>() {
                            @Override
                            protected void updateItem(Integer daysDone, boolean empty) {
                                super.updateItem(daysDone, empty);
                                if (empty || daysDone == null) {
                                    setText("");
                                    setStyle("");
                                } else {
                                    // originalTaskIDとユーザーIDを取得
                                    int rowIdx = getIndex();
                                    if (rowIdx < 0 || rowIdx >= originalTaskIds.size()) {
                                        setText(""); setStyle(""); return;
                                    }
                                    String originalTid = originalTaskIds.get(rowIdx);
                                    String period = taskPeriodMap.getOrDefault(originalTid, "");
                                    String color = "#ffffff";
                                    if ("毎週".equals(period)) {
                                        // 今週分にisDone==trueが1つでもあれば緑、なければ白
                                        String key = memberIds.get(colIdx-1) + "_" + originalTid;
                                        List<Boolean> doneList = statusMap.getOrDefault(key, Collections.emptyList());
                                        boolean anyDone = false;
                                        for (Boolean b : doneList) if (b) anyDone = true;
                                        color = anyDone ? "#4fc24f" : "#ffffff";
                                    } else {
                                        switch (daysDone) {
                                            case 0: color = "#ffffff"; break;
                                            case 1: color = "#e0f8e0"; break;
                                            case 2: color = "#b2e5b2"; break;
                                            case 3: color = "#7fd87f"; break;
                                            case 4: color = "#4fc24f"; break; // 普通の緑
                                            case 5: color = "#2e9e2e"; break;
                                            case 6: color = "#176b17"; break;
                                            case 7: color = "#0a2d0a"; break; // 黒に近い緑
                                            default: color = "#ffffff";
                                        }
                                    }
                                    setText("");
                                    setStyle("-fx-background-color: " + color + "; -fx-alignment: center; -fx-font-size: 15px; -fx-padding: 4 0;");
                                }
                            }
                        });
                        memCol.setEditable(false);
                        taskTable.getColumns().add(memCol);
                    }
                    // データ行生成（行＝タスク、列＝[タスク名, 各メンバーの7日間達成日数]）
                    javafx.collections.ObservableList<ObservableList<Object>> rows = javafx.collections.FXCollections.observableArrayList();
                    for (int t = 0; t < originalTaskIds.size(); t++) {
                        ObservableList<Object> row = javafx.collections.FXCollections.observableArrayList();
                        row.add(taskNames.get(t)); // 1列目: タスク名
                        String originalTid = originalTaskIds.get(t);
                        for (String uid : memberIds) {
                            String key = uid + "_" + originalTid;
                            List<Boolean> doneList = statusMap.getOrDefault(key, Collections.emptyList());
                            int daysDone = 0;
                            for (Boolean b : doneList) if (b) daysDone++;
                            row.add(daysDone);
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
                    System.err.println("teamID is null, skipping sendChatMessage");
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
}
