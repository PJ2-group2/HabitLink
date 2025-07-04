package com.habit.server.service;

import com.habit.domain.Task;
import com.habit.domain.UserTaskStatus;
import com.habit.server.repository.TaskRepository;
import com.habit.server.repository.UserTaskStatusRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

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
    
    // デフォルトの期限時刻（0時固定にしたい場合は、getDueTimeメソッドでこの値を常に返すよう変更）
    private static final LocalTime DEFAULT_DUE_TIME = LocalTime.MIDNIGHT;
    
    // 重複実行防止用のフラグ（1分ごと実行のため、処理が重複しないよう制御）
    private volatile boolean isRunning = false;
    
    /**
     * コンストラクタ
     */ 
    public TaskAutoResetService(TaskRepository taskRepository, UserTaskStatusRepository userTaskStatusRepository) {
        this.taskRepository = taskRepository;
        this.userTaskStatusRepository = userTaskStatusRepository;
    }
    
    /**
     * 指定チームの全タスクを自動再設定チェック
     *
     * @param teamId 対象チームID
     *
     * 【処理フロー】
     * 1. 指定チームの全タスクを取得
     * 3. 対象タスクの全ユーザー分をチェック・再設定
     */
    public void checkAndResetTasks(String teamId) {
        List<Task> teamTasks = taskRepository.findTeamTasksByTeamID(teamId);
        LocalDate today = LocalDate.now();
        
        for (Task task : teamTasks) {
            resetTaskForAllUsers(task, today);
        }
    }
    
    /**
     * 特定タスクを全ユーザーに対して再設定
     */
    private void resetTaskForAllUsers(Task task, LocalDate today) {
        List<UserTaskStatus> allStatuses = userTaskStatusRepository.findByTaskId(task.getTaskId());
        
        for (UserTaskStatus status : allStatuses) {
            LocalDate taskDate = status.getDate();
            
            // 1. 達成済みチェック(期限切れ時は達成させない前提)
            if (status.isDone()) {
                createNextTaskInstance(task, status.getUserId(), getNextDate(task, taskDate));
            }
            
            // 2. 期限切れチェック
            else if (!status.isDone() && isOverdue(task, taskDate, today)) {
                // ★修正★ 期限切れ時もタスク達成時と同じく次のサイクル日に設定
                LocalDate nextDate = getNextDate(task, taskDate);
                createNextTaskInstanceForOverdue(task, status.getUserId(), nextDate);
            }
        }
    }
    
    /**
     * 期限切れかどうかを判定するメソッド
     */
    private boolean isOverdue(Task task, LocalDate taskDate, LocalDate today) {
        LocalTime dueTime = getDueTime(task);
        LocalDateTime deadline = taskDate.atTime(dueTime); // 時刻情報を追加
        LocalDateTime now = LocalDateTime.now();
        
        return now.isAfter(deadline);
    }
    
    /**
     * タスクの期限時刻を取得（0時固定オプション対応）
     *
     * @param task 対象タスク
     * @return 期限時刻
     *
     * 【期限時刻の決定ロジック】
     * - タスクに個別の期限時刻が設定されている場合: その時刻を使用
     * - 設定されていない場合: デフォルト0時を使用
     *
     * 【0時固定にしたい場合の変更方法】
     * 下記のコメントアウト部分を有効にして、return文を置き換える：
     * return DEFAULT_DUE_TIME;
     */
    private LocalTime getDueTime(Task task) {
        LocalTime taskDueTime = task.getDueTime();
        
        // 実装を楽にしたい場合：常に0時を返す（期限時刻を0時固定）
        // return DEFAULT_DUE_TIME;
        
        // 柔軟性を保つ場合：設定時刻 or デフォルト0時
        return taskDueTime != null ? taskDueTime : DEFAULT_DUE_TIME;
    }
    
    /**
     * 次回タスク日を計算
     */
    private LocalDate getNextDate(Task task, LocalDate currentDate) {
        String cycleType = task.getCycleType();
        
        switch (cycleType) {
            case "daily":
                return currentDate.plusDays(1);
            case "weekly":
                return currentDate.plusWeeks(1);
            default:
                return currentDate.plusDays(1); // デフォルトは翌日
        }
    }
    
    /**
     * 新しいタスクインスタンスを作成
     *
     * @param originalTask 元のタスク（テンプレートとして使用）
     * @param userId 対象ユーザーID
     * @param nextDate 次回タスク日
     * @return true: 新規作成された, false: 既存のため作成されず
     *
     * 【重要な処理】
     * 1. 新しいTaskIDを生成
     * 2. 新しいTaskをデータベースに保存
     * 3. 新しいUserTaskStatusを作成・保存
     * 4. ログ出力
     */
    private boolean createNextTaskInstance(Task completeTask, String userId, LocalDate nextDate) {
        // オリジナルのタスクIDを取得（最新のプロパティを使用するため）
        String originalTaskId = completeTask.getOriginalTaskId() != null ?
            completeTask.getOriginalTaskId() : completeTask.getTaskId();

        // 最新の元のタスクを検索
        Task originalTask = findOriginalTaskById(originalTaskId);
        if (originalTask == null) {
            System.err.println("警告: オリジナルのタスクが見つかりません。今のタスクをオリジナルとします。: " + originalTaskId);
            originalTask = completeTask; // フォールバック
        }

        // 元のタスクの期限時刻をそのまま使用
        LocalTime nextDueTime = originalTask.getDueTime();

        // 期限日を付加した新しいTaskIDを生成
        String newTaskId = generateNewTaskId(originalTaskId, nextDate);

        // 新しいTaskIDでの重複チェック
        var existingStatus = userTaskStatusRepository
            .findByUserIdAndOriginalTaskIdAndDate(userId, newTaskId, nextDate);
            
        if (existingStatus.isEmpty()) {

            // 1. 新しいTaskを作成・保存
            Task newTask = new Task(
                    newTaskId, // 新しいTaskID
                    originalTask.getTaskName(), // 同じタスク名
                    originalTask.getDescription(), // 同じ説明
                    originalTask.getEstimatedMinutes(), // 同じ推定時間
                    originalTask.getRepeatDays(), // 同じ繰り返し曜日
                    originalTask.isTeamTask(), // 同じチーム設定
                    nextDueTime, // 調整された期限時刻
                    nextDate, // 調整された期限日付
                    originalTask.getCycleType() // 同じサイクルタイプ
            );

            // TaskをDBに保存
            String teamId = findTeamIdByOriginalTask(originalTaskId);
            newTask.setTeamId(teamId); // チーム共通タスクのためteamIdを設定
            taskRepository.saveTask(newTask, teamId);
            
            // 2. 特定ユーザーのみに対してUserTaskStatusを作成
            // 既存のUserTaskStatusがないことを確認（taskIdでのみチェック、originalTaskIdでの重複チェックは行わない）
            boolean existsByTaskId = userTaskStatusRepository.findByUserIdAndTaskIdAndDate(
                userId, newTaskId, nextDate).isPresent();
                
            if (!existsByTaskId) {
                UserTaskStatus newStatus = new UserTaskStatus(
                    userId,     // 対象ユーザー
                    newTaskId,  // 新しいTaskID
                    teamId,     // チームID
                    nextDate,   // 次回実行日
                    false       // 初期状態は未完了
                );
                
                // 元のTaskIDを明示的に設定
                newStatus.setOriginalTaskId(originalTaskId);
                
                // データベースにUserTaskStatusを保存・永続化
                userTaskStatusRepository.save(newStatus);
            }
            
            if (!existsByTaskId) {
                System.out.println("自動再設定完了: userId=" + userId +
                        ", newTaskId=" + newTaskId +
                        ", TaskName=" + originalTask.getTaskName() +
                        ", originalTaskId=" + originalTaskId +
                        ", adjustedDueDate=" + nextDate +
                        ", adjustedDueTime=" + nextDueTime +
                        ", teamId=" + teamId);
            } else {
                System.out.println("自動再設定スキップ（既存あり）: userId=" + userId +
                    ", originalTaskId=" + completeTask.getTaskId() +
                    ", nextDate=" + nextDate);
            }
                
            // 作成されたタスクが正しく保存されているかを確認
            var savedTask = taskRepository.findTeamTasksByTeamID(teamId).stream()
                .filter(t -> t.getTaskId().equals(newTaskId))
                .findFirst();
            if (savedTask.isPresent()) {
                System.out.println("新しいタスクがDBに正常に保存されました: " + savedTask.get().getTaskName());
            } else {
                System.err.println("警告: 新しいタスクがDBに保存されていません: " + newTaskId);
            }
            return true;  // 新規作成成功
        }
        return false;     // 既存のため作成せず
    }
    
    /**
     * 期限切れタスクの新しいインスタンスを作成（期限時刻を調整）
     *
     * @param originalTask 元のタスク（テンプレートとして使用）
     * @param userId 対象ユーザーID
     * @param nextDate 次回タスク日
     * @return true: 新規作成された, false: 既存のため作成されず
     *
     * 元の期限時刻と等しく、元の期限日+1cycleのタスクを作成
     */
    private boolean createNextTaskInstanceForOverdue(Task overdueTask, String userId, LocalDate nextDate) {
        // オリジナルのタスクIDを取得（最新のプロパティを使用するため）
        String originalTaskId = overdueTask.getOriginalTaskId() != null ?
            overdueTask.getOriginalTaskId() : overdueTask.getTaskId();
            
        // 最新の元のタスクを検索
        Task originalTask = findOriginalTaskById(originalTaskId);
        if (originalTask == null) {
            System.err.println("警告: オリジナルのタスクが見つかりません。今のタスクをオリジナルとします。: " + originalTaskId);
            originalTask = overdueTask; // フォールバック
        }
        
        // 元のタスクの期限時刻をそのまま使用
        LocalTime nextDueTime = originalTask.getDueTime();

        // 期限日を付加した新しいTaskIDを生成
        String newTaskId = generateNewTaskId(originalTaskId, nextDate);

        var existingStatusByNewTaskId = userTaskStatusRepository
            .findByUserIdAndTaskIdAndDate(userId, newTaskId, nextDate); // 新しいTaskIDでの重複チェック
            
        if (existingStatusByNewTaskId.isEmpty()) {
            // 1. 新しいTaskを作成・保存
            Task newTask = new Task(
                newTaskId,                          // 新しいTaskID
                originalTask.getTaskName(),         // 同じタスク名
                originalTask.getDescription(),      // 同じ説明
                originalTask.getEstimatedMinutes(), // 同じ推定時間
                originalTask.getRepeatDays(),       // 同じ繰り返し曜日
                originalTask.isTeamTask(),          // 同じチーム設定
                nextDueTime,                    // 調整された期限時刻
                nextDate,                    // 調整された期限日付
                originalTask.getCycleType()         // 同じサイクルタイプ
            );
            
            // TaskをDBに保存
            String teamId = findTeamIdByOriginalTask(originalTaskId);
            newTask.setTeamId(teamId); // チーム共通タスクのためteamIdを設定
            taskRepository.saveTask(newTask, teamId);
            
            // 2. 特定ユーザーのみに対してUserTaskStatusを作成
            // 既存のUserTaskStatusがないことを確認（taskIdでのみチェック）
            boolean existsByTaskIdOverdue = userTaskStatusRepository.findByUserIdAndTaskIdAndDate(
                userId, newTaskId, nextDate).isPresent();
                
            if (!existsByTaskIdOverdue) {
                UserTaskStatus newStatus = new UserTaskStatus(
                    userId,         // 対象ユーザー
                    newTaskId,      // 新しいTaskID
                    teamId,         // チームID
                    nextDate,       // 調整された期限日付
                    false    // 初期状態は未完了
                );
                
                // 元のTaskIDを明示的に設定
                newStatus.setOriginalTaskId(originalTaskId);
                 
                // データベースにUserTaskStatusを保存・永続化
                userTaskStatusRepository.save(newStatus);
            }
            
            if (!existsByTaskIdOverdue) {
                System.out.println("期限切れタスクの再設定完了: userId=" + userId +
                    ", newTaskId=" + newTaskId +
                    ", TaskName=" + originalTask.getTaskName() +
                    ", originalTaskId=" + originalTaskId+
                    ", adjustedDueDate=" + nextDate +
                    ", adjustedDueTime=" + nextDueTime +
                    ", teamId=" + teamId);
            } else {
                System.out.println("期限切れタスクの再設定スキップ（既存あり）: userId=" + userId +
                    ", originalTaskId=" + originalTaskId +
                    ", adjustedDueDate=" + nextDate);
            }
                
            // 作成されたタスクが正しく保存されているかを確認
            var savedTask = taskRepository.findTeamTasksByTeamID(teamId).stream()
                .filter(t -> t.getTaskId().equals(newTaskId))
                .findFirst();
            if (savedTask.isPresent()) {
                System.out.println("新しいタスクがDBに正常に保存されました: " + savedTask.get().getTaskName());
            } else {
                System.err.println("警告: 新しいタスクがDBに保存されていません: " + newTaskId);
            }
            
            // 作成されたUserTaskStatusが正しく保存されているかを確認
            var savedStatus = userTaskStatusRepository.findByUserIdAndTaskIdAndDate(
                userId, newTaskId, nextDate);
            if (savedStatus.isPresent()) {
                System.out.println("新しいUserTaskStatusがDBに正常に保存されました: " + savedStatus.get().getTaskId());
            } else {
                System.err.println("警告: 新しいUserTaskStatusがDBに保存されていません: " + newTaskId);
            }
            
            return true;  // 新規作成成功
        } else {
            System.out.println("警告: 新タスクIDが既存IDと重複しています。 作成をスキップします。");
        }
        return false;     // 既存のため作成せず
    }
    
    /**
     * 期限切れ時の期限時刻を調整
     *
     * @param originalDueTime 元の期限時刻
     * @param targetDate 対象日付
     * @return 調整された期限時刻
     */
    private LocalTime adjustDueTimeForOverdue(LocalTime originalDueTime, LocalDate targetDate) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        // 元の期限時刻が設定されていない場合はデフォルト
        if (originalDueTime == null) {
            return LocalTime.of(23, 59);
        }
        
        // 翌日以降の場合は元の時刻をそのまま使用（調整不要）
        if (targetDate.isAfter(today)) {
            return originalDueTime;
        }
        
        // 当日の場合のみ時刻調整
        if (targetDate.equals(today)) {
            // 現在時刻が元の期限時刻を過ぎている場合
            if (now.isAfter(originalDueTime)) {
                // 2時間後に設定（翌日への調整は呼び出し元で判断）
                LocalTime twoHoursLater = now.plusHours(2);
                LocalTime endOfDay = LocalTime.of(23, 59);
                
                if (twoHoursLater.isAfter(endOfDay)) {
                    // 当日内で調整不可能な場合はnullを返す
                    return null;
                } else {
                    return twoHoursLater;
                }
            } else {
                // まだ期限時刻前の場合はそのまま使用
                return originalDueTime;
            }
        }
        
        // その他の場合は元の時刻をそのまま使用
        return originalDueTime;
    }
    
    /**
     * 新しいTaskIDを生成
     *
     * @param originalTaskId 元のTaskID
     * @param date 対象日付
     * @return 新しいTaskID
     *
     * 【命名規則の変更】
     * 元のTaskID + "_" + 日付(YYYYMMDD)
     * 例: "dailyTask_20250630"
     *
     * 【変更理由】
     * - タイムスタンプを削除して、同じ日の同じタスクは同じIDになるよう修正
     * - これにより元のTaskIDとの関連性が明確になり、重複防止も確実になる
     */
    private String generateNewTaskId(String originalTaskId, LocalDate date) {
        String dateStr = date.toString().replace("-", ""); // YYYYMMDD形式
        return originalTaskId + "_" + dateStr;
    }
    
    /**
     * 元のTaskIDから最新のタスクを取得
     *
     * @param originalTaskId 元のTaskID
     * @return 最新のタスク（見つからない場合はnull）
     */
    private Task findOriginalTaskById(String originalTaskId) {
        try {
            com.habit.server.repository.TeamRepository teamRepo =
                new com.habit.server.repository.TeamRepository();
            java.util.List<String> allTeamIds = teamRepo.findAllTeamIds();
            
            for (String teamId : allTeamIds) {
                java.util.List<Task> teamTasks = taskRepository.findTeamTasksByTeamID(teamId);
                for (Task task : teamTasks) {
                    if (task.getTaskId().equals(originalTaskId)) {
                        return task;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("元のタスク取得エラー: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 元のTaskIDからチームIDを取得
     *
     * @param originalTaskId 元のTaskID
     * @return チームID
     */
    private String findTeamIdByOriginalTask(String originalTaskId) {
        // TaskRepositoryから元のタスクを取得してチームIDを特定
        // ここでは簡易実装として、既存のチームタスク一覧から検索
        try {
            com.habit.server.repository.TeamRepository teamRepo =
                new com.habit.server.repository.TeamRepository();
            java.util.List<String> allTeamIds = teamRepo.findAllTeamIds();
            
            for (String teamId : allTeamIds) {
                java.util.List<Task> teamTasks = taskRepository.findTeamTasksByTeamID(teamId);
                for (Task task : teamTasks) {
                    if (task.getTaskId().equals(originalTaskId)) {
                        return teamId;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("チームID取得エラー: " + e.getMessage());
        }
        
        // 見つからない場合のデフォルト（実際にはエラーハンドリングを強化）
        return "unknown_team";
    }
    
    /**
     * 定期実行用：全チームのタスクをチェック
     *  TaskAutoResetSchedulerにより1分ごとに自動実行
     *  TaskAutoResetControllerのAPIからも呼び出し可能
     *
     * 【処理フロー】
     * 1. 重複実行防止チェック
     * 2. 全チームIDを取得
     * 3. 各チームのタスクを順次チェック・再設定
     * 4. 実行結果をログ出力（処理チーム数、再設定タスク数）
     */
    public void runScheduledCheck() {
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
                java.time.LocalDateTime.now());
            
            int processedTeams = 0;
            int totalResets = 0;
            
            // 各チームを順次処理
            for (String teamId : allTeamIds) {
                try {
                    int resets = checkAndResetTasksWithCount(teamId);
                    totalResets += resets;
                    processedTeams++;
                } catch (Exception e) {
                    System.err.println("チーム " + teamId + " の自動再設定でエラー: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("自動再設定チェック完了: " + processedTeams + "チームで処理, " +
                totalResets + "タスクを再設定 at " + java.time.LocalDateTime.now());
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
    private int checkAndResetTasksWithCount(String teamId) {
        List<Task> teamTasks = taskRepository.findTeamTasksByTeamID(teamId); // チームの全タスクを取得
        LocalDate today = LocalDate.now();
        int resetCount = 0;
        
        for (Task task : teamTasks) {
            // 各タスクに対して再設定チェックを実施
            resetCount += resetTaskForAllUsersWithCount(task, today);
        }
        return resetCount;
    }
    
    /**
     * 特定タスクを全ユーザーに対して再設定
     * @param task 対象タスク
     * @param today 今日の日付
     * @return 再設定されたタスクの数
     * 
     * 【処理フロー】
     * 1. 指定タスクの全ユーザー分を取得
     * 2. 各ユーザーのタスクステータスをチェック
     * 3. 再設定が必要な場合は新しいタスクを作成
     */
    private int resetTaskForAllUsersWithCount(Task task, LocalDate today) {
        // タスクの全ユーザー分のステータスを取得
        List<UserTaskStatus> allStatuses = userTaskStatusRepository.findByTaskId(task.getTaskId());
        int resetCount = 0; // 再設定されたタスクの数

        for (UserTaskStatus status : allStatuses) {
            LocalDate taskDate = status.getDate();

            // 1. 達成済みチェック(期限切れ後は達成させない前提)
            if (status.isDone()) {
                // 新インスタンスを作成して再設定
                if (createNextTaskInstance(task, status.getUserId(), getNextDate(task, taskDate))) {
                    resetCount++;
                }
            }

            // 2. 期限切れチェック（未完了かつ期限切れ）
            else if (!status.isDone() && isOverdue(task, taskDate, today)) {
                System.out.println("期限切れタスク検出: taskId=" + task.getTaskId() +
                    ", taskName=" + task.getTaskName() +
                    ", userId=" + status.getUserId() +
                    ", taskDate=" + taskDate +
                    ", today=" + today);
                // 次のサイクル日に設定
                LocalDate nextDate = getNextDate(task, taskDate);
                // 新インスタンスを作成して再設定
                if (createNextTaskInstanceForOverdue(task, status.getUserId(), nextDate)) {
                    resetCount++;
                }
            }
        }
        return resetCount;
    }
    /**
     * 特定のタスク完了時に即座に次のタスクを再設定する（外部呼び出し用）
     *
     * @param completedTask 完了したタスク
     * @param userId 対象ユーザーID
     * @param completionDate 完了日
     * @param teamId チームID
     * @return true: 再設定実行, false: 対象外またはスキップ
     */
    public boolean createNextTaskInstanceImmediately(Task completedTask, String userId, LocalDate completionDate, String teamId) {
        try {
            // 完了したUserTaskStatusを取得して期限内達成かチェック
            Optional<UserTaskStatus> optStatus = userTaskStatusRepository
                .findByUserIdAndTaskIdAndDate(userId, completedTask.getTaskId(), completionDate);
            
            if (optStatus.isPresent()) {
                UserTaskStatus status = optStatus.get();
                LocalDate taskDate = status.getDate(); 

                // 達成済みの場合のみ即座に再設定(期限切れタスクは達成させない前提)
                if (status.isDone()) {
                    // 次のサイクルに正しく設定
                    LocalDate nextDate = getNextDate(completedTask, taskDate);
                    boolean created = createNextTaskInstance(completedTask, userId, nextDate);
                    
                    if (created) {
                        System.out.println("即座のタスク再設定成功: originalTaskId=" + completedTask.getOriginalTaskId() +
                            ", userId=" + userId + ", nextDate=" + nextDate);
                        return true;
                    } else {
                        System.out.println("即座のタスク再設定スキップ（既存あり）: originalTaskId=" + completedTask.getOriginalTaskId() +
                            ", userId=" + userId + ", nextDate=" + nextDate);
                        return false;
                    }
                } else {
                    System.out.println("即座の再設定スキップ（期限外達成）: taskId=" + completedTask.getTaskId() +
                        ", userId=" + userId);
                    return false;
                }
            } else {
                System.out.println("即座の再設定スキップ（ステータス未取得）: taskId=" + completedTask.getTaskId() +
                    ", userId=" + userId);
                return false;
            }
        } catch (Exception e) {
            System.err.println("即座のタスク再設定でエラー: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}