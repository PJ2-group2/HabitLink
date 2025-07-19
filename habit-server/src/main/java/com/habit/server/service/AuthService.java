package com.habit.server.service;

import com.habit.domain.User;
import com.habit.server.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
  private UserRepository userRepository;
  // セッションIDとユーザーIDのマッピング
  private final Map<String, String> sessionMap = new ConcurrentHashMap<>();

  public AuthService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * ログイン認証し、成功時はセッションIDを発行して返す
   */
  public String loginAndCreateSession(String username, String password) {
    User user = userRepository.findByUsername(username);
    if (user != null && BCrypt.checkpw(password, user.getPassword())) {
      String sessionId = UUID.randomUUID().toString();
      sessionMap.put(sessionId, user.getUserId());
      return sessionId;
    }
    return null;
  }

  /**
   * セッションIDからユーザーを取得
   */
  public User getUserBySession(String sessionId) {
    String userId = sessionMap.get(sessionId);
    if (userId == null)
      return null;
    return userRepository.findById(userId);
  }

  /**
   * 新規登録し、セッションIDを発行して返す
   */
  public String registerAndCreateSession(String username, String password) {
    if (userRepository.findByUsername(username) != null) {
      return null; // ユーザー名が既に存在する場合はnullを返す
    }
    // パスワードをハッシュ化
    String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
    User user =
        new User(java.util.UUID.randomUUID().toString(), username, hashedPassword);
    userRepository.save(user);
    String sessionId = UUID.randomUUID().toString();
    sessionMap.put(sessionId, user.getUserId());
    return sessionId;
  }

  /**
   * ログアウト（セッション破棄）
   */
  public void logout(String sessionId) { sessionMap.remove(sessionId); }
}
