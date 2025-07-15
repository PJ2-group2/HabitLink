package com.habit.domain;

import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * ユーザー情報を管理するクラス。
 */
/**
 * ユーザー情報を管理するクラス。
 * 参加済みチーム情報も保持する。
 */
public class User {
  private String userId;
  private String username;
  private String hashedPassword;
  private int sabotagePoints;
  /** 参加済みチームIDのリスト */
  private java.util.ArrayList<String> joinedTeamIds;

  public User(String userId, String username, String hashedPassword) {
    this.userId = userId;
    this.username = username;
    this.hashedPassword = hashedPassword;
    this.sabotagePoints = 25; // 初期サボりポイントは25
    this.joinedTeamIds = new java.util.ArrayList<>();
  }

  public void addSabotagePoints(int points) {
    this.sabotagePoints += points;
    if (this.sabotagePoints > 50) {
        this.sabotagePoints = 50;
    } else if (this.sabotagePoints < 0) {
        this.sabotagePoints = 0;
    }
  }

  public int getSabotagePoints() {
        return sabotagePoints;
    }

    public void setSabotagePoints(int sabotagePoints) {
        this.sabotagePoints = sabotagePoints;
    }


  public String getUserId() { return userId; }

  public String getUsername() { return username; }

  public void setHashedPassword(String newHashedPassword) {
    this.hashedPassword = newHashedPassword;
  }

  public String getPassword() { return hashedPassword; }

  /** 参加済みチームID一覧を取得 */
  public java.util.ArrayList<String> getJoinedTeamIds() {
    return joinedTeamIds;
  }

  /** チームIDを追加 */
  public void addJoinedTeamId(String teamId) {
    if (!joinedTeamIds.contains(teamId)) {
      joinedTeamIds.add(teamId);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof User))
      return false;
    User user = (User)o;
    return sabotagePoints == user.sabotagePoints &&
        Objects.equals(userId, user.userId) &&
        Objects.equals(username, user.username) &&
        Objects.equals(hashedPassword, user.hashedPassword) &&
        Objects.equals(joinedTeamIds, user.joinedTeamIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, username, hashedPassword, sabotagePoints,
                        joinedTeamIds);
  }

  /**
   * このオブジェクトを JSONObject に変換します。
   */
  public JSONObject toJson() {
    JSONObject json = new JSONObject();
    json.put("userId", userId);
    json.put("username", username);
    json.put("hashedPassword", hashedPassword);
    json.put("sabotagePoints", sabotagePoints);

    // List<String> を JSONArray に変換
    JSONArray arr = new JSONArray(joinedTeamIds);
    json.put("joinedTeamIds", arr);
    return json;
  }

  /**
   * JSONObject から User オブジェクトを復元します。
   */
  public static User fromJson(JSONObject json) {
    String userId = json.getString("userId");
    String username = json.getString("username");
    String hashedPassword = json.getString("hashedPassword");
    User user = new User(userId, username, hashedPassword);

    // sabotagePoints をセット
    user.sabotagePoints = json.optInt("sabotagePoints", 0);

    // joinedTeamIds を復元
    JSONArray arr = json.optJSONArray("joinedTeamIds");
    if (arr != null) {
      for (int i = 0; i < arr.length(); i++) {
        user.joinedTeamIds.add(arr.getString(i));
      }
    }

    return user;
  }
}
