package com.habit.server.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.habit.domain.Team;
import com.habit.domain.TeamMode;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TeamRepositoryTest {

  @TempDir Path tempDir;

  private TeamRepository repository;
  private String dbUrl;

  @BeforeEach
  void setUp() {
    Path dbFile = tempDir.resolve(UUID.randomUUID().toString() + ".db");
    dbUrl = "jdbc:sqlite:" + dbFile.toAbsolutePath().toString();
    repository = new TeamRepository(dbUrl);
  }

  @Test
  void testFindTeamIdByName() {
    Team team =
        new Team("team1", "Alpha", "creatorA", TeamMode.FIXED_TASK_MODE);
    repository.save(team, "pass123", 5, "edit", "cat", "public",
                    Arrays.asList("member1", "member2"));
    String foundId = repository.findTeamIdByName("Alpha");
    assertEquals("team1", foundId);
  }

  @Test
  void testFindTeamIdByNameNotFound() {
    assertNull(repository.findTeamIdByName("Nonexistent"));
  }

  @Test
  void testFindAllPublicTeamNames() {
    Team publicTeam =
        new Team("t1", "Public1", "creator", TeamMode.FIXED_TASK_MODE);
    Team privateTeam =
        new Team("t2", "Private1", "creator", TeamMode.FIXED_TASK_MODE);
    repository.save(publicTeam, "p1", 10, "e1", "c1", "public",
                    Arrays.asList());
    repository.save(privateTeam, "p2", 10, "e1", "c1", "private",
                    Arrays.asList());
    List<String> publicNames = repository.findAllPublicTeamNames();
    assertEquals(1, publicNames.size());
    assertTrue(publicNames.contains("Public1"));
  }

  @Test
  void testFindAllPublicTeamNamesEmpty() {
    List<String> names = repository.findAllPublicTeamNames();
    assertTrue(names.isEmpty());
  }

  @Test
  void testFindTeamNameByPasscode() {
    Team team = new Team("team2", "Beta", "creatorB", TeamMode.FIXED_TASK_MODE);
    repository.save(team, "secret", 3, "e", "c", "public", Arrays.asList());
    assertEquals("Beta", repository.findTeamNameByPasscode("secret"));
  }

  @Test
  void testFindTeamNameByPasscodeNotFound() {
    assertNull(repository.findTeamNameByPasscode("wrong"));
  }

  @Test
  void testFindTeamIdByPasscode() {
    Team team =
        new Team("team3", "Gamma", "creatorC", TeamMode.FIXED_TASK_MODE);
    repository.save(team, "pc", 2, "e", "c", "private", Arrays.asList());
    assertEquals("team3", repository.findTeamIdByPasscode("pc"));
  }

  @Test
  void testFindTeamIdByPasscodeNotFound() {
    assertNull(repository.findTeamIdByPasscode("nopass"));
  }

  @Test
  void testFindTeamNameById() {
    Team team =
        new Team("team4", "Delta", "creatorD", TeamMode.FIXED_TASK_MODE);
    repository.save(team, "p", 1, "e", "c", "public", Arrays.asList());
    assertEquals("Delta", repository.findTeamNameById("team4"));
  }

  @Test
  void testFindTeamNameByIdNotFound() {
    assertNull(repository.findTeamNameById("unknown"));
  }

  @Test
  void testAddMemberByTeamName() {
    Team team = new Team("t5", "Team5", "creatorE", TeamMode.FIXED_TASK_MODE);
    repository.save(team, "p5", 5, "e5", "c5", "public", Arrays.asList());
    boolean added = repository.addMemberByTeamName("Team5", "memberX");
    assertTrue(added);
    List<String> members = repository.findMemberIdsByTeamId("t5");
    assertEquals(2, members.size());
    assertTrue(members.contains("creatorE"));
    assertTrue(members.contains("memberX"));
  }

  @Test
  void testAddMemberByTeamNameDuplicate() {
    Team team = new Team("t6", "Team6", "creatorF", TeamMode.FIXED_TASK_MODE);
    repository.save(team, "p6", 5, "e6", "c6", "public",
                    Arrays.asList("memberY"));
    boolean first = repository.addMemberByTeamName("Team6", "memberY");
    boolean second = repository.addMemberByTeamName("Team6", "memberY");
    assertTrue(first);
    assertTrue(second);
    List<String> members = repository.findMemberIdsByTeamId("t6");
    long countY = members.stream().filter("memberY" ::equals).count();
    assertEquals(1, countY);
  }

  @Test
  void testAddMemberByTeamNameNotFound() {
    assertFalse(repository.addMemberByTeamName("NoTeam", "m"));
  }

  @Test
  void testFindMemberIdsByTeamIdWithCreatorAndMembers() {
    Team team = new Team("t7", "Team7", "creatorG", TeamMode.FIXED_TASK_MODE);
    List<String> initialMembers =
        Arrays.asList("m1", "m2", "creatorG", "", null);
    repository.save(team, "p7", 5, "e7", "c7", "public", initialMembers);
    List<String> members = repository.findMemberIdsByTeamId("t7");
    assertEquals(3, members.size());
    assertEquals("creatorG", members.get(0));
    assertTrue(members.contains("m1"));
    assertTrue(members.contains("m2"));
  }
}
