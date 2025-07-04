package com.habit.server.scheduler;

import com.habit.server.service.TaskAutoResetService;
import com.habit.server.repository.TaskRepository;
import com.habit.server.repository.UserTaskStatusRepository;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * タスク自動再設定の定期実行スケジューラー
 *  タスクの自動再設定を定期的に実行する。
 *  1分ごとに実行
 * 期限切れタスクの検出が目的
 */
public class TaskAutoResetScheduler {
    private final TaskAutoResetService taskAutoResetService;
    private final ScheduledExecutorService scheduler;
    
    public TaskAutoResetScheduler(TaskAutoResetService taskAutoResetService) {
        this.taskAutoResetService = taskAutoResetService;
        this.scheduler = Executors.newScheduledThreadPool(1); // スレッドプールのサイズは1（単一スレッドで実行）
    }
    
    /**
     * スケジューラーの開始メソッド
     * 毎日午前0時に実行されるように設定
     */
    public void start() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0); // 次の午前0時
        long initialDelay = now.until(nextRun, ChronoUnit.SECONDS); // 初回実行までの遅延（秒単位）
        long period = TimeUnit.DAYS.toSeconds(1); // 24時間

        scheduler.scheduleAtFixedRate(
            this::executeAutoReset, // 実行するメソッド
            initialDelay,           // 初回実行までの遅延(次の午前0時まで)
            period,                 // 実行間隔(24時間ごと)
            TimeUnit.SECONDS        // 時間単位
        );
        
        System.out.println("タスク自動再設定スケジューラーを開始しました。");
        System.out.println("初回実行まで: " + initialDelay + "秒 (次の午前0時)");
        System.out.println("実行間隔: 24時間ごと");
    }
    
    /**
     * スケジューラーの停止メソッド
     * 1. 新しいタスクの受付を停止
     * 2. 60秒間実行中のタスクの完了を待機
     * 3. 完了しない場合は強制終了
     *
     * 【呼び出しタイミング】
     * サーバーシャットダウン時（HabitServer.javaのShutdownHookから）
     */
    public void stop() {
        scheduler.shutdown(); // 新しいタスクの受付を停止
        try {
            // 60秒間実行中のタスクの完了を待機
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow(); // 強制終了
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow(); // 割り込み時も強制終了
        }
        System.out.println("タスク自動再設定スケジューラーを停止しました。");
    }
    
    /**
     * 自動再設定を実行するメソッド
     *  TaskAutoResetService.runScheduledCheck()を呼び出し、全チームのタスクを自動再設定
     *  例外が発生してもスケジューラーは停止せず、次回実行を継続
     */
    private void executeAutoReset() {
        try {
            System.out.println("タスク自動再設定を開始: " + LocalDateTime.now());
            taskAutoResetService.runScheduledCheck(); // メイン処理を実行
            System.out.println("タスク自動再設定を完了: " + LocalDateTime.now());
        } catch (Exception e) {
            // エラーログを出力（スケジューラーは継続）
            System.err.println("タスク自動再設定でエラーが発生: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 手動実行（テスト用）
     *
     * 【用途】
     * - 開発時のテスト
     * - デバッグ時の動作確認
     * - TaskAutoResetControllerのAPIから呼び出し
     *
     * 【注意】
     * スケジューラーの定期実行とは独立して実行される
     */
    public void executeNow() {
        executeAutoReset();
    }
}