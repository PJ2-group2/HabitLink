package com.habit.server;

// 習慣化共有プログラムのサーバ側プログラム
// クライアントからのHTTPリクエストを受けて、チームやタスクの情報を管理します
// サーバはSQLiteを用いてチーム・タスク情報を永続化します

import com.sun.net.httpserver.HttpServer;
import com.habit.server.controller.HelloController;
import com.habit.server.controller.TaskController;
import com.habit.server.controller.TeamController;
import com.habit.server.controller.AuthController;
import com.habit.server.controller.MessageController;
import com.habit.server.controller.UserController;
import com.habit.server.controller.UserTaskStatusController;
import com.habit.server.manager.DatabaseTeamManager;
import com.habit.server.repository.MessageRepository;
import com.habit.server.repository.TaskRepository;
import com.habit.server.repository.UserRepository;
import com.habit.server.repository.UserTaskStatusRepository;
import com.habit.server.service.AuthService;

import java.net.InetSocketAddress;

// JDBC based team management
// import com.habit.server.DatabaseTeamManager;

/**
 * 習慣化共有アプリのサーバ本体クラス。
 * HTTPリクエストを受けてチーム・タスク・ユーザ管理など各種APIを提供する。
 * SQLiteによる永続化や、チャット・チーム機能も実装。
 */
public class HabitServer {
  private static TaskController taskController;
  // ユーザ認証用サービス
  private static UserRepository userRepository = new UserRepository();
  private static AuthService authService = new AuthService(userRepository);
  // チャットサービス用リポジトリ
  private static MessageRepository messageRepository = new MessageRepository();
  private static MessageController messageController = new MessageController(messageRepository, userRepository);

  private static UserController userController = new UserController(authService, userRepository);

  private static UserTaskStatusRepository userTaskStatusRepository = new UserTaskStatusRepository();
  private static UserTaskStatusController userTaskStatusController = new UserTaskStatusController(authService, userTaskStatusRepository);
  // --- チーム管理用（SQLite でチームIDを保持）---
  private static DatabaseTeamManager teamManager = new DatabaseTeamManager("jdbc:sqlite:habit.db");
  private static TaskRepository taskRepository = new TaskRepository();

  public static void main(String[] args) throws Exception {
    // サーバを8080番ポートで起動
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
    // 各APIエンドポイントを登録
    server.createContext("/hello", new HelloController());           // 動作確認用

    taskController = new TaskController(teamManager);
    server.createContext("/addTask", taskController.getAddTaskHandler());       // タスク追加
    server.createContext("/getTasks", taskController.getGetTasksHandler()); // タスク一覧取得
    AuthController authController = new AuthController(authService, userRepository);
    server.createContext("/login", authController.getLoginHandler());           // ログイン
    server.createContext("/register", authController.getRegisterHandler());     // 新規登録
    TeamController teamController = new TeamController(authService, userRepository, taskRepository);
    server.createContext("/createTeam", teamController.getCreateTeamHandler());   // チーム作成
    server.createContext("/joinTeam", teamController.getJoinTeamHandler());     // チーム参加
    server.createContext("/publicTeams", teamController.getPublicTeamsHandler()); // 公開チーム一覧
    server.createContext("/findTeamByPasscode", teamController.getFindTeamByPasscodeHandler()); // 合言葉検索
    server.createContext("/sendChatMessage", messageController.getSendChatMessageHandler()); // チャット送信
    server.createContext("/getChatLog", messageController.getGetChatLogHandler()); // チャット履歴取得
    server.createContext("/getJoinedTeamInfo", userController.getGetJoinedTeamInfoHandler()); // 参加チーム取得
    server.createContext("/getUserTaskIds", userTaskStatusController.getGetUserTaskIdsHandler()); // UserTaskStatusからTaskId取得
    server.createContext("/getTeamName", teamController.getGetTeamNameHandler()); // チーム名取得
    // タスクID→タスク名マップ取得API
    server.createContext("/getUserTeamTasks", taskController.getUserTeamTasksHandler(authService)); // チーム内で自分に紐づくタスク取得
    server.createContext("/getTaskIdNameMap", taskController.getTaskIdNameMapHandler());
    server.setExecutor(null);
    // ユーザーの未完了タスク一覧取得API
    server.createContext("/getUserIncompleteTasks", userTaskStatusController.getUserIncompleteTasksHandler(authService));
    // ユーザー・チーム・日付ごとの全UserTaskStatus（taskId, isDone）を返すAPI
    server.createContext("/getUserTaskStatusList", userTaskStatusController.getGetUserTaskStatusListHandler());
    // ユーザーのタスク完了API
    server.createContext("/completeUserTask", userTaskStatusController.getCompleteUserTaskHandler());
    // タスク保存API
    server.createContext("/saveTask", taskController.getSaveTaskHandler());
    // UserTaskStatus保存API
    server.createContext("/saveUserTaskStatus", userTaskStatusController.getSaveUserTaskStatusHandler());
    server.createContext("/getTeamMembers", teamController.getGetTeamMembersHandler()); // チームメンバー一覧
    server.createContext("/getTeamTasks", teamController.getGetTeamTasksHandler()); // チームタスク一覧
    server.start();
    System.out.println("サーバが起動しました: http://localhost:8080/hello");
  }
}