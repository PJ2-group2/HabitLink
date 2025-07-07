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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * タスクの自動再設定サービス
 *  タスクが期限を過ぎても未完了の場合、次回サイクルのタスクを自動生成
 *  1分ごとに自動実行（TaskAutoResetSchedulerによる）
 *  手動実行も可能（TaskAutoResetControllerのAPI経由）
 *  既に同じ日付のタスクが存在する場合は重複作成しない
 */
public class TaskAutoResetService {
    private final TaskRepository taskRepository;
    private final UserTaskStatusRepository userTaskStatusRepository;
    private final UserRepository userRepository; // UserRepositoryを追加
    private final MessageRepository messageRepository; // MessageRepositoryを追加
    private final Clock clock;
    private static final Path LAST_EXECUTION_FILE = Paths.get("last_execution.log");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    // 重複実行防止用のフラグ（1分ごと実行のため、処理が重複しないよう制御）
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
            System.out.println("サーバー停止期間中の未処理のタスク更新を開始します: " + missedDates.size() + "日分");
            for (LocalDate date : missedDates) {
                System.out.println(date + " のタスクを更新します。");
                runScheduledCheckForDate(date);
            }
            System.out.println("未処理のタスク更新が完了しました。");
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
            System.out.println("自動再設定処理が実行中のため、今回はスキップします");
            return;
        }
        
        isRunning = true;
        try {
            // TeamRepositoryから全チームIDを取得
            com.habit.server.repository.TeamRepository teamRepository =
                new com.habit.server.repository.TeamRepository();
            List<String> allTeamIds = teamRepository.findAllTeamIds();
            
            System.out.println("自動再設定チェック開始: " + allTeamIds.size() + "チーム対象 at " +
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
                    System.err.println("チーム " + teamId + " の自動再設定でエラー: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("自動再設定チェック完了: " + processedTeams + "チームで処理, " +
                totalResets + "タスクを再設定 at " + java.time.LocalDateTime.now(clock));
            
            saveLastExecutionTime(date);
        } catch (Exception e) {
            System.err.println("自動再設定の定期実行でエラー: " + e.getMessage());
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

        for (Task task : teamTasks) {
            String cycleType = task.getCycleType();
            if (cycleType == null) {
                continue; // 繰り返し設定のないタスクはスキップ
            }

            LocalDate dateToCheck = executionDate.minusDays(1); // チェック対象は実行日の前日
            LocalDate newDueDate = null;

            switch (cycleType) {
                case "DAILY":
                    newDueDate = executionDate;
                    break;
                case "WEEKLY":
                    newDueDate = dateToCheck.plusWeeks(1); // 基準日（昨日）から1週間後
                    break;
                default:
                    continue; // 未対応のサイクルタイプはスキップ
            }

            // Task自体のdueDateを更新して保存
            task.setDueDate(newDueDate);
            taskRepository.save(task);

            // 昨日の日付でUserTaskStatusを検索
            List<UserTaskStatus> statusesToCheck = userTaskStatusRepository.findByTaskIdAndDate(task.getTaskId(), dateToCheck);

            if (!statusesToCheck.isEmpty()) {
                for (UserTaskStatus oldStatus : statusesToCheck) {
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
                        } else {
                            // 未完了ならポイントを増やす（9を超えない）
                            newPoints = Math.min(9, currentPoints + 1);
                            changeAmount = newPoints - currentPoints;

                            // サボり報告メッセージを送信
                            String reportMessage = user.getUsername() + "さんが昨日のタスク「" + task.getTaskName() + "」をサボりました。";
                            Message systemMessage = new Message(SERVER_USER, oldStatus.getTeamId(), reportMessage, LocalDateTime.now(clock));
                            messageRepository.save(systemMessage);
                            System.out.println("[" + oldStatus.getTeamId() + "]へサボり報告メッセージを送信しました: " + reportMessage);
                        }
                        user.setSabotagePoints(newPoints);
                        userRepository.save(user);
                        System.out.println(user.getUsername() + " のサボりポイントを " + currentPoints + " Ptから " + newPoints + " Ptに変更 (変動量: " + (changeAmount > 0 ? "+" : "") + changeAmount + ")");
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
                        System.out.println("新しいUserTaskStatusを生成: userId=" + newStatus.getUserId() +
                            ", taskId=" + newStatus.getTaskId() +
                            ", date=" + newStatus.getDate());
                    } else {
                        System.out.println("UserTaskStatusは既に存在します。スキップ: userId=" + newStatus.getUserId() +
                            ", taskId=" + newStatus.getTaskId() +
                            ", date=" + newStatus.getDate());
                    }
                }
            }
        }
        return resetCount;
    }

    private void saveLastExecutionTime(LocalDate date) {
        try {
            Files.writeString(LAST_EXECUTION_FILE, date.format(DATE_FORMATTER));
        } catch (IOException e) {
            System.err.println("最終実行日時の保存に失敗しました: " + e.getMessage());
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
            System.err.println("最終実行日時の読み込みに失敗しました: " + e.getMessage());
            return null;
        }
    }
}