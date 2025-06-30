package com.habit.server.service;

import com.habit.domain.Task;
import com.habit.domain.UserTaskStatus;
import com.habit.server.repository.TaskRepository;
import com.habit.server.repository.UserTaskStatusRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * タスクの自動再設定サービス
 *
 * 【機能概要】
 * 1. 期限内達成時の自動再設定: タスクが期限内に完了した場合、次回サイクルのタスクを自動生成
 * 2. 期限切れ時の自動再設定: タスクが期限を過ぎても未完了の場合、当日分の新しいタスクを自動生成
 *
 * 【実行タイミング】
 * - 1時間ごとに自動実行（TaskAutoResetSchedulerによる）
 * - 手動実行も可能（TaskAutoResetControllerのAPI経由）
 *
 * 【対象タスク】
 * - cycleType が "daily" または "weekly" のタスクのみ
 * - 既に同じ日付のタスクが存在する場合は重複作成しない
 */
public class TaskAutoResetService {
    private final TaskRepository taskRepository;
    private final UserTaskStatusRepository userTaskStatusRepository;
    
    // デフォルトの期限時刻（0時固定にしたい場合は、getDueTimeメソッドでこの値を常に返すよう変更）
    private static final LocalTime DEFAULT_DUE_TIME = LocalTime.MIDNIGHT;
    
    // 重複実行防止用のフラグ（1時間ごと実行のため、処理が重複しないよう制御）
    private volatile boolean isRunning = false;
    
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
     * 2. 各タスクが自動再設定対象かチェック（daily/weeklyのみ）
     * 3. 対象タスクの全ユーザー分をチェック・再設定
     */
    public void checkAndResetTasks(String teamId) {
        List<Task> teamTasks = taskRepository.findTeamTasksByTeamID(teamId);
        LocalDate today = LocalDate.now();
        
        for (Task task : teamTasks) {
            if (shouldAutoReset(task)) {
                resetTaskForAllUsers(task, today);
            }
        }
    }
    
    /**
     * タスクが自動再設定対象かどうかを判定
     *
     * @param task 判定対象のタスク
     * @return true: 自動再設定対象, false: 対象外
     *
     * 【判定条件】
     * - cycleType が "daily" (毎日繰り返し) の場合: 対象
     * - cycleType が "weekly" (毎週繰り返し) の場合: 対象
     * - それ以外: 対象外（一回限りのタスクなど）
     */
    private boolean shouldAutoReset(Task task) {
        String cycleType = task.getCycleType();
        return "daily".equals(cycleType) || "weekly".equals(cycleType);
    }
    
    /**
     * 特定タスクを全ユーザーに対して再設定
     */
    private void resetTaskForAllUsers(Task task, LocalDate today) {
        List<UserTaskStatus> allStatuses = userTaskStatusRepository.findByTaskId(task.getTaskId());
        
        for (UserTaskStatus status : allStatuses) {
            LocalDate taskDate = status.getDate();
            
            // 1. 期限内達成チェック
            if (status.isDone() && isWithinDeadline(task, status)) {
                createNextTaskInstance(task, status.getUserId(), getNextDate(task, taskDate));
            }
            
            // 2. 期限切れチェック
            else if (!status.isDone() && isOverdue(task, taskDate, today)) {
                createNextTaskInstance(task, status.getUserId(), today);
            }
        }
    }
    
    /**
     * 期限内達成かどうかを判定
     */
    private boolean isWithinDeadline(Task task, UserTaskStatus status) {
        LocalDate taskDate = status.getDate();
        LocalTime dueTime = getDueTime(task);
        LocalDateTime deadline = taskDate.atTime(dueTime);
        
        return status.getCompletionTimestamp() != null && 
               status.getCompletionTimestamp().isBefore(deadline);
    }
    
