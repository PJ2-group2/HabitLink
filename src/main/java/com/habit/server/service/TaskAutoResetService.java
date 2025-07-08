package com.habit.server.service;

import com.habit.domain.Message;
import com.habit.domain.Task;
import com.habit.domain.User;
import com.habit.domain.UserTaskStatus;
import com.habit.server.repository.MessageRepository;
import com.habit.server.repository.TaskRepository;
import com.habit.server.repository.UserRepository;
import com.habit.server.repository.UserTaskStatusRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * タスクの自動再設定サービス
 *  タスクが期限を過ぎても未完了の場合、次回サイクルのタスクを自動生成
 *  手動実行も可能（TaskAutoResetControllerのAPI経由）
 *  既に同じ日付のタスクが存在する場合は重複作成しない
 */
public class TaskAutoResetService {
    private static final Logger logger = LoggerFactory.getLogger(TaskAutoResetService.class);
    private final TaskRepository taskRepository;
    private final UserTaskStatusRepository userTaskStatusRepository;
    private final UserRepository userRepository; // UserRepositoryを追加
    private final MessageRepository messageRepository; // MessageRepositoryを追加
    private final Clock clock;
    private static final Path LAST_EXECUTION_FILE = Paths.get("last_execution.log");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    // 重複実行防止用のフラグ
    private volatile boolean isRunning = false;

    // システムメッセージ用の固定ユーザー
    private static final User SERVER_USER = new User("system", "System", "");

    /**
     * コンストラクタ
     */ 
    public TaskAutoResetService(TaskRepository taskRepository, UserTaskStatusRepository userTaskStatusRepository, UserRepository userRepository, MessageRepository messageRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.userTaskStatusRepository = userTaskStatusRepository;
        this.userRepository = userRepository; // UserRepositoryを初期化
        this.messageRepository = messageRepository; // MessageRepositoryを初期化
        this.clock = clock;
    }

    public void catchUpMissedExecutions() {
        LocalDate lastExecutionDate = loadLastExecutionTime();
        LocalDate today = LocalDate.now(clock);

        if (lastExecutionDate == null) {
            lastExecutionDate = today.minusDays(1); //
        }

        List<LocalDate> missedDates = lastExecutionDate.plusDays(1).datesUntil(today.plusDays(1))
                                       .collect(Collectors.toList());

        if (!missedDates.isEmpty()) {
            logger.info("サーバー停止期間中の未処理のタスク更新を開始します: " + missedDates.size() + "日分");
            for (LocalDate date : missedDates) {
                logger.info(date + " のタスクを更新します。");
                runScheduledCheckForDate(date);
            }
            logger.info("未処理のタスク更新が完了しました。");
        }
    }
    
    /**
     * 定期実行用：全チームのタスクをチェック
     *  TaskAutoResetSchedulerにより毎日午前0時に自動実行
     *  TaskAutoResetControllerのAPIからも呼び出し可能
     *
     * 【処理フロー】
     * 1. 重複実行防止チェック
     * 2. 全チームIDを取得
     * 3. 各チームのタスクを順次チェック・再設定
     * 4. 実行結果をログ出力（処理チーム数、再設定タスク数）
     */
    public void runScheduledCheck() {
        runScheduledCheckForDate(LocalDate.now(clock));
    }

    public void runScheduledCheckForDate(LocalDate date) {
        // 重複実行防止（前回の処理がまだ終わっていない場合はスキップ）
        if (isRunning) {
            logger.info("自動再設定処理が実行中のため、今回はスキップします");
            return;
        }
        
        isRunning = true;
        try {
            // TeamRepositoryから全チームIDを取得
            com.habit.server.repository.TeamRepository teamRepository =
                new com.habit.server.repository.TeamRepository();
            List<String> allTeamIds = teamRepository.findAllTeamIds();
            
            logger.info("自動再設定チェック開始: " + allTeamIds.size() + "チーム対象 at " +
                java.time.LocalDateTime.now(clock));
            
            int processedTeams = 0;
            int totalResets = 0;
            
            // 各チームを順次処理
            for (String teamId : allTeamIds) {
                try {
                    int resets = checkAndResetTasks(teamId, date); // checkAndResetTasksWithCount から checkAndResetTasks に変更
                    totalResets += resets;
                    processedTeams++;
                } catch (Exception e) {
                    logger.error("チーム " + teamId + " の自動再設定でエラー: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            logger.info("自動再設定チェック完了: " + processedTeams + "チームで処理, " +
                totalResets + "タスクを再設定 at " + java.time.LocalDateTime.now(clock));
            
            saveLastExecutionTime(date);
        } catch (Exception e) {
            logger.error("自動再設定の定期実行でエラー: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 必ず実行フラグをリセット
            isRunning = false;
        }
    }
    
    /**
     * 指定チームの全タスクを自動再設定チェック
     *
     * @param teamId 対象チームID
     * @return 再設定されたタスクの数
     *
     * 【処理フロー】
     * 1. 指定チームの全タスクを取得
     * 2. 各タスクに対して再設定チェックを実施
     * 3. 対象タスクの全ユーザー分をチェック・再設定
     */
    public int checkAndResetTasks(String teamId, LocalDate executionDate) { // private から public に変更
        List<Task> teamTasks = taskRepository.findTeamTasksByTeamID(teamId);
        int resetCount = 0;
        
        logger.info("[DEBUG] チーム " + teamId + " のタスク数: " + teamTasks.size());
        logger.info("[DEBUG] 実行日: " + executionDate + ", チェック対象日(前日): " + executionDate.minusDays(1));

        for (Task task : teamTasks) {
            logger.info("[DEBUG] タスク処理開始: " + task.getTaskName() + " (ID: " + task.getTaskId() + ")");
            
            String cycleType = task.getCycleType();
            logger.info("[DEBUG] cycleType: " + cycleType);
            
            if (cycleType == null) {
                logger.info("[DEBUG] cycleTypeがnullのためスキップ: " + task.getTaskName());
                continue; // 繰り返し設定のないタスクはスキップ
            }

            LocalDate dateToCheck = executionDate.minusDays(1); // チェック対象は実行日の前日
            LocalDate newDueDate = null;

            switch (cycleType.toLowerCase()) {
                case "daily":
                    newDueDate = executionDate;
                    break;
                case "weekly":
                    newDueDate = dateToCheck.plusWeeks(1); // 基準日（昨日）から1週間後
                    break;
                default:
                    logger.info("[DEBUG] 未対応のcycleTypeのためスキップ: " + cycleType + " (タスク: " + task.getTaskName() + ")");
                    continue; // 未対応のサイクルタイプはスキップ
            }

            logger.info("[DEBUG] 新しい期限日: " + newDueDate);

            // Task自体のdueDateを更新して保存
            task.setDueDate(newDueDate);
            taskRepository.save(task);

            // 昨日の日付でUserTaskStatusを検索
            List<UserTaskStatus> statusesToCheck = userTaskStatusRepository.findByTaskIdAndDate(task.getTaskId(), dateToCheck);
            logger.info("[DEBUG] " + dateToCheck + " のUserTaskStatus数: " + statusesToCheck.size());

            if (!statusesToCheck.isEmpty()) {
                logger.info("[INFO] " + dateToCheck + " の未処理UserTaskStatus件数: " + statusesToCheck.size());
                
                for (UserTaskStatus oldStatus : statusesToCheck) {
                    logger.info("[DEBUG] UserTaskStatus処理開始: userId=" + oldStatus.getUserId() +
                                     ", isDone=" + oldStatus.isDone() + ", teamId=" + oldStatus.getTeamId() +
                                     ", date=" + oldStatus.getDate());
                    
                    // isDoneを判定
                    com.habit.domain.User user = userRepository.findById(oldStatus.getUserId());
                    if (user != null) {
                        int currentPoints = user.getSabotagePoints();
                        int newPoints;
                        int changeAmount;

                        if(oldStatus.isDone()) {
                            // 完了していたらポイントを減らす（0未満にはしない）
                            newPoints = Math.max(0, currentPoints - 1);
                            changeAmount = newPoints - currentPoints;
                            logger.info("[INFO] タスク完了により " + user.getUsername() + " のサボりポイントを減少");
                        } else {
                            // 未完了ならポイントを増やす（9を超えない）
                            newPoints = Math.min(9, currentPoints + 1);
                            changeAmount = newPoints - currentPoints;
                            logger.info("[INFO] タスク未完了により " + user.getUsername() + " のサボりポイントを増加 → サボり報告メッセージを送信");

                            // サボり報告メッセージを送信
                            try {
                                String reportMessage = user.getUsername() + "さんが昨日のタスク「" + task.getTaskName() + "」をサボりました。";
                                Message systemMessage = new Message(SERVER_USER, oldStatus.getTeamId(), reportMessage, LocalDateTime.now(clock));
                                
                                logger.info("[DEBUG] メッセージ保存前: " +
                                                  "sender=" + systemMessage.getSender().getUserId() +
                                                  ", teamId=" + oldStatus.getTeamId() +
                                                  ", content=" + systemMessage.getContent());
                                
                                messageRepository.save(systemMessage);
                                
                                logger.info("[SUCCESS] サボり報告メッセージ送信完了: チーム=" + oldStatus.getTeamId() +
                                                  ", ユーザー=" + user.getUsername() +
                                                  ", タスク=" + task.getTaskName() +
                                                  ", 実行時刻=" + LocalDateTime.now(clock));
                            } catch (Exception messageException) {
                                logger.error("[ERROR] サボり報告メッセージの送信に失敗: チーム=" + oldStatus.getTeamId() +
                                                  ", ユーザー=" + user.getUsername() +
                                                  ", タスク=" + task.getTaskName() +
                                                  ", エラー=" + messageException.getMessage());
                                messageException.printStackTrace();
                            }
                        }
                        
                        try {
                            user.setSabotagePoints(newPoints);
                            userRepository.save(user);
                            logger.info("[SUCCESS] " + user.getUsername() + " のサボりポイントを " + currentPoints + " Ptから " + newPoints + " Ptに変更 (変動量: " + (changeAmount > 0 ? "+" : "") + changeAmount + ")");
                        } catch (Exception userSaveException) {
                            logger.error("[ERROR] ユーザーのサボりポイント保存に失敗: " + user.getUsername() + ", エラー=" + userSaveException.getMessage());
                            userSaveException.printStackTrace();
                        }
                    } else {
                        logger.error("[ERROR] ユーザーが見つかりません: userId=" + oldStatus.getUserId());
                    }

                    // 新しいdueDateでisDoneがfalseのUserTaskStatusを生成
                    UserTaskStatus newStatus = new UserTaskStatus(
                        oldStatus.getUserId(),
                        task.getTaskId(),
                        task.getTeamId(),
                        newDueDate,
                        false // 初期状態は未完了
                    );

                    // 重複チェック
                    Optional<UserTaskStatus> existingStatus = userTaskStatusRepository.findByUserIdAndTaskIdAndDate(
                        newStatus.getUserId(), newStatus.getTaskId(), newStatus.getDate());

                    if (existingStatus.isEmpty()) {
                        userTaskStatusRepository.save(newStatus);
                        resetCount++;
                        logger.info("新しいUserTaskStatusを生成: userId=" + newStatus.getUserId() +
                            ", taskId=" + newStatus.getTaskId() +
                            ", date=" + newStatus.getDate());
                    } else {
                        logger.info("UserTaskStatusは既に存在します。スキップ: userId=" + newStatus.getUserId() +
                            ", taskId=" + newStatus.getTaskId() +
                            ", date=" + newStatus.getDate());
                    }
                }
            } else {
                logger.info("[DEBUG] " + dateToCheck + " の日付でUserTaskStatusが見つからないため、タスク「" + task.getTaskName() + "」をスキップ");
            }
        }
        return resetCount;
    }

    /**
     * デバッグ用：「今日まで」の未消化タスクでサボり報告を送信
     * 通常の処理は「昨日まで」だが、デバッグ時は「今日まで」をチェック
     *
     * @param teamId 対象チームID
     * @param executionDate 実行日（今日の日付）
     * @return 処理されたタスクの数
     */
    public int checkAndReportTasksForToday(String teamId, LocalDate executionDate) {
        List<Task> teamTasks = taskRepository.findTeamTasksByTeamID(teamId);
        int processedCount = 0;
        
        logger.info("[DEBUG] デバッグ用サボり報告チェック開始: チーム " + teamId + " のタスク数: " + teamTasks.size());
        logger.info("[DEBUG] 実行日: " + executionDate + ", チェック対象日(今日): " + executionDate);

        for (Task task : teamTasks) {
            logger.info("[DEBUG] タスク処理開始: " + task.getTaskName() + " (ID: " + task.getTaskId() + ")");
            
            String cycleType = task.getCycleType();
            logger.info("[DEBUG] cycleType: " + cycleType);
            
            if (cycleType == null) {
                logger.info("[DEBUG] cycleTypeがnullのためスキップ: " + task.getTaskName());
                continue; // 繰り返し設定のないタスクはスキップ
            }

            LocalDate dateToCheck = executionDate; // チェック対象は実行日（今日）

            // 今日の日付でUserTaskStatusを検索
            List<UserTaskStatus> statusesToCheck = userTaskStatusRepository.findByTaskIdAndDate(task.getTaskId(), dateToCheck);
            logger.info("[DEBUG] " + dateToCheck + " のUserTaskStatus数: " + statusesToCheck.size());

            if (!statusesToCheck.isEmpty()) {
                logger.info("[INFO] " + dateToCheck + " の未処理UserTaskStatus件数: " + statusesToCheck.size());
                
                for (UserTaskStatus status : statusesToCheck) {
                    logger.info("[DEBUG] UserTaskStatus処理開始: userId=" + status.getUserId() +
                                     ", isDone=" + status.isDone() + ", teamId=" + status.getTeamId() +
                                     ", date=" + status.getDate());
                    
                    // 未完了の場合のみサボり報告メッセージを送信
                    if (!status.isDone()) {
                        com.habit.domain.User user = userRepository.findById(status.getUserId());
                        if (user != null) {
                            logger.info("[INFO] デバッグ用サボり報告: " + user.getUsername() + " のタスク「" + task.getTaskName() + "」が未完了");

                            // サボり報告メッセージを送信
                            try {
                                String reportMessage = "[デバッグ] " + user.getUsername() + "さんが今日のタスク「" + task.getTaskName() + "」をサボりました。";
                                Message systemMessage = new Message(SERVER_USER, status.getTeamId(), reportMessage, LocalDateTime.now(clock));
                                
                                logger.info("[DEBUG] メッセージ保存前: " +
                                                  "sender=" + systemMessage.getSender().getUserId() +
                                                  ", teamId=" + status.getTeamId() +
                                                  ", content=" + systemMessage.getContent());
                                
                                messageRepository.save(systemMessage);
                                
                                logger.info("[SUCCESS] デバッグ用サボり報告メッセージ送信完了: チーム=" + status.getTeamId() +
                                                  ", ユーザー=" + user.getUsername() +
                                                  ", タスク=" + task.getTaskName() +
                                                  ", 実行時刻=" + LocalDateTime.now(clock));
                                processedCount++;
                            } catch (Exception messageException) {
                                logger.error("[ERROR] デバッグ用サボり報告メッセージの送信に失敗: チーム=" + status.getTeamId() +
                                                  ", ユーザー=" + user.getUsername() +
                                                  ", タスク=" + task.getTaskName() +
                                                  ", エラー=" + messageException.getMessage());
                                messageException.printStackTrace();
                            }
                        } else {
                            logger.error("[ERROR] ユーザーが見つかりません: userId=" + status.getUserId());
                        }
                    } else {
                        logger.info("[DEBUG] タスク完了済みのためスキップ: userId=" + status.getUserId() + ", taskName=" + task.getTaskName());
                    }
                }
            } else {
                logger.info("[DEBUG] " + dateToCheck + " の日付でUserTaskStatusが見つからないため、タスク「" + task.getTaskName() + "」をスキップ");
            }
        }
        return processedCount;
    }

    /**
     * デバッグ用：全チームの「今日まで」の未消化タスクでサボり報告を送信
     */
    public void runDebugSabotageReportForToday() {
        // 重複実行防止（前回の処理がまだ終わっていない場合はスキップ）
        if (isRunning) {
            logger.info("自動再設定処理が実行中のため、デバッグ処理をスキップします");
            return;
        }
        
        isRunning = true;
        try {
            LocalDate today = LocalDate.now(clock);
            
            // TeamRepositoryから全チームIDを取得
            com.habit.server.repository.TeamRepository teamRepository =
                new com.habit.server.repository.TeamRepository();
            List<String> allTeamIds = teamRepository.findAllTeamIds();
            
            logger.info("デバッグ用サボり報告チェック開始: " + allTeamIds.size() + "チーム対象 at " +
                java.time.LocalDateTime.now(clock));
            
            int processedTeams = 0;
            int totalReports = 0;
            
            // 各チームを順次処理
            for (String teamId : allTeamIds) {
                try {
                    int reports = checkAndReportTasksForToday(teamId, today);
                    totalReports += reports;
                    processedTeams++;
                } catch (Exception e) {
                    logger.error("チーム " + teamId + " のデバッグ用サボり報告でエラー: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            logger.info("デバッグ用サボり報告チェック完了: " + processedTeams + "チームで処理, " +
                totalReports + "件のサボり報告を送信 at " + java.time.LocalDateTime.now(clock));
            
        } catch (Exception e) {
            logger.error("デバッグ用サボり報告の実行でエラー: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 必ず実行フラグをリセット
            isRunning = false;
        }
    }

    private void saveLastExecutionTime(LocalDate date) {
        try {
            Files.writeString(LAST_EXECUTION_FILE, date.format(DATE_FORMATTER));
        } catch (IOException e) {
            logger.error("最終実行日時の保存に失敗しました: " + e.getMessage());
        }
    }

    private LocalDate loadLastExecutionTime() {
        if (!Files.exists(LAST_EXECUTION_FILE)) {
            return null;
        }
        try {
            String content = Files.readString(LAST_EXECUTION_FILE);
            return LocalDate.parse(content, DATE_FORMATTER);
        } catch (IOException e) {
            logger.error("最終実行日時の読み込みに失敗しました: " + e.getMessage());
            return null;
        }
    }
}