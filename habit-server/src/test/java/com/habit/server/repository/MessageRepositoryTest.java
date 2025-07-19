// src/test/java/com/habit/server/repository/MessageRepositoryTest.java
package com.habit.server.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.habit.domain.Message;
import com.habit.domain.MessageType;
import com.habit.domain.User;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class MessageRepositoryTest {

  private MessageRepository repo;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() throws Exception {
    Path dbFile = tempDir.resolve("test-users.db");
    String url = "jdbc:sqlite:" + dbFile.toAbsolutePath();
    repo = new MessageRepository(url);
  }

  @Test
  void testSaveAndFindMessages() {
    // two messages for team "teamA"
    Message m1 = new Message("id1", new User("u1", "Alice", "pwd"), "teamA",
                             "hello", MessageType.NORMAL);
    Message m2 = new Message("id2", new User("u2", "Bob", "pwd"), "teamA",
                             "world", MessageType.NORMAL);

    repo.save(m1);
    repo.save(m2);

    List<MessageRepository.MessageEntry> entries =
        repo.findMessagesByteamID("teamA", 10);
    assertEquals(2, entries.size(), "Should retrieve both messages");

    MessageRepository.MessageEntry first = entries.get(0);
    assertEquals("u1", first.senderId);
    assertEquals("teamA", first.teamId);
    assertEquals("hello", first.content);
    Duration diff1 = Duration.between(m1.getTimestamp(), first.time).abs();
    assertTrue(
        diff1.compareTo(Duration.ofSeconds(1)) <= 0,
        ()
            -> String.format(
                "Timestamps differ by %d ms, which is more than 1 second",
                diff1.toMillis()));

    MessageRepository.MessageEntry second = entries.get(1);
    assertEquals("u2", second.senderId);
    assertEquals("teamA", second.teamId);
    assertEquals("world", second.content);
    Duration diff2 = Duration.between(m2.getTimestamp(), second.time).abs();
    assertTrue(
        diff2.compareTo(Duration.ofSeconds(1)) <= 0,
        ()
            -> String.format(
                "Timestamps differ by %d ms, which is more than 1 second",
                diff2.toMillis()));
  }

  @Test
  void testFindWithLimit() {
    // insert 5 messages for teamB
    for (int i = 1; i <= 5; i++) {
      repo.save(new Message("id" + i, new User("u" + i, "User" + i, "pwd"),
                            "teamB", "msg" + i, MessageType.NORMAL));
    }

    List<MessageRepository.MessageEntry> limited =
        repo.findMessagesByteamID("teamB", 3);
    assertEquals(3, limited.size(), "Should respect the LIMIT clause");
  }

  @Test
  void testFindNonexistentTeamReturnsEmpty() {
    List<MessageRepository.MessageEntry> none =
        repo.findMessagesByteamID("no-such-team", 5);
    assertTrue(none.isEmpty(), "No messages for unknown team");
  }
}
