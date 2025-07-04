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
    
    // 実行間隔（1分ごと）- 変更したい場合はここを修正
    private static final int EXECUTION_INTERVAL_MINUTES = 1;
    
    public TaskAutoResetScheduler() {
        this.taskAutoResetService = new TaskAutoResetService(
            new TaskRepository(),
            new UserTaskStatusRepository()
        );
        this.scheduler = Executors.newScheduledThreadPool(1); // スレッドプールのサイズは1（単一スレッドで実行）
    }
    
    /**
     * スケジューラーの開始メソッド
     *  1分ごとに実行
     *  scheduleAtFixedRate: 前回の実行開始時刻から固定間隔で実行
     * （処理時間に関係なく、1分ごとに確実に実行される）
     */
    public void start() {
        long initialDelay = 60; // 1分後に初回実行（サーバー起動直後の負荷を避ける）
        long period = EXECUTION_INTERVAL_MINUTES * 60; // 1分間隔（単位: 秒）

        scheduler.scheduleAtFixedRate(
            this::executeAutoReset, // 実行するメソッド
            initialDelay,           // 初回実行までの遅延
            period,                 // 実行間隔
            TimeUnit.SECONDS        // 時間単位
        );
        
        System.out.println("タスク自動再設定スケジューラーを開始しました。");
        System.out.println("実行間隔: " + EXECUTION_INTERVAL_MINUTES + "分ごと");
        System.out.println("初回実行まで: " + initialDelay + "秒");
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