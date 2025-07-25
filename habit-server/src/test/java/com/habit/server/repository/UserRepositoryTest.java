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
    System.out.println("[テスト開始] testSaveAndFindById / ユーザー保存とID検索テスト");
    User u = new User("u1", "alice", "pass123");
    u.addSabotagePoints(5);
    u.addJoinedTeamId("teamA");
    u.addJoinedTeamId("teamB");

    repo.save(u);
    System.out.println("ユーザーを保存: " + u.getUserId() + ", 名前: " + u.getUsername() + ", パスワード: " + u.getPassword() + ", サボタージュポイント: " + u.getSabotagePoints() + ", チーム: " + u.getJoinedTeamIds());

    User fetched = repo.findById("u1");
    System.out.println("ID検索で取得: " + (fetched != null ? fetched.getUserId() : "null"));

    assertNotNull(fetched, "Should find saved user by ID");
    assertEquals(u.getUserId(), fetched.getUserId());
    assertEquals(u.getUsername(), fetched.getUsername());
    assertEquals(u.getPassword(), fetched.getPassword());
    assertEquals(30, fetched.getSabotagePoints());
    assertIterableEquals(u.getJoinedTeamIds(), fetched.getJoinedTeamIds(),
                         "JoinedTeamIds should round-trip");
    System.out.println("[成功] testSaveAndFindById / ユーザー保存とID検索テスト完了");
  }

  @Test
  void testFindByUsername() {
    System.out.println("[テスト開始] testFindByUsername / ユーザー名による検索テスト");
    User u = new User("u2", "bob", "pwd");
    repo.save(u);
    System.out.println("ユーザーを保存: " + u.getUserId() + ", 名前: " + u.getUsername());

    User byName = repo.findByUsername("bob");
    System.out.println("ユーザー名検索で取得: " + (byName != null ? byName.getUserId() : "null"));
    assertNotNull(byName, "Should find saved user by username");
    assertEquals("u2", byName.getUserId());
    System.out.println("[成功] testFindByUsername / ユーザー名による検索テスト完了");
  }

  @Test
  void testUpdateSabotagePoints() {
    System.out.println("[テスト開始] testUpdateSabotagePoints / サボタージュポイント更新テスト");
    User u = new User("u3", "carol", "x");
    repo.save(u);
    System.out.println("ユーザー保存: " + u.getUserId() + ", 初期ポイント: " + u.getSabotagePoints());

    repo.updateSabotagePoints("u3", 42);
    System.out.println("サボタージュポイントを42に更新");
    User updated = repo.findById("u3");
    System.out.println("更新後取得: " + (updated != null ? updated.getSabotagePoints() : "null"));
    assertNotNull(updated);
    assertEquals(42, updated.getSabotagePoints());
    System.out.println("[成功] testUpdateSabotagePoints / サボタージュポイント更新テスト完了");
  }

  @Test
  void testNonexistentReturnsNull() {
    System.out.println("[テスト開始] testNonexistentReturnsNull / 存在しないユーザー検索テスト");
    User u1 = repo.findById("nope");
    User u2 = repo.findByUsername("nobody");
    System.out.println("ID検索結果: " + (u1 == null ? "null" : u1.getUserId()));
    System.out.println("ユーザー名検索結果: " + (u2 == null ? "null" : u2.getUserId()));
    assertNull(u1, "Unknown ID should yield null");
    assertNull(u2, "Unknown username should yield null");
    System.out.println("[成功] testNonexistentReturnsNull / 存在しないユーザー検索テスト完了");
  }

  @Test
  void testReplaceOnSave() {
    System.out.println("[テスト開始] testReplaceOnSave / saveによる上書き保存テスト");
    User u = new User("u4", "dave", "init");
    u.addSabotagePoints(1);
    repo.save(u);
    System.out.println("初回保存: " + u.getUserId() + ", パスワード: " + u.getPassword() + ", ポイント: " + u.getSabotagePoints());

    // change fields locally and save again
    u.setHashedPassword("newpass");
    u.addSabotagePoints(4); // now total 5
    u.addJoinedTeamId("teamX");
    repo.save(u);
    System.out.println("上書き保存: " + u.getUserId() + ", 新パスワード: " + u.getPassword() + ", ポイント: " + u.getSabotagePoints() + ", チーム: " + u.getJoinedTeamIds());

    User fetched = repo.findById("u4");
    System.out.println("取得: " + fetched.getUserId() + ", パスワード: " + fetched.getPassword() + ", ポイント: " + fetched.getSabotagePoints() + ", チーム: " + fetched.getJoinedTeamIds());
    assertEquals("newpass", fetched.getPassword());
    assertEquals(30, fetched.getSabotagePoints());
    assertTrue(fetched.getJoinedTeamIds().contains("teamX"));
    System.out.println("[成功] testReplaceOnSave / saveによる上書き保存テスト完了");
  }
}
