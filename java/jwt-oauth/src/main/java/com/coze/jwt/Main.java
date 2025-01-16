package com.coze.jwt;

import com.coze.jwt.config.AppConfig;
import com.coze.jwt.server.TokenServer;
import com.coze.openapi.service.auth.JWTOAuthClient;

public class Main {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        TokenServer server = null;
        try {
            // 加载配置
            AppConfig config = AppConfig.load(System.getenv("JWT_OAUTH_CONFIG_PATH"));

            // 初始化 JWT OAuth 客户端
            JWTOAuthClient oauth = createOAuthClient(config);

            // 启动服务器
            server = new TokenServer(oauth, config);
            server.start(PORT);

            // 保持主线程运行
            Thread.currentThread().join();
        }finally {
            if (server != null) {
                server.stop();
            }
        }

    }

    private static JWTOAuthClient createOAuthClient(AppConfig config) throws Exception {
        return new JWTOAuthClient.JWTOAuthBuilder()
                .privateKey(config.getPrivateKey())
                .publicKey(config.getPublicKeyId())
                .clientID(config.getClientId())
                .baseURL(config.getCozeApiBase())
                .build();
    }


}