package com.habit.server.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.habit.domain.Task;
import com.habit.domain.UserTaskStatus;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TaskRepositoryTest {
  private TaskRepository repo;

  @BeforeEach
  void setUp(@TempDir Path tempDir) {
    // create a new empty SQLite DB file for each test
    String url = "jdbc:sqlite:" + tempDir.resolve("test.db").toString();
    repo = new TaskRepository(url);
  }

  @Test
  void findUserTaskStatus_whenNoneExists_returnsNull() {
    LocalDate today = LocalDate.now();
    UserTaskStatus status = repo.findUserTaskStatus("alice", "task-1", today);
    assertNull(status, "Should return null when no status record exists");
  }

  @Test
  void saveAndFindUserTaskStatus_roundTripsCorrectly() {
    LocalDate date = LocalDate.of(2025, 7, 1);
    UserTaskStatus toSave = new UserTaskStatus("bob", "task-42", date, false);
    // mark done (sets completionTimestamp internally)
    toSave.setDone(true);

    repo.saveUserTaskStatus(toSave);

    UserTaskStatus found = repo.findUserTaskStatus("bob", "task-42", date);
    assertNotNull(found, "Should find the status we just saved");
    assertEquals("bob", found.getUserId());
    assertEquals("task-42", found.getTaskId());
    assertEquals(date, found.getDate());
    assertTrue(found.isDone(), "Done flag should be true");
    assertNotNull(found.getCompletionTimestamp(),
                  "Completion timestamp should be set");
  }

  @Test
  void findUserTaskStatusesForPeriod_filtersByDateRange() {
    String user = "carol";
    // three dates: 1st, 2nd, 3rd
    LocalDate d1 = LocalDate.of(2025, 7, 1);
    LocalDate d2 = d1.plusDays(1);
    LocalDate d3 = d1.plusDays(2);

    UserTaskStatus s1 = new UserTaskStatus(user, "t1", d1, false);
    UserTaskStatus s2 = new UserTaskStatus(user, "t2", d2, true);
    UserTaskStatus s3 = new UserTaskStatus(user, "t3", d3, false);

    // setDone on s2 so it has a timestamp
    s2.setDone(true);

    repo.saveUserTaskStatus(s1);
    repo.saveUserTaskStatus(s2);
    repo.saveUserTaskStatus(s3);

    List<UserTaskStatus> list =
        repo.findUserTaskStatusesForPeriod(user, d1, d2);
    assertEquals(2, list.size(), "Should only get statuses on 1st & 2nd");
    // ensure dates are within [d1, d2]
    assertTrue(list.stream().allMatch(
        s -> !s.getDate().isBefore(d1) && !s.getDate().isAfter(d2)));
  }

  @Test
  void saveAndFindTeamTasksByTeamID_includesOnlyTeamTasks() {
    String teamId = "team-alpha";

    // team task
    Task teamTask = new Task("tk1", "Team meeting", "Discuss Q3 goals", 30,
                             List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                             true, LocalTime.of(9, 0), "WEEKLY");
    repo.saveTask(teamTask, teamId);

    // non-team task (still passing same teamId, but isTeamTask=false)
    Task personal = new Task("tk2", "Personal todo", "Buy groceries", 15,
                             List.of(), false, null, "DAILY");
    repo.saveTask(personal, teamId);

    List<Task> found = repo.findTeamTasksByTeamID(teamId);
    assertEquals(1, found.size(),
                 "Should only return tasks with isTeamTask=true");

    Task t = found.get(0);
    assertEquals("tk1", t.getTaskId());
    assertEquals("Team meeting", t.getTaskName());
    assertEquals("Discuss Q3 goals", t.getDescription());
    assertEquals(30, t.getEstimatedMinutes());
    assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                 t.getRepeatDays());
    assertTrue(t.isTeamTask());
    assertEquals(LocalTime.of(9, 0), t.getDueTime());
    assertEquals("WEEKLY", t.getCycleType());
  }
}
