package com.habit.server.controller;

import com.habit.server.service.TaskAutoResetService;
import com.habit.server.scheduler.TaskAutoResetScheduler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

/**
 * タスク自動再設定の手動実行用コントローラー
 *
 * 【提供API】
 * - GET /manualTaskReset : 全チーム対象の手動実行
 * - GET /manualTaskResetTeam?teamId=xxx : 特定チーム対象の手動実行
 *
 * 【用途】
 * - 緊急時の手動実行（スケジューラーを待たずに即座に実行）
 * - テスト・デバッグ時の動作確認
 * - 特定チームのみの再設定が必要な場合
 *
 * 【注意事項】
 * TaskAutoResetServiceには重複実行防止機能があるため、
 * スケジューラー実行中に手動実行してもスキップされる
 */
public class TaskAutoResetController {
    private final TaskAutoResetService taskAutoResetService;
    private final TaskAutoResetScheduler taskAutoResetScheduler;
    
    public TaskAutoResetController(TaskAutoResetService taskAutoResetService) {
        this.taskAutoResetService = taskAutoResetService;
        this.taskAutoResetScheduler = null; // スケジューラーは後で設定
    }
    
    public TaskAutoResetController(TaskAutoResetService taskAutoResetService, com.habit.server.scheduler.TaskAutoResetScheduler taskAutoResetScheduler) {
        this.taskAutoResetService = taskAutoResetService;
        this.taskAutoResetScheduler = taskAutoResetScheduler;
    }
    
    /**
     * 手動実行API（全チーム対象）
     *
     * 【エンドポイント】GET /manualTaskReset
     * 【実行対象】全チームのタスク
     * 【戻り値】実行結果メッセージ（text/plain）
     */
    public HttpHandler getManualResetHandler() {
        return new ManualResetHandler();
    }
    
    /**
     * 特定チーム用手動実行API
     *
     * 【エンドポイント】GET /manualTaskResetTeam?teamId=xxx
     * 【実行対象】指定チームのタスクのみ
     * 【パラメータ】teamId（必須）
     * 【戻り値】実行結果メッセージ（text/plain）
     */
    public HttpHandler getManualResetTeamHandler() {
        return new ManualResetTeamHandler();
    }
    
    /**
     * デバッグ用テスト実行API
     *
     * 【エンドポイント】GET /debugScheduledReset?delay=秒数
     * 【実行対象】指定秒後に自動実行をテスト
     * 【パラメータ】delay（必須、デフォルト10秒）
     * 【戻り値】実行結果メッセージ（text/plain）
     */
    public HttpHandler getDebugScheduledResetHandler() {
        return new DebugScheduledResetHandler();
    }

    /**
     * デバッグ用サボり報告API
     *
     * 【エンドポイント】GET /debugSabotageReport
     * 【実行対象】全チームの「今日まで」の未消化タスク
     * 【戻り値】実行結果メッセージ（text/plain）
     */
    public HttpHandler getDebugSabotageReportHandler() {
        return new DebugSabotageReportHandler();
    }
    
    /**
     * 全チーム対象の手動実行ハンドラー
     *
     * 【処理内容】
     * 1. TaskAutoResetService.runScheduledCheck()を呼び出し
     * 2. 全チームのタスクを自動再設定
     * 3. 実行結果をレスポンスとして返却
     */
    private class ManualResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response;
            
            try {
                // 全チームのタスク自動再設定を実行
                taskAutoResetService.runScheduledCheck();
                response = "タスク自動再設定を手動実行しました";
            } catch (Exception e) {
                response = "エラーが発生しました: " + e.getMessage();
                e.printStackTrace();
            }
            
            // レスポンス設定
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes("UTF-8"));
            }
        }
    }
    
    /**
     * 特定チーム対象の手動実行ハンドラー
     *
     * 【処理内容】
     * 1. クエリパラメータからteamIdを取得
     * 2. TaskAutoResetService.checkAndResetTasks(teamId)を呼び出し
     * 3. 指定チームのタスクのみを自動再設定
     * 4. 実行結果をレスポンスとして返却
     *
     * 【パラメータ】
     * teamId: 対象チームID（必須）
     */
    private class ManualResetTeamHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // クエリパラメータからteamIdを取得
            String query = exchange.getRequestURI().getQuery();
            String teamId = null;
            
            if (query != null && query.startsWith("teamId=")) {
                teamId = java.net.URLDecoder.decode(query.substring(7), "UTF-8");
            }
            
            String response;
            
            // teamIdパラメータの検証
            if (teamId == null || teamId.isEmpty()) {
                response = "teamIdパラメータが必要です";
            } else {
                try {
                    // 指定チームのタスク自動再設定を実行
                    taskAutoResetService.checkAndResetTasks(teamId, java.time.LocalDate.now());
                    response = "チーム " + teamId + " のタスク自動再設定を実行しました";
                } catch (Exception e) {
                    response = "エラーが発生しました: " + e.getMessage();
                    e.printStackTrace();
                }
            }
            
            // レスポンス設定
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes("UTF-8"));
            }
        }
    }
    
    /**
     * デバッグ用スケジュール実行ハンドラー
     *
     * 【処理内容】
     * 1. クエリパラメータからdelay（秒数）を取得
     * 2. TaskAutoResetScheduler.scheduleTestExecution(delay)を呼び出し
     * 3. 指定秒後にタスク自動再設定をテスト実行
     * 4. 実行結果をレスポンスとして返却
     *
     * 【パラメータ】
     * delay: 実行までの遅延秒数（デフォルト10秒）
     */
    private class DebugScheduledResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // クエリパラメータからdelayを取得
            String query = exchange.getRequestURI().getQuery();
            int delay = 10; // デフォルト10秒
            
            if (query != null && query.startsWith("delay=")) {
                try {
                    delay = Integer.parseInt(query.substring(6));
                } catch (NumberFormatException e) {
                    delay = 10; // パース失敗時はデフォルト値
                }
            }
            
            String response;
            
            // スケジューラーの可用性チェック
            if (taskAutoResetScheduler == null) {
                response = "スケジューラーが利用できません";
            } else {
                try {
                    // デバッグ用テスト実行をスケジュール
                    taskAutoResetScheduler.scheduleTestExecution(delay);
                    response = delay + "秒後にタスク自動再設定のテスト実行をスケジュールしました";
                } catch (Exception e) {
                    response = "エラーが発生しました: " + e.getMessage();
                    e.printStackTrace();
                }
            }
            
            // レスポンス設定
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes("UTF-8"));
            }
        }
    }

    /**
     * デバッグ用サボり報告ハンドラー
     *
     * 【処理内容】
     * 1. TaskAutoResetService.runDebugSabotageReportForToday()を呼び出し
     * 2. 全チームの「今日まで」の未消化タスクでサボり報告を送信
     * 3. 実行結果をレスポンスとして返却
     */
    private class DebugSabotageReportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response;
            
            try {
                // 「今日まで」の未消化タスクでサボり報告を実行
                taskAutoResetService.runDebugSabotageReportForToday();
                response = "デバッグ用サボり報告を実行しました（今日までの未消化タスクを対象）";
            } catch (Exception e) {
                response = "エラーが発生しました: " + e.getMessage();
                e.printStackTrace();
            }
            
            // レスポンス設定
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes("UTF-8"));
            }
        }
    }
}
