// src/test/java/com/habit/server/repository/UserRepositoryTest.java
package com.habit.server.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.habit.domain.User;
import java.nio.file.Path;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class UserRepositoryTest {

  private UserRepository repo;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    // use a file-backed DB in a temp directory so all connections share state
    Path dbFile = tempDir.resolve("test-users.db");
    String url = "jdbc:sqlite:" + dbFile.toAbsolutePath();
    repo = new UserRepository(url);
  }

  @Test
  void testSaveAndFindById() {
    User u = new User("u1", "alice", "pass123");
    u.addSabotagePoints(5);
    u.addJoinedTeamId("teamA");
    u.addJoinedTeamId("teamB");

    repo.save(u);
    User fetched = repo.findById("u1");

    assertNotNull(fetched, "Should find saved user by ID");
    assertEquals(u.getUserId(), fetched.getUserId());
    assertEquals(u.getUsername(), fetched.getUsername());
    assertEquals(u.getHashedPassword(), fetched.getHashedPassword());
    assertEquals(30, fetched.getSabotagePoints());
    assertIterableEquals(u.getJoinedTeamIds(), fetched.getJoinedTeamIds(),
                         "JoinedTeamIds should round-trip");
  }

  @Test
  void testFindByUsername() {
    User u = new User("u2", "bob", "pwd");
    repo.save(u);

    User byName = repo.findByUsername("bob");
    assertNotNull(byName, "Should find saved user by username");
    assertEquals("u2", byName.getUserId());
  }

  @Test
  void testUpdateSabotagePoints() {
    User u = new User("u3", "carol", "x");
    repo.save(u);

    repo.updateSabotagePoints("u3", 42);
    User updated = repo.findById("u3");
    assertNotNull(updated);
    assertEquals(42, updated.getSabotagePoints());
  }

  @Test
  void testNonexistentReturnsNull() {
    assertNull(repo.findById("nope"), "Unknown ID should yield null");
    assertNull(repo.findByUsername("nobody"),
               "Unknown username should yield null");
  }

  @Test
  void testReplaceOnSave() {
    User u = new User("u4", "dave", "init");
    u.addSabotagePoints(1);
    repo.save(u);

    // change fields locally and save again
    u.setPassword("newpass");
    u.addSabotagePoints(4); // now total 5
    u.addJoinedTeamId("teamX");
    repo.save(u);

    User fetched = repo.findById("u4");
    assertEquals("newpass", fetched.getHashedPassword());
    assertEquals(30, fetched.getSabotagePoints());
    assertTrue(fetched.getJoinedTeamIds().contains("teamX"));
  }
}
