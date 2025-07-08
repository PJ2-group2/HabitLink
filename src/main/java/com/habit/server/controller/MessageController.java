package com.habit.server.controller;

import com.habit.domain.Message;
import com.habit.domain.MessageType;
import com.habit.domain.User;
import com.habit.server.repository.MessageRepository;
import com.habit.server.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageController {
  private static final Logger logger = LoggerFactory.getLogger(MessageController.class);
  private final MessageRepository messageRepository;
  private final UserRepository userRepository;

  public MessageController(MessageRepository messageRepository,
                           UserRepository userRepository) {
    this.messageRepository = messageRepository;
    this.userRepository = userRepository;
  }

  public HttpHandler getGetChatLogHandler() {
    return this.new GetChatLogHandler();
  }

  public HttpHandler getSendChatMessageHandler() {
    return this.new SendChatMessageHandler();
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
            if (param.startsWith("teamID="))
              teamID = java.net.URLDecoder.decode(param.substring(7), "UTF-8");
            if (param.startsWith("limit=")) {
              try {
                limit = Integer.parseInt(param.substring(6));
              } catch (Exception ignored) {
              }
            }
          }
        }
        JSONArray responseArray = new JSONArray();
        if (teamID != null) {
          List<MessageRepository.MessageEntry> messages =
              messageRepository.findMessagesByteamID(teamID, limit);
          for (var entry : messages) {
            User sender = userRepository.findById(entry.senderId);
            // senderがnullの場合（例：システムユーザー）のための代替処理
            if (sender == null) {
                // システムメッセージ用の代替ユーザーを作成
                sender = new User(entry.senderId, "System", "");
            }
            Message msg = new Message(entry.id, sender, entry.teamId,
                                      entry.content, MessageType.NORMAL);
            msg.setTimeStamp(entry.time);
            responseArray.put(msg.toJson());
          }
        }

        String responseStr = responseArray.toString();
        exchange.getResponseHeaders().set("Content-Type",
                                          "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, responseStr.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseStr.getBytes());
        os.close();
      } catch (Exception e) {
        e.printStackTrace();
        String err = "[]";
        exchange.getResponseHeaders().set("Content-Type",
                                          "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, err.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(err.getBytes());
        os.close();
      }
    }
  }

  private class SendChatMessageHandler implements HttpHandler {
    public void respond(HttpExchange exchange, int code, String response)
        throws IOException {
      exchange.sendResponseHeaders(code, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (!"POST".equals(exchange.getRequestMethod())) {
        respond(exchange, 405, "POSTメソッドのみ対応");
        return;
      }
      byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
      String bodyStr =
          (bodyBytes != null)
              ? new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8)
              : "";
      String response;
      String teamID = null, content = null, senderId = null;
      User sender = null;

      if (bodyStr != null && !bodyStr.isEmpty()) {
        String[] params = bodyStr.split("&");
        for (String param : params) {
          String[] kv = param.split("=", 2);
          if (kv.length < 2)
            continue;
          switch (kv[0]) {
          case "teamID":
            teamID = java.net.URLDecoder.decode(kv[1], "UTF-8");
            break;
          case "senderId":
            senderId = java.net.URLDecoder.decode(kv[1], "UTF-8");
            break;
          case "content":
            content = java.net.URLDecoder.decode(kv[1], "UTF-8");
            break;
          }
        }
      }
      if (teamID == null || senderId == null || content == null) {
        respond(exchange, 400, "パラメータが不正です");
        return;
      }
      try {
        sender = userRepository.findById(senderId);
        if (sender == null) {
          respond(exchange, 400, "送信者が存在しません");
          return;
        }
      } catch (Exception ex) {
        ex.printStackTrace();
        respond(exchange, 500, "サーバ内部エラー");
        return;
      }

      Message message = new Message("not set yet", sender, teamID, content,
                                    com.habit.domain.MessageType.NORMAL);
      messageRepository.save(message);

      logger.info(
          "[チャット] teamID={} , senderId={} , username={} , content={}",
          teamID,
          senderId,
          sender.getUsername(),
          content);

      respond(exchange, 200, "チャット送信成功");
    }
  }
}
