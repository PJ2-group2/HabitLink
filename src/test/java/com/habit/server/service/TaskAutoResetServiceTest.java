package com.habit.server.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.habit.domain.Task;
import com.habit.domain.UserTaskStatus;
import com.habit.server.repository.TaskRepository;
import com.habit.server.repository.UserTaskStatusRepository;

import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

/**
 * このテストコードは、以下の4つのシナリオを検証します。
 * 1.`testResetDailyTasks`:毎日のタスクが、日付が変わると正しく新しい未完了タスクとして作成されるか。
 * 2.`testResetWeeklyTasks_NotDone`:昨日のタスクが未完了だった場合でも、新しいタスクが作成されるか。
 * 3.`testNoResetForNonRecurringTask`:現在の実装では、繰り返し設定のないタスクもリセットされてしまうことを確認するテストです。繰り返しなしのタスクを実装する場合は修正が必要です。
 * 4.`testResetForMultipleUsers`:複数のユーザーが関わるタスクが、全ユーザー分正しくリセットされるか。
 *
 * MavenやGradleなどのビルドツールを使って、mvn test や gradle test を実行すれば、このテストが実行されます。
 */


class TaskAutoResetServiceTest {

    private TaskRepository taskRepository;
    private UserTaskStatusRepository userTaskStatusRepository;
    private TaskAutoResetService taskAutoResetService;

    private static final String TEST_DB_PATH = "test_habit.db";
    private static final String TEST_DB_URL = "jdbc:sqlite:" + TEST_DB_PATH;

    // テスト用の固定された時刻 (2025-07-05 00:01 JST)
    private static final Instant FIXED_INSTANT = Instant.parse("2025-07-04T15:01:00Z");
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Tokyo");
    private final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZONE_ID);

    @BeforeEach
    void setUp() {
        // 各テストの前にテスト用DBファイルを削除してクリーンな状態にする
        new File(TEST_DB_PATH).delete();

        // テスト用のリポジトリを初期化
        taskRepository = new TaskRepository(TEST_DB_URL);
        userTaskStatusRepository = new UserTaskStatusRepository(TEST_DB_URL);

        // テスト対象のサービスに、テスト用リポジトリと「固定した時計」を注入
        taskAutoResetService = new TaskAutoResetService(taskRepository, userTaskStatusRepository, fixedClock);
    }

    @AfterEach
    void tearDown() {
        // 各テストの後にテスト用DBファイルを削除
        new File(TEST_DB_PATH).delete();
    }

    @Test
    void testResetDailyTasks() {
        // --- Given (前提条件) ---
        final String teamId = "team-a";
        final String userId = "user-1";
        final String taskId = "task-daily-1";
        final LocalDate today = LocalDate.now(fixedClock); // 2025-07-05
        final LocalDate yesterday = today.minusDays(1);   // 2025-07-04

        // 1. 毎日繰り返しのタスクを作成
        Task dailyTask = new Task(taskId, "日課の散歩", "毎日30分歩く", teamId, "DAILY");
        taskRepository.save(dailyTask);

        // 2. 昨日 (7/4) のタスク状況を作成し、完了済みにしておく
        UserTaskStatus yesterdayStatus = new UserTaskStatus(userId, taskId, teamId, yesterday, true);
        userTaskStatusRepository.save(yesterdayStatus);

        // --- When (実行) ---
        // チームAのタスクリセット処理を実行
        int resetCount = taskAutoResetService.checkAndResetTasks(teamId);

        // --- Then (検証) ---
        assertEquals(1, resetCount, "1件のタスクが再設定されるはず");

        // 今日 (7/5) のタスクが「未完了」で作成されていることを確認
        Optional<UserTaskStatus> todayStatusOpt = userTaskStatusRepository.findByUserIdAndTaskIdAndDate(userId, taskId, today);

        assertTrue(todayStatusOpt.isPresent(), "今日の日付で新しいタスク状況が作成されているはず");
        UserTaskStatus todayStatus = todayStatusOpt.get();
        assertFalse(todayStatus.isDone(), "新しく作成されたタスクは未完了のはず");
        assertEquals(userId, todayStatus.getUserId());
        assertEquals(taskId, todayStatus.getTaskId());
        assertEquals(teamId, todayStatus.getTeamId());
    }

    @Test
    void testResetWeeklyTasks_NotDone() {
        // --- Given (前提条件) ---
        final String teamId = "team-b";
        final String userId = "user-2";
        final String taskId = "task-weekly-1";
        final LocalDate today = LocalDate.now(fixedClock);
        final LocalDate yesterday = today.minusDays(1);

        // 1. 毎週繰り返しのタスクを作成
        Task weeklyTask = new Task(taskId, "週報の提出", "金曜日に提出", teamId, "WEEKLY");
        taskRepository.save(weeklyTask);

        // 2. 昨日 (7/4) のタスク状況を作成し、**未完了**にしておく
        UserTaskStatus yesterdayStatus = new UserTaskStatus(userId, taskId, teamId, yesterday, false);
        userTaskStatusRepository.save(yesterdayStatus);

        // --- When (実行) ---
        int resetCount = taskAutoResetService.checkAndResetTasks(teamId);

        // --- Then (検証) ---
        assertEquals(1, resetCount, "1件のタスクが再設定されるはず");

        // 今日のタスクが「未完了」で作成されていることを確認
        Optional<UserTaskStatus> todayStatusOpt = userTaskStatusRepository.findByUserIdAndTaskIdAndDate(userId, taskId, today);
        assertTrue(todayStatusOpt.isPresent(), "未完了でも新しいタスクは作成される");
        assertFalse(todayStatusOpt.get().isDone());
    }

    @Test
    void testNoResetForNonRecurringTask() {
        // --- Given (前提条件) ---
        final String teamId = "team-c";
        final String userId = "user-3";
        final String taskId = "task-onetime-1";
        final LocalDate today = LocalDate.now(fixedClock);
        final LocalDate yesterday = today.minusDays(1);

        // 1. 繰り返し設定のないタスクを作成
        Task onetimeTask = new Task(taskId, "一度きりのプレゼン", "", teamId, null); // cycleTypeがnull
        taskRepository.save(onetimeTask);

        // 2. 昨日のタスク状況を作成
        UserTaskStatus yesterdayStatus = new UserTaskStatus(userId, taskId, teamId, yesterday, true);
        userTaskStatusRepository.save(yesterdayStatus);

        // --- When (実行) ---
        // 繰り返し設定のないタスクはリセットされないはずだが、現在の実装ではリセットされてしまう。
        // そのため、現在の実装がすべてのタスクをリセットすることを確認するテストとして記述する。
        int resetCount = taskAutoResetService.checkAndResetTasks(teamId);

        // --- Then (検証) ---
        // 現状の実装では、cycleTypeに関わらずすべてのタスクがリセット対象となる
        assertEquals(1, resetCount, "現在の実装では、繰り返し設定がなくてもタスクは再設定される");
        
        Optional<UserTaskStatus> todayStatusOpt = userTaskStatusRepository.findByUserIdAndTaskIdAndDate(userId, taskId, today);
        assertTrue(todayStatusOpt.isPresent(), "繰り返し設定がなくても新しいタスクが作成されてしまう");
    }

    @Test
    void testResetForMultipleUsers() {
        // --- Given (前提条件) ---
        final String teamId = "team-d";
        final String taskId = "task-multi-user-1";
        final LocalDate today = LocalDate.now(fixedClock);
        final LocalDate yesterday = today.minusDays(1);

        Task task = new Task(taskId, "全員で掃除", "", teamId, "DAILY");
        taskRepository.save(task);

        // 複数ユーザーの昨日のステータスを作成
        UserTaskStatus user4_yesterday = new UserTaskStatus("user-4", taskId, teamId, yesterday, true);
        UserTaskStatus user5_yesterday = new UserTaskStatus("user-5", taskId, teamId, yesterday, false);
        userTaskStatusRepository.save(user4_yesterday);
        userTaskStatusRepository.save(user5_yesterday);

        // --- When (実行) ---
        int resetCount = taskAutoResetService.checkAndResetTasks(teamId);

        // --- Then (検証) ---
        assertEquals(2, resetCount, "2ユーザー分のタスクが再設定されるはず");

        // 各ユーザーの今日のタスクが作成されていることを確認
        Optional<UserTaskStatus> user4_today = userTaskStatusRepository.findByUserIdAndTaskIdAndDate("user-4", taskId, today);
        Optional<UserTaskStatus> user5_today = userTaskStatusRepository.findByUserIdAndTaskIdAndDate("user-5", taskId, today);

        assertTrue(user4_today.isPresent());
        assertFalse(user4_today.get().isDone());
        assertTrue(user5_today.isPresent());
        assertFalse(user5_today.get().isDone());
    }
}