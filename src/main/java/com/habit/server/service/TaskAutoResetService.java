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
    
    // 重複実行防止用のフラグ（1分ごと実行のため、処理が重複しないよう制御）
    private volatile boolean isRunning = false;

    // 今日の日付
    private LocalDate today = LocalDate.now();

    /**
     * コンストラクタ
     */ 
    public TaskAutoResetService(TaskRepository taskRepository, UserTaskStatusRepository userTaskStatusRepository) {
        this.taskRepository = taskRepository;
        this.userTaskStatusRepository = userTaskStatusRepository;
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
                    int resets = checkAndResetTasks(teamId); // checkAndResetTasksWithCount から checkAndResetTasks に変更
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
    public int checkAndResetTasks(String teamId) { // private から public に変更
        List<Task> teamTasks = taskRepository.findTeamTasksByTeamID(teamId); // チームの全タスクを取得
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1); // 前日の日付
        int resetCount = 0;
        
        for (Task task : teamTasks) {
            // 1. TaskのdueDateを今日の日付に更新
            task.setDueDate(today);
            taskRepository.saveTask(task, teamId); // Taskを保存（更新）
            
            // 2. 前日のdueDateを持つUserTaskStatusを検索
            List<UserTaskStatus> yesterdayStatuses = userTaskStatusRepository.findByTaskIdAndDate(task.getTaskId(), yesterday);

            if (!yesterdayStatuses.isEmpty()) {
                for (UserTaskStatus status : yesterdayStatuses) {
                // 3. isDoneを判定（この部分は後で実装）
                if(status.isDone()) {
                    // 後で実装(サボりポイントの処理など)
                } else {
                    // 後で実装(サボりポイントの処理など)
                }
                
                // 4. 新しいdueDate（今日の日付）でisDoneがfalseのUserTaskStatusを生成
                UserTaskStatus newStatus = new UserTaskStatus(
                    status.getUserId(),
                    task.getTaskId(), // TaskのtaskIdを使用
                    teamId,
                    today, // 新しいdueDate
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
}