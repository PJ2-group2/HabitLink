package com.habit.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Message serialization and deserialization.
 */
public class MessageTest {
  @Test
  void testToJsonStructure() {
    Message msg = new Message("id1", "sender1", "team1", "hello world",
                              MessageType.NORMAL);
    JSONObject json = msg.toJson();

    // Verify all fields are present and correctly serialized
    assertEquals("id1", json.getString("messageId"));
    assertEquals("sender1", json.getString("senderId"));
    assertEquals("team1", json.getString("teamID"));
    assertEquals("hello world", json.getString("content"));
    assertEquals(MessageType.NORMAL.name(), json.getString("type"));
    assertNotNull(json.getString("timestamp"));

    // Timestamp should be a valid ISO-8601 string and not in the future
    LocalDateTime parsed = LocalDateTime.parse(json.getString("timestamp"));
    assertFalse(parsed.isAfter(LocalDateTime.now()));
  }

  @Test
  void testFromJsonReconstructsMessage() {
    LocalDateTime customTime = LocalDateTime.of(2022, 1, 1, 10, 30, 45);
    JSONObject json = new JSONObject();
    json.put("messageId", "id2");
    json.put("senderId", "sender2");
    json.put("teamID", "team2");
    json.put("content", "test content");
    json.put("timestamp", customTime.toString());
    json.put("type", MessageType.PENALTY_REPORT.name());

    Message msg = Message.fromJson(json);

    assertEquals("id2", msg.getMessageId());
    assertEquals("sender2", msg.getSenderId());
    assertEquals("team2", msg.getTeamID());
    assertEquals("test content", msg.getContent());
    assertEquals(customTime, msg.getTimestamp());
    assertEquals(MessageType.PENALTY_REPORT, msg.getType());
  }

  @Test
  void testRoundTripSerialization() {
    Message original = new Message("id3", "sender3", "team3", "roundtrip test",
                                   MessageType.NORMAL);
    JSONObject json = original.toJson();
    Message recreated = Message.fromJson(json);

    // All fields should round-trip exactly
    assertEquals(original.getMessageId(), recreated.getMessageId());
    assertEquals(original.getSenderId(), recreated.getSenderId());
    assertEquals(original.getTeamID(), recreated.getTeamID());
    assertEquals(original.getContent(), recreated.getContent());
    assertEquals(original.getType(), recreated.getType());
    assertEquals(original.getTimestamp(), recreated.getTimestamp());
  }
}
