package com.habit.tools;

import com.habit.domain.Task;
import com.habit.domain.Team;
import com.habit.domain.User;
import com.habit.domain.UserTaskStatus;
import com.habit.server.repository.TaskRepository;
import com.habit.server.repository.TeamRepository;
import com.habit.server.repository.UserRepository;
import com.habit.server.repository.UserTaskStatusRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class DemoDataGenerator {

    public static void main(String[] args) {
        System.out.println("Starting demo data generation...");

        // Repositoryのインスタンス化
        UserRepository userRepository = new UserRepository();
        TeamRepository teamRepository = new TeamRepository();
        TaskRepository taskRepository = new TaskRepository();
        UserTaskStatusRepository userTaskStatusRepository = new UserTaskStatusRepository();

        // 1. Create a Team
        Team team = new Team(UUID.randomUUID().toString(), "プレゼン用デモチーム", "creator-user-id", "all");
        // teamRepository.save()のシグネチャに合わせて修正
        List<String> initialMembers = new ArrayList<>();
        initialMembers.add(team.getCreatorId());
        teamRepository.save(team, "passcode", 10, "all", initialMembers);
        System.out.println("Created Team: " + team.getteamName());

        // 2. Create Users
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            String rawPassword = "password";
            String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
            User user = new User(UUID.randomUUID().toString(), "User" + i, hashedPassword);
            user.addJoinedTeamId(team.getTeamID());
            userRepository.save(user);
            users.add(user);
            // team_membersテーブルにも追加
            teamRepository.addMemberByTeamName(team.getteamName(), user.getUserId());
        }
        System.out.println("Created " + users.size() + " users.");

        // 3. Create Tasks
        List<Task> tasks = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            // Taskのコンストラクタに合わせて修正
            Task task = new Task(UUID.randomUUID().toString(), "デモタスク " + i, "これはタスク" + i + "の説明です。", team.getTeamID(), LocalDate.now(), "daily");
            taskRepository.save(task);
            tasks.add(task);
        }
        System.out.println("Created " + tasks.size() + " tasks.");

        // 4. Simulate Task Completions for the past 7 days
        Random random = new Random();
        LocalDate today = LocalDate.now();
        int statusCount = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.minusDays(i);
            for (User user : users) {
                for (Task task : tasks) {
                    // Randomly decide if the user completed the task on this day
                    boolean isCompleted = random.nextBoolean();
                    // Create a status record regardless of completion, to ensure auto-reset works
                    UserTaskStatus status = new UserTaskStatus(user.getUserId(), task.getTaskId(), team.getTeamID(), date, isCompleted);
                    userTaskStatusRepository.save(status);
                    statusCount++;
                }
            }
        }
        System.out.println("Generated " + statusCount + " task completion records.");
        System.out.println("Demo data generation finished.");
    }
}
