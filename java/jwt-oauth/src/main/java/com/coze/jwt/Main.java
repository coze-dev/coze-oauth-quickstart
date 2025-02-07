package com.coze.jwt;

import com.coze.jwt.server.TokenServer;
import com.coze.openapi.client.auth.LoadAuthConfig;
import com.coze.openapi.client.auth.OAuthConfig;
import com.coze.openapi.service.auth.JWTOAuthClient;

public class Main {
  private static final String configFilePath = "coze_oauth_config.json";
  private static final int PORT = 8080;

  public static void main(String[] args) throws Exception {
    TokenServer server = null;
    try {
      // 加载配置
      OAuthConfig config = OAuthConfig.load(new LoadAuthConfig(configFilePath));

      // 初始化 JWT OAuth 客户端
      JWTOAuthClient oauth = JWTOAuthClient.loadFromConfig(new LoadAuthConfig(configFilePath));

      // 启动服务器
      server = new TokenServer(oauth, config);
      server.start(PORT);

      // 保持主线程运行
      Thread.currentThread().join();
    } finally {
      if (server != null) {
        server.stop();
      }
    }
  }
}
