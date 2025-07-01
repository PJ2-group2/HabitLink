package com.habit.server.service;

import com.habit.domain.Task;
import com.habit.domain.Team;
import com.habit.domain.TeamMode;
import com.habit.domain.UserTaskStatus;
import com.habit.server.repository.TaskRepository;
import com.habit.server.repository.TeamRepository;
import com.habit.server.repository.UserTaskStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TeamTaskServiceのテストクラス
 */
public class TeamTaskServiceTest {
    private TeamTaskService teamTaskService;
    private TaskRepository taskRepository;
    private TeamRepository teamRepository;
    private UserTaskStatusRepository userTaskStatusRepository;

    @BeforeEach
    void setUp() {
        // テスト用のリポジトリを初期化
        taskRepository = new TaskRepository("jdbc:sqlite::memory:");
        teamRepository = new TeamRepository("jdbc:sqlite::memory:");
        userTaskStatusRepository = new UserTaskStatusRepository();
        
        teamTaskService = new TeamTaskService(taskRepository, teamRepository, userTaskStatusRepository);
        
        // テスト用のチームを作成
        Team testTeam = new Team("team001", "テストチーム", "creator001", TeamMode.FIXED_TASK_MODE);
        testTeam.addMember("user001");
        testTeam.addMember("user002");
        testTeam.addMember("user003");
        
        // チームを保存
        teamRepository.save(testTeam, "testpass", 10, "all", "habit", "public", 
            Arrays.asList("user001", "user002", "user003"));
    }

    @Test
    void testCreateTeamTask() {
        // チーム共通タスクを作成
        Task teamTask = new Task(
            "task001",
            "朝の運動",
            "毎日30分の運動",
            30,
            Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            true,
            "team001",
            LocalTime.of(7, 0),
            "daily"
        );

        // TeamTaskServiceを使用してタスクを作成
        Task createdTask = teamTaskService.createTeamTask(teamTask);
        
        // タスクが正常に作成されたことを確認
        assertNotNull(createdTask);
        assertEquals("task001", createdTask.getTaskId());
        assertEquals("team001", createdTask.getTeamId());
        assertTrue(createdTask.isTeamTask());
        
        // 全チームメンバーにUserTaskStatusが作成されたことを確認
        LocalDate today = LocalDate.now();
        assertTrue(userTaskStatusRepository.findByUserIdAndTaskIdAndDate("user001", "task001", today).isPresent());
        assertTrue(userTaskStatusRepository.findByUserIdAndTaskIdAndDate("user002", "task001", today).isPresent());
        assertTrue(userTaskStatusRepository.findByUserIdAndTaskIdAndDate("user003", "task001", today).isPresent());
    }

    @Test
    void testCreateUserTaskStatusForNewMember() {
        // 既存のチーム共通タスクを作成
        Task teamTask = new Task(
            "task002",
            "読書習慣",
            "毎日30分読書",
            30,
            Arrays.asList(DayOfWeek.values()),
            true,
            "team001",
            LocalTime.of(20, 0),
            "daily"
        );
        teamTaskService.createTeamTask(teamTask);
        
        // 新しいメンバーを追加
        teamTaskService.createUserTaskStatusForNewMember("team001", "user004");
        
        // 新メンバーに既存タスクが紐づけられたことを確認
        LocalDate today = LocalDate.now();
        assertTrue(userTaskStatusRepository.findByUserIdAndTaskIdAndDate("user004", "task002", today).isPresent());
    }

    @Test
    void testGetTeamTaskCompletionRate() {
        // チーム共通タスクを作成
        Task teamTask = new Task(
            "task003",
            "水分補給",
            "1日2L水を飲む",
            5,
            Arrays.asList(DayOfWeek.values()),
            true,
            "team001",
            LocalTime.of(12, 0),
            "daily"
        );
        teamTaskService.createTeamTask(teamTask);
        
        LocalDate today = LocalDate.now();
        
        // 一部のメンバーがタスクを完了
        UserTaskStatus status1 = userTaskStatusRepository.findByUserIdAndTaskIdAndDate("user001", "task003", today).get();
        status1.setDone(true);
        userTaskStatusRepository.save(status1);
        
        UserTaskStatus status2 = userTaskStatusRepository.findByUserIdAndTaskIdAndDate("user002", "task003", today).get();
        status2.setDone(true);
        userTaskStatusRepository.save(status2);
        
        // 完了率を計算
        double completionRate = teamTaskService.getTeamTaskCompletionRate("team001", "task003", today);
        
        // 3人中2人完了なので、完了率は約0.67
        assertEquals(0.67, completionRate, 0.01);
    }

    @Test
    void testGetUserTeamTaskStatuses() {
        // 複数のチーム共通タスクを作成
        Task task1 = new Task("task004", "ストレッチ", "朝のストレッチ", 10, 
            Arrays.asList(DayOfWeek.values()), true, "team001", LocalTime.of(6, 30), "daily");
        Task task2 = new Task("task005", "瞑想", "夜の瞑想", 15, 
            Arrays.asList(DayOfWeek.values()), true, "team001", LocalTime.of(22, 0), "daily");
        
        teamTaskService.createTeamTask(task1);
        teamTaskService.createTeamTask(task2);
        
        // ユーザーのチーム共通タスク一覧を取得
        List<UserTaskStatus> userTeamTasks = teamTaskService.getUserTeamTaskStatuses("user001", LocalDate.now());
        
        // 2つのタスクが取得されることを確認
        assertEquals(2, userTeamTasks.size());
        
        // 各タスクがチーム共通タスクであることを確認
        for (UserTaskStatus status : userTeamTasks) {
            assertEquals("team001", status.getTeamId());
            assertEquals("user001", status.getUserId());
        }
    }
}