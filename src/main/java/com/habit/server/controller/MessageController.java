package com.habit.server.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.habit.server.repository.MessageRepository;
import com.habit.server.repository.UserRepository;
import com.habit.domain.Message;
import com.habit.domain.User;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class MessageController {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageController(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    public HttpHandler getGetChatLogHandler() {
        return new GetChatLogHandler();
    }

    private class GetChatLogHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String query = exchange.getRequestURI().getQuery();
                String teamID = null;
                int limit = 50;
                if (query != null) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("teamID=")) teamID = java.net.URLDecoder.decode(param.substring(7), "UTF-8");
                        if (param.startsWith("limit=")) {
                            try {
                                limit = Integer.parseInt(param.substring(6));
                            } catch (Exception ignored) {}
                        }
                    }
                }
                String response;
                if (teamID == null) {
                    response = "[]";
                } else {
                    List<Message> messages = messageRepository.findMessagesByteamID(teamID, limit);
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < messages.size(); i++) {
                        Message msg = messages.get(i);
                        User user = userRepository.findById(msg.getSenderId());
                        String username = (user != null && user.getUsername() != null && !user.getUsername().isEmpty())
                                ? user.getUsername()
                                : "unknown";
                        String json = String.format("{\"senderId\":\"%s\",\"username\":\"%s\",\"content\":\"%s\"}",
                                msg.getSenderId().replace("\"", "\\\""),
                                username.replace("\"", "\\\""),
                                msg.getContent().replace("\"", "\\\""));
                        sb.append(json);
                        if (i < messages.size() - 1) sb.append(",");
                    }
                    sb.append("]");
                    response = sb.toString();
                }
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
                String err = "[]";
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, err.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(err.getBytes());
                os.close();
            }
        }
    }
    public HttpHandler getSendChatMessageHandler() {
        return new SendChatMessageHandler();
    }

    private class SendChatMessageHandler implements HttpHandler {
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
            String teamID = null, senderId = null, content = null;
            if (bodyStr != null && !bodyStr.isEmpty()) {
                String[] params = bodyStr.split("&");
                for (String param : params) {
                    String[] kv = param.split("=", 2);
                    if (kv.length < 2) continue;
                    switch (kv[0]) {
                        case "teamID": teamID = java.net.URLDecoder.decode(kv[1], "UTF-8"); break;
                        case "senderId": senderId = java.net.URLDecoder.decode(kv[1], "UTF-8"); break;
                        case "content": content = java.net.URLDecoder.decode(kv[1], "UTF-8"); break;
                    }
                }
            }
            if (teamID == null || senderId == null || content == null) {
                response = "パラメータが不正です";
                exchange.sendResponseHeaders(400, response.getBytes().length);
            } else {
                String username = senderId;
                try {
                    User user = userRepository.findById(senderId);
                    if (user != null) {
                        String uname = user.getUsername();
                        if (uname != null && !uname.trim().isEmpty()) {
                            username = uname;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                Message message = new Message(content, senderId, teamID, content, com.habit.domain.MessageType.NORMAL);
                messageRepository.save(message);

                System.out.println("[チャット] teamID=" + teamID + ", senderId=" + senderId + ", username=" + username + ", content=" + content);
                response = "チャット送信成功";
                exchange.sendResponseHeaders(200, response.getBytes().length);
            }
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}