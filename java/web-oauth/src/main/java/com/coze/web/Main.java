package com.coze.web;

import com.coze.openapi.client.auth.LoadAuthConfig;
import com.coze.openapi.service.auth.WebOAuthClient;
import com.coze.web.config.AppConfig;
import com.coze.web.server.TokenServer;

public class Main {
  private static final int PORT = 8080;
  private static final String configFilePath = "coze_oauth_config.json";

  public static void main(String[] args) throws Exception {
    TokenServer server = null;
    try {
      // 加载配置
      AppConfig config = AppConfig.load();

      // 初始化 WEB OAuth 客户端
      WebOAuthClient oauth = WebOAuthClient.loadFromConfig(new LoadAuthConfig(configFilePath));

      // 启动服务器
      server = new TokenServer(oauth, config);
      server.start(PORT);

      // 保持主线程运行
      Thread.currentThread().join();
    } finally {
      server.stop();
    }
  }
}
