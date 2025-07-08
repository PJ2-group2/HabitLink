package com.habit.client.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * アプリケーションの設定を管理するクラス。
 * プロパティファイルからサーバアドレスを読み込み、クライアントに提供する。
 *
 */
public class Config {
  private static final Logger logger = LoggerFactory.getLogger(Config.class);

  private static final Properties properties = new Properties();

  static {
    try (InputStream input = Config.class.getClassLoader().getResourceAsStream(
             "application.properties")) {
      if (input == null) {
        logger.error("Sorry, unable to find application.properties");
      } else {
        properties.load(input);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public static String getServerUrl() {
    String server_url = (String)properties.getProperty("server.url");
    logger.debug("server url: {}", server_url);
    return server_url;
  }

  public static boolean getIsDebug() {
    String debug_key = (String)properties.getProperty("debug");
    debug_key = debug_key.trim();
    logger.debug("debug key: {}", debug_key);
    return debug_key.equals("1");
  }
}
