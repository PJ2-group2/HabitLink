package com.habit.server;

import com.habit.domain.util.Config;
// 習慣化共有プログラムのサーバ側プログラム
// クライアントからのHTTPリクエストを受けて、チームやタスクの情報を管理します
// サーバはSQLiteを用いてチーム・タスク情報を永続化します
import com.habit.server.controller.AuthController;
import com.habit.server.controller.HelloController;
import com.habit.server.controller.MessageController;
import com.habit.server.controller.TaskAutoResetController;
import com.habit.server.controller.TaskController;
import com.habit.server.controller.TeamController;
import com.habit.server.controller.TeamTaskController;
import com.habit.server.controller.UserController;
import com.habit.server.controller.UserTaskStatusController;
import com.habit.server.repository.MessageRepository;
import com.habit.server.repository.TaskRepository;
import com.habit.server.repository.TeamRepository;
import com.habit.server.repository.UserRepository;
import com.habit.server.repository.UserTaskStatusRepository;
import com.habit.server.scheduler.TaskAutoResetScheduler;
import com.habit.server.service.AuthService;
import com.habit.server.service.TaskAutoResetService;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// JDBC based team management
// import com.habit.server.DatabaseTeamManager;

/**
 * 習慣化共有アプリのサーバ本体クラス。
 * HTTPリクエストを受けてチーム・タスク・ユーザ管理など各種APIを提供する。
 * SQLiteによる永続化や、チャット・チーム機能も実装。
 */
public class HabitServer {

  private static final Logger logger =
      LoggerFactory.getLogger(HabitServer.class);

  private static UserRepository userRepository = new UserRepository();
  private static TaskRepository taskRepository = new TaskRepository();
  private static TeamRepository teamRepository = new TeamRepository();
  private static UserTaskStatusRepository userTaskStatusRepository =
      new UserTaskStatusRepository();

  private static TaskController taskController;

  // ユーザ認証用サービス
  private static AuthService authService = new AuthService(userRepository);

  // チャットサービス用リポジトリ
  private static MessageRepository messageRepository = new MessageRepository();
  private static MessageController messageController =
      new MessageController(messageRepository, userRepository);

  private static UserController userController =
      new UserController(authService, teamRepository);

  private static UserTaskStatusController userTaskStatusController =
      new UserTaskStatusController(authService, userTaskStatusRepository);

  // タスク自動再設定関連コンポーネント
  // スケジューラー: 自動実行を担当
  private static TaskAutoResetService taskAutoResetService;
  private static TaskAutoResetScheduler taskAutoResetScheduler;

  // チーム共通タスク管理関連コンポーネント
  private static TeamTaskController teamTaskController =
      new TeamTaskController();
  private static TaskAutoResetController taskAutoResetController;

  public static void main(String[] args) throws Exception {
    final boolean is_debug = Config.getIsDebug();
    logger.debug("debug: {}", is_debug);

    // サーバを8080番ポートで起動
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

    // === タスク自動再設定機能の開始 ===
    // Clockを生成（本番環境ではシステムデフォルトの時刻を使用）
    Clock clock = Clock.systemDefaultZone();
    // ServiceとSchedulerを初期化
    taskAutoResetService =
        new TaskAutoResetService(taskRepository, userTaskStatusRepository,
                                 userRepository, messageRepository, clock);
    taskAutoResetScheduler = new TaskAutoResetScheduler(taskAutoResetService);
    taskAutoResetController = new TaskAutoResetController(
        taskAutoResetService, taskAutoResetScheduler);

    // サーバー起動時に未処理のタスク更新を実行
    taskAutoResetService.catchUpMissedExecutions();

    // 各APIエンドポイントを登録
    server.createContext("/hello", new HelloController()); // 動作確認用

    taskController = new TaskController(taskRepository, teamRepository,
                                        userTaskStatusRepository);

    AuthController authController = new AuthController(authService);
    server.createContext("/login",
                         authController.getLoginHandler()); // ログイン
    server.createContext("/register",
                         authController.getRegisterHandler()); // 新規登録
    TeamController teamController =
        new TeamController(authService, userRepository, taskRepository);
    server.createContext("/createTeam",
                         teamController.getCreateTeamHandler()); // チーム作成
    server.createContext("/joinTeam",
                         teamController.getJoinTeamHandler()); // チーム参加
    server.createContext(
        "/publicTeams",
        teamController.getPublicTeamsHandler()); // 公開チーム一覧
    server.createContext(
        "/findTeamByPasscode",
        teamController.getFindTeamByPasscodeHandler()); // 合言葉検索
    server.createContext(
        "/getTeamIdByPasscode",
        teamController
            .getGetTeamIdByPasscodeHandler()); // 合言葉からチームID取得
    server.createContext(
        "/sendChatMessage",
        messageController.getSendChatMessageHandler()); // チャット送信
    server.createContext(
        "/getChatLog",
        messageController.getGetChatLogHandler()); // チャット履歴取得
    server.createContext(
        "/getJoinedTeamInfo",
        userController.getGetJoinedTeamInfoHandler()); // 参加チーム取得
    server.createContext(
        "/getSabotagePoints",
        userController.getSabotagePointsHandler()); // サボりポイント取得
    server.createContext(
        "/updateSabotagePoints",
        userController
            .getUpdateSabotagePointsHandler()); // サボりポイント更新（テスト用）
    server.createContext(
        "/getUserTaskIds",
        userTaskStatusController
            .getGetUserTaskIdsHandler()); // UserTaskStatusからTaskId取得
    server.createContext(
        "/getTeamName", teamController.getGetTeamNameHandler()); // チーム名取得
    // タスクID→タスク名マップ取得API
    server.createContext("/getUserTeamTasks",
                         taskController.getUserTeamTasksHandler(
                             authService)); // チーム内で自分に紐づくタスク取得
    server.createContext("/getTaskIdNameMap",
                         taskController.getTaskIdNameMapHandler());
    // ユーザーの未完了タスク一覧取得API
    server.createContext(
        "/getUserIncompleteTasks",
        userTaskStatusController.getUserIncompleteTasksHandler(authService));
    // ユーザーの未完了タスク(isDone=false, dueDate > now)一覧取得API
    server.createContext(
        "/getIncompleteUserTaskStatus",
        userTaskStatusController.getIncompleteUserTaskStatusHandler(
            authService));
    // チーム全員分のタスク進捗一覧API
    server.createContext(
        "/getTeamTaskStatusList",
        userTaskStatusController.getGetTeamTaskStatusListHandler());
    // ユーザー・チーム・日付ごとの全UserTaskStatus（taskId, isDone）を返すAPI
    server.createContext(
        "/getUserTaskStatusList",
        userTaskStatusController.getGetUserTaskStatusListHandler());
    // ユーザーのタスク完了API
    server.createContext("/completeUserTask",
                         userTaskStatusController.getCompleteUserTaskHandler());
    // タスク保存API
    server.createContext("/saveTask", taskController.getSaveTaskHandler());

    if (is_debug) {
      // UserTaskStatus保存API
      // タスク自動再設定手動実行API
      server.createContext(
          "/manualTaskReset",
          taskAutoResetController.getManualResetHandler()); // 全チーム手動実行
      server.createContext(
          "/manualTaskResetTeam",
          taskAutoResetController
              .getManualResetTeamHandler()); // 特定チーム手動実行

      server.createContext(
          "/debugScheduledReset",
          taskAutoResetController
              .getDebugScheduledResetHandler()); // デバッグ用スケジュール実行
      server.createContext(
          "/debugSabotageReport",
          taskAutoResetController
              .getDebugSabotageReportHandler()); // デバッグ用サボり報告（今日まで）
    }

    server.createContext(
        "/saveUserTaskStatus",
        userTaskStatusController.getSaveUserTaskStatusHandler());
    server.createContext(
        "/getTeamMembers",
        teamController.getGetTeamMembersHandler()); // チームメンバー一覧
    server.createContext(
        "/getTeamTasks",
        teamController.getGetTeamTasksHandler()); // チームタスク一覧
    server.createContext(
        "/getTeamSabotageRanking",
        teamController
            .getGetTeamSabotageRankingHandler()); // チーム内サボりランキング

    // チーム共通タスク管理API
    server.createContext(
        "/getTeamTaskCompletionRate",
        teamTaskController
            .getTeamTaskCompletionRateHandler()); // チーム共通タスクの完了率取得
    server.createContext(
        "/getUserTeamTaskStatuses",
        teamTaskController
            .getUserTeamTasksHandler()); // ユーザーのチーム共通タスク一覧取得

    server.setExecutor(null);
    server.start();

    // 自動実行スケジューラーを開始
    try {
      logger.info("タスク自動再設定スケジューラーを開始します...");
      taskAutoResetScheduler.start();
      logger.info("タスク自動再設定スケジューラーが正常に開始されました。");
    } catch (Exception e) {
      logger.error("タスク自動再設定スケジューラーの開始に失敗しました: {}",
                   e.getMessage(), e);
      // スケジューラーが失敗してもサーバー自体は起動を継続
    }

    // サーバーシャットダウン時の処理を登録
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("サーバをシャットダウンしています...");
      // スケジューラーを安全に停止
      taskAutoResetScheduler.stop();
      // HTTPサーバーを停止
      server.stop(0);
    }));

    logger.info("サーバが起動しました: http://localhost:8080/hello");
    logger.info("タスク自動再設定機能が有効になりました");

    if (is_debug) {
      logger.info(
          "手動実行API: /manualTaskReset, /manualTaskResetTeam?teamId=xxx");
      logger.info(
          "デバッグAPI: /debugScheduledReset?delay=秒数, /debugSabotageReport");
    }
  }
}
