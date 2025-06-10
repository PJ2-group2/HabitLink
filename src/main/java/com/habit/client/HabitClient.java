package com.habit.client;
// Javaの標準HTTPクライアントを使ったサンプルクライアント
// サーバにHTTPリクエストを送り、応答を表示します

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class HabitClient {
    public static void main(String[] args) throws Exception {
        // HTTPクライアントを作成
        HttpClient client = HttpClient.newHttpClient();
        // サーバの/helloエンドポイントにGETリクエストを送信
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/hello"))
            .build();
        // サーバからの応答を受け取る
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        // 応答内容を表示
        System.out.println("サーバからの応答: " + response.body());
    }
}
