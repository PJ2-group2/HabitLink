package com.habit.server.controller;

import com.habit.server.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 認証関連APIのコントローラ
 */
public class AuthController {
  private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  // ------------------------------------------------------------------------------
  // GetHander Methods

  public HttpHandler getLoginHandler() { return this.new LoginHandler(); }

  public HttpHandler getRegisterHandler() { return this.new RegisterHandler(); }

  // ------------------------------------------------------------------------------
  // Hander implementation classes

  // --- ログインAPI ---
  class LoginHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (!"POST".equals(exchange.getRequestMethod())) {
        String response = "POSTメソッドのみ対応";
        exchange.sendResponseHeaders(405, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
        return;
      }
      byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
      String bodyStr =
          (bodyBytes != null)
              ? new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8)
              : "";
      String response;
      String username = null, password = null;
      if (bodyStr != null && !bodyStr.isEmpty()) {
        String[] params = bodyStr.split("&");
        for (String param : params) {
          String[] kv = param.split("=", 2);
          if (kv.length < 2)
            continue;
          if (kv[0].equals("username"))
            username = java.net.URLDecoder.decode(kv[1], "UTF-8");
          if (kv[0].equals("password"))
            password = java.net.URLDecoder.decode(kv[1], "UTF-8");
        }
      }
      if (username != null && password != null) {
        String sessionId =
            authService.loginAndCreateSession(username, password);
        if (sessionId != null) {
          var user = authService.getUserBySession(sessionId);
          if (user != null) {
            logger.info(
                "ログインユーザ情報: userId={} , username={} , sabotagePoints={} , joinedTeamIds={}",
                user.getUserId(),
                user.getUsername(),
                user.getSabotagePoints(),
                user.getJoinedTeamIds());
          }
          response = "ログイン成功\nSESSION_ID:" + sessionId;
        } else {
          response = "ユーザ名またはパスワードが間違っています";
        }
      } else {
        response = "パラメータが不正です";
      }
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  // --- 新規登録API ---
  class RegisterHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (!"POST".equals(exchange.getRequestMethod())) {
        String response = "POSTメソッドのみ対応";
        exchange.sendResponseHeaders(405, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
        return;
      }
      byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
      String bodyStr =
          (bodyBytes != null)
              ? new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8)
              : "";
      String response;
      String username = null, password = null;
      if (bodyStr != null && !bodyStr.isEmpty()) {
        String[] params = bodyStr.split("&");
        for (String param : params) {
          String[] kv = param.split("=", 2);
          if (kv.length < 2)
            continue;
          if (kv[0].equals("username"))
            username = java.net.URLDecoder.decode(kv[1], "UTF-8");
          if (kv[0].equals("password"))
            password = java.net.URLDecoder.decode(kv[1], "UTF-8");
        }
      }
      if (username != null && password != null) {
        String sessionId =
            authService.registerAndCreateSession(username, password);
        if (sessionId != null) {
          var user = authService.getUserBySession(sessionId);
          if (user != null) {
            logger.info(
                "新規登録ユーザ情報: userId={} , username={} , sabotagePoints={} , joinedTeamIds={}",
                user.getUserId(),
                user.getUsername(),
                user.getSabotagePoints(),
                user.getJoinedTeamIds());
          }
          response = "登録成功\nSESSION_ID:" + sessionId;
        } else {
          response = "そのユーザー名は既に使用されています";
        }
      } else {
        response = "パラメータが不正です";
      }
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }
}