    /**
     * 期限切れかどうかを判定
     */
    private boolean isOverdue(Task task, LocalDate taskDate, LocalDate today) {
        LocalTime dueTime = getDueTime(task);
        LocalDateTime deadline = taskDate.atTime(dueTime);
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
    private boolean createNextTaskInstance(Task originalTask, String userId, LocalDate nextDate) {
        // 元のTaskIDを使用して重複チェック（同じ元タスクの同日インスタンスがないかチェック）
        var existingStatus = userTaskStatusRepository
            .findByUserIdAndOriginalTaskIdAndDate(userId, originalTask.getTaskId(), nextDate);
            
        if (existingStatus.isEmpty()) {
            // 新しいTaskIDを生成（重複がない場合のみ）
            String newTaskId = generateNewTaskId(originalTask.getTaskId(), nextDate);
            // 1. 新しいTaskを作成・保存
            Task newTask = new Task(
                newTaskId,                          // 新しいTaskID
                originalTask.getTaskName(),         // 同じタスク名
                originalTask.getDescription(),      // 同じ説明
                originalTask.getEstimatedMinutes(), // 同じ推定時間
                originalTask.getRepeatDays(),       // 同じ繰り返し曜日
                originalTask.isTeamTask(),          // 同じチーム設定
                originalTask.getDueTime(),          // 同じ期限時刻
                originalTask.getCycleType()         // 同じサイクルタイプ
            );
            
            // TaskをDBに保存（teamIDも必要）
            String teamId = findTeamIdByOriginalTask(originalTask.getTaskId());
            taskRepository.saveTask(newTask, teamId);
            
            // 2. 新しいUserTaskStatusインスタンスを作成
            UserTaskStatus newStatus = new UserTaskStatus(
                userId,     // 対象ユーザー
                newTaskId,  // 新しいTaskID
                nextDate,   // 次回実行日
                false       // 初期状態は未完了
            );
            
            // 元のTaskIDを明示的に設定
            newStatus.setOriginalTaskId(originalTask.getTaskId());
            
            // ★重要★ データベースにUserTaskStatusを保存・永続化
            userTaskStatusRepository.save(newStatus);
            
            System.out.println("自動再設定完了: userId=" + userId +
                ", newTaskId=" + newTaskId +
                ", originalTaskId=" + originalTask.getTaskId() +
                ", originalTaskIdFromStatus=" + newStatus.getOriginalTaskId() +
                ", teamId=" + teamId +
                ", date=" + nextDate);
                
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
     *
     * 【実行頻度】TaskAutoResetSchedulerにより1時間ごとに自動実行
     * 【手動実行】TaskAutoResetControllerのAPIからも呼び出し可能
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
            
            System.out.println("自動再設定チェック完了: " + processedTeams + "チーム処理, " +
                totalResets + "タスク再設定 at " + java.time.LocalDateTime.now());
        } catch (Exception e) {
            System.err.println("自動再設定の定期実行でエラー: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 必ず実行フラグをリセット
            isRunning = false;
        }
    }
    
    /**
     * 再設定数をカウントするバージョン
     */
    private int checkAndResetTasksWithCount(String teamId) {
        List<Task> teamTasks = taskRepository.findTeamTasksByTeamID(teamId);
        LocalDate today = LocalDate.now();
        int resetCount = 0;
        
        for (Task task : teamTasks) {
            if (shouldAutoReset(task)) {
                resetCount += resetTaskForAllUsersWithCount(task, today);
            }
        }
        return resetCount;
    }
    
    /**
     * 再設定数をカウントするバージョン
     */
    private int resetTaskForAllUsersWithCount(Task task, LocalDate today) {
        List<UserTaskStatus> allStatuses = userTaskStatusRepository.findByTaskId(task.getTaskId());
        int resetCount = 0;
        
        for (UserTaskStatus status : allStatuses) {
            LocalDate taskDate = status.getDate();
            
            // 1. 期限内達成チェック
            if (status.isDone() && isWithinDeadline(task, status)) {
                if (createNextTaskInstance(task, status.getUserId(), getNextDate(task, taskDate))) {
                    resetCount++;
                }
            }
            
            // 2. 期限切れチェック
            else if (!status.isDone() && isOverdue(task, taskDate, today)) {
                if (createNextTaskInstance(task, status.getUserId(), today)) {
                    resetCount++;
                }
            }
        }
        return resetCount;
    }
}