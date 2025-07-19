package com.habit.server.scheduler;

import com.habit.server.service.TaskAutoResetService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * タスク自動再設定の定期実行スケジューラー
 *  タスクの自動再設定を定期的に実行する。
 * 期限切れタスクの検出が目的
 */
public class TaskAutoResetScheduler {
    private static final Logger logger = LoggerFactory.getLogger(TaskAutoResetScheduler.class);
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

        logger.info("=== タスク自動再設定スケジューラー初期化 ===");
        logger.info("現在時刻: " + now);
        logger.info("次回実行予定: " + nextRun);
        logger.info("初回実行まで: " + initialDelay + "秒 (" + (initialDelay / 3600) + "時間" + ((initialDelay % 3600) / 60) + "分)");
        logger.info("実行間隔: 24時間ごと");

        try {
            scheduler.scheduleAtFixedRate(
                this::executeAutoReset, // 実行するメソッド
                initialDelay,           // 初回実行までの遅延(次の午前0時まで)
                period,                 // 実行間隔(24時間ごと)
                TimeUnit.SECONDS        // 時間単位
            );
            
            logger.info("[SUCCESS] タスク自動再設定スケジューラーを正常に開始しました。");
            
        } catch (Exception e) {
            logger.error("[ERROR] タスク自動再設定スケジューラーの開始に失敗しました: " + e.getMessage());
            e.printStackTrace();
            throw e; // 初期化失敗を上位に伝える
        }
        
        logger.info("=== スケジューラー初期化完了 ===");
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
        logger.info("タスク自動再設定スケジューラーを停止しました。");
    }
    
    /**
     * 自動再設定を実行するメソッド
     *  TaskAutoResetService.runScheduledCheck()を呼び出し、全チームのタスクを自動再設定
     *  例外が発生してもスケジューラーは停止せず、次回実行を継続
     */
    private void executeAutoReset() {
        LocalDateTime startTime = LocalDateTime.now();
        logger.info("=== タスク自動再設定を開始 ===");
        logger.info("開始時刻: " + startTime);
        logger.info("スレッド: " + Thread.currentThread().getName());
        
        try {
            // 1秒の遅延を挿入
            try {
                Thread.sleep(1000); // 1000ミリ秒 = 1秒
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // 割り込みを再設定
                logger.warn("タスク自動再設定の遅延中に割り込みが発生しました。", ie);
            }

            // メイン処理を実行
            taskAutoResetService.runScheduledCheck();
            
            LocalDateTime endTime = LocalDateTime.now();
            logger.info("=== タスク自動再設定を正常完了 ===");
            logger.info("完了時刻: " + endTime);
            logger.info("処理時間: " + java.time.Duration.between(startTime, endTime).toMillis() + "ms");
            
        } catch (Exception e) {
            LocalDateTime errorTime = LocalDateTime.now();
            logger.error("=== タスク自動再設定でエラーが発生 ===");
            logger.error("エラー発生時刻: " + errorTime);
            logger.error("エラー内容: " + e.getMessage());
            logger.error("スタックトレース:");
            e.printStackTrace();
            
            // エラー後も次回実行を継続するため、ここで例外を再スローしない
        }
        
        logger.info("=== タスク自動再設定処理終了 ===");
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
    
    /**
     * デバッグ用：指定秒後に自動実行をテストする
     *
     * @param delaySeconds 実行までの遅延秒数
     */
    public void scheduleTestExecution(int delaySeconds) {
        logger.info("=== デバッグ用テスト実行をスケジュール ===");
        logger.info("実行予定: " + delaySeconds + "秒後");
        logger.info("予定時刻: " + LocalDateTime.now().plusSeconds(delaySeconds));
        
        scheduler.schedule(() -> {
            logger.info("=== デバッグ用テスト実行開始 ===");
            executeAutoReset();
            logger.info("=== デバッグ用テスト実行完了 ===");
        }, delaySeconds, TimeUnit.SECONDS);
    }
}
