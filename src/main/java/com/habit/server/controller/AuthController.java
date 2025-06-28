package com.habit.server.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.habit.server.AuthService;
import com.habit.server.UserRepository;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 認証関連APIのコントローラ
 */
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    public HttpHandler getLoginHandler() {
        return new LoginHandler();
    }

    public HttpHandler getRegisterHandler() {
        return new RegisterHandler();
    }

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
            String bodyStr = (bodyBytes != null) ? new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8) : "";
            String response;
            String username = null, password = null;
            if (bodyStr != null && !bodyStr.isEmpty()) {
                String[] params = bodyStr.split("&");
                for (String param : params) {
                    String[] kv = param.split("=", 2);
                    if (kv.length < 2) continue;
                    if (kv[0].equals("username")) username = java.net.URLDecoder.decode(kv[1], "UTF-8");
                    if (kv[0].equals("password")) password = java.net.URLDecoder.decode(kv[1], "UTF-8");
                }
            }
            if (username != null && password != null) {
                String sessionId = authService.loginAndCreateSession(username, password);
                if (sessionId != null) {
                    var user = authService.getUserBySession(sessionId);
                    if (user != null) {
                        System.out.println("ログインユーザ情報: userId=" + user.getUserId() +
                            ", username=" + user.getUsername() +
                            ", sabotagePoints=" + user.getSabotagePoints() +
                            ", joinedTeamIds=" + user.getJoinedTeamIds());
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
            String bodyStr = (bodyBytes != null) ? new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8) : "";
            String response;
            String username = null, password = null;
            if (bodyStr != null && !bodyStr.isEmpty()) {
                String[] params = bodyStr.split("&");
                for (String param : params) {
                    String[] kv = param.split("=", 2);
                    if (kv.length < 2) continue;
                    if (kv[0].equals("username")) username = java.net.URLDecoder.decode(kv[1], "UTF-8");
                    if (kv[0].equals("password")) password = java.net.URLDecoder.decode(kv[1], "UTF-8");
                }
            }
            if (username != null && password != null) {
                String sessionId = authService.registerAndCreateSession(username, password);
                if (sessionId != null) {
                    var user = authService.getUserBySession(sessionId);
                    if (user != null) {
                        System.out.println("新規登録ユーザ情報: userId=" + user.getUserId() +
                            ", username=" + user.getUsername() +
                            ", sabotagePoints=" + user.getSabotagePoints() +
                            ", joinedTeamIds=" + user.getJoinedTeamIds());
                    }
                    response = "登録成功\nSESSION_ID:" + sessionId;
                } else {
                    response = "登録失敗";
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