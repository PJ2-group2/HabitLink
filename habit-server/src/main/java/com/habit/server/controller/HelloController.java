package com.habit.server.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;

/**
 * /hello エンドポイント用コントローラ
 */
public class HelloController implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response = "Hello, HTTP! サーバは動作中です。";
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}