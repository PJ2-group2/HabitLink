package com.habit.client.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * アプリケーションの設定を管理するクラス。
 * プロパティファイルからサーバアドレスを読み込み、クライアントに提供する。
 * 
 */
public class Config {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find application.properties");
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String getServerUrl() {
        return properties.getProperty("server.url");
    }
}
