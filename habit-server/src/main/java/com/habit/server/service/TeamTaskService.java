package com.habit.server.service;

import com.habit.domain.Task;
import com.habit.domain.Team;
import com.habit.domain.UserTaskStatus;
import com.habit.server.repository.TaskRepository;
import com.habit.server.repository.TeamRepository;
import com.habit.server.repository.UserTaskStatusRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * チーム共通タスクの管理を行うサービスクラス
 */
public class TeamTaskService {
    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final UserTaskStatusRepository userTaskStatusRepository;

    public TeamTaskService(TaskRepository taskRepository, TeamRepository teamRepository, 
                          UserTaskStatusRepository userTaskStatusRepository) {
        this.taskRepository = taskRepository;
        this.teamRepository = teamRepository;
        this.userTaskStatusRepository = userTaskStatusRepository;
    }

    /**
     * チーム共通タスクを作成し、全チームメンバーにUserTaskStatusを自動生成
     * 
     * @param task チーム共通タスク
     * @return 作成されたタスク
     */
    public Task createTeamTask(Task task) {
        // タスクを保存
        Task savedTask = taskRepository.save(task);

        // チームの全メンバーに対してUserTaskStatusを生成
        createUserTaskStatusForAllMembers(savedTask);

        return savedTask;
    }

    /**
     * チームの全メンバーに対してUserTaskStatusを生成
     * 
     * @param task チーム共通タスク
     */
    public void createUserTaskStatusForAllMembers(Task task) {
        Team team = teamRepository.findById(task.getTeamId());
        if (team == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        
        // チームの全メンバーに対してUserTaskStatusを作成
        for (String memberId : team.getMemberIds()) {
            // 既存のUserTaskStatusがないことを確認（taskIdでチェック）
            boolean existsByTaskId = userTaskStatusRepository.findByUserIdAndTaskIdAndDate(
                memberId, task.getTaskId(), today).isPresent();

            if (!existsByTaskId) {
                UserTaskStatus newStatus = new UserTaskStatus(
                    memberId,
                    task.getTaskId(),
                    task.getTeamId(),
                    task.getDueDate(), // タスクのdueDateをそのまま使用
                    false
                );
                userTaskStatusRepository.save(newStatus);
            }
        }
    }

    /**
     * 新しいメンバーがチームに参加した際に、既存のチーム共通タスクのUserTaskStatusを生成
     * 
     * @param teamId チームID
     * @param newMemberId 新しいメンバーのID
     */
    public void createUserTaskStatusForNewMember(String teamId, String newMemberId) {
        // チームの全タスクを取得
        List<Task> teamTasks = taskRepository.findByTeamId(teamId);
        
        LocalDate today = LocalDate.now();
        
        for (Task task : teamTasks) {

            // 既存のUserTaskStatusがないことを確認（taskIdでチェック）
            boolean existsByTaskId = userTaskStatusRepository.findByUserIdAndTaskIdAndDate(
                newMemberId, task.getTaskId(), today).isPresent();

            if (!existsByTaskId) {
                UserTaskStatus newStatus = new UserTaskStatus(
                    newMemberId,
                    task.getTaskId(),
                    teamId,
                    task.getDueDate(), // タスクのdueDateをそのまま使用
                    false
                );
                userTaskStatusRepository.save(newStatus);
            }
            
        }
    }

    /**
     * チーム共通タスクの進捗状況を取得（チーム全体の統計）
     * 
     * @param teamId チームID
     * @param taskId タスクID
     * @param date 対象日
     * @return 完了率（0.0〜1.0）
     */
    public double getTeamTaskCompletionRate(String teamId, String taskId, LocalDate date) {
        Team team = teamRepository.findById(teamId);
        if (team == null) {
            return 0.0;
        }

        int totalMembers = team.getMemberIds().size();
        if (totalMembers == 0) {
            return 0.0;
        }

        int completedMembers = 0;
        for (String memberId : team.getMemberIds()) {
            userTaskStatusRepository.findByUserIdAndTaskIdAndDate(
                memberId, taskId, date).ifPresent(status -> {
                if (status.isDone()) {
                    // Java8のラムダ式ではローカル変数を変更できないため、配列を使用
                }
            });
        }
        
        // より簡潔な実装に変更
        for (String memberId : team.getMemberIds()) {
            if (userTaskStatusRepository.findByUserIdAndTaskIdAndDate(
                memberId, taskId, date).map(UserTaskStatus::isDone).orElse(false)) {
                completedMembers++;
            }
        }

        return (double) completedMembers / totalMembers;
    }

    /**
     * 指定したユーザーのチーム共通タスク一覧を取得
     * 
     * @param userId ユーザーID
     * @param date 対象日
     * @return チーム共通タスクのUserTaskStatus一覧
     */
    public List<UserTaskStatus> getUserTeamTaskStatuses(String userId, LocalDate date) {
        return userTaskStatusRepository.findByUserIdAndDateAndTeamIdNotNull(userId, date);
    }

    /**
     * タスクを削除する
     * @param teamId チームID
     * @param taskId タスクID
     * @param userId ユーザーID
     */
    public void deleteTask(String teamId, String taskId, String userId) {
        Team team = teamRepository.findById(teamId);
        if (team == null) {
            throw new IllegalArgumentException("チームが見つかりません");
        }

        // 権限チェック
        if ("CREATOR_ONLY".equals(team.getEditPermission()) && !team.getCreatorId().equals(userId)) {
            throw new SecurityException("タスクの削除権限がありません");
        }

        taskRepository.deleteById(taskId);
    }
}