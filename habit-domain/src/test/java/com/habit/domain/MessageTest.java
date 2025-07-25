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
    System.out.println("[START] testToJsonStructure / メッセージのJSON構造テスト開始");
    User sender1 = new User("id", "usrname", "pwd");
    Message msg =
        new Message("id1", sender1, "team1", "hello world", MessageType.NORMAL);
    JSONObject json = msg.toJson();

    System.out.println("メッセージID: " + json.getString("messageId"));
    System.out.println("送信者: " + json.getJSONObject("sender"));
    System.out.println("チームID: " + json.getString("teamID"));
    System.out.println("内容: " + json.getString("content"));
    System.out.println("タイプ: " + json.getString("type"));
    System.out.println("タイムスタンプ: " + json.getString("timestamp"));

    // Verify all fields are present and correctly serialized
    assertEquals("id1", json.getString("messageId"));
    assertEquals(sender1, User.fromJson(json.getJSONObject("sender")));
    assertEquals("team1", json.getString("teamID"));
    assertEquals("hello world", json.getString("content"));
    assertEquals(MessageType.NORMAL.name(), json.getString("type"));
    assertNotNull(json.getString("timestamp"));

    // Timestamp should be a valid ISO-8601 string and not in the future
    LocalDateTime parsed = LocalDateTime.parse(json.getString("timestamp"));
    assertFalse(parsed.isAfter(LocalDateTime.now()));
    System.out.println("[SUCCESS] testToJsonStructure / メッセージのJSON構造テスト成功");
  }

  @Test
  void testFromJsonReconstructsMessage() {
    System.out.println("[START] testFromJsonReconstructsMessage / JSONからMessage復元テスト開始");
    User sender2 = new User("id", "usrname", "pwd");
    LocalDateTime customTime = LocalDateTime.of(2022, 1, 1, 10, 30, 45);
    JSONObject json = new JSONObject();
    json.put("messageId", "id2");
    json.put("sender", sender2.toJson());
    json.put("teamID", "team2");
    json.put("content", "test content");
    json.put("timestamp", customTime.toString());
    json.put("type", MessageType.PENALTY_REPORT.name());

    System.out.println("入力JSON: " + json);

    Message msg = Message.fromJson(json);

    System.out.println("復元されたMessage: " + msg);

    assertEquals("id2", msg.getMessageId());
    assertEquals(sender2, msg.getSender());
    assertEquals("team2", msg.getTeamID());
    assertEquals("test content", msg.getContent());
    assertEquals(customTime, msg.getTimestamp());
    assertEquals(MessageType.PENALTY_REPORT, msg.getType());
    System.out.println("[SUCCESS] testFromJsonReconstructsMessage / JSONからMessage復元テスト成功");
  }

  @Test
  void testRoundTripSerialization() {
    System.out.println("[START] testRoundTripSerialization / シリアライズ・デシリアライズ往復テスト開始");
    User sender3 = new User("id", "usrname", "pwd");
    Message original = new Message("id3", sender3, "team3", "roundtrip test",
                                   MessageType.NORMAL);
    JSONObject json = original.toJson();
    System.out.println("シリアライズされたJSON: " + json);
    Message recreated = Message.fromJson(json);
    System.out.println("復元されたMessage: " + recreated);

    // All fields should round-trip exactly
    assertEquals(original.getMessageId(), recreated.getMessageId());
    assertEquals(original.getSender(), recreated.getSender());
    assertEquals(original.getTeamID(), recreated.getTeamID());
    assertEquals(original.getContent(), recreated.getContent());
    assertEquals(original.getType(), recreated.getType());
    assertEquals(original.getTimestamp(), recreated.getTimestamp());
    System.out.println("[SUCCESS] testRoundTripSerialization / シリアライズ・デシリアライズ往復テスト成功");
  }
}
