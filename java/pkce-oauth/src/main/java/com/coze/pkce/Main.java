package com.coze.pkce;

import com.coze.common.config.AppConfig;
import com.coze.common.model.TokenResponse;
import com.coze.common.utils.Client;
import com.coze.openapi.service.auth.PKCEOAuthClient;
import com.coze.openapi.service.auth.WebOAuthClient;
import com.coze.pkce.server.TokenServer;

public class Main {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        TokenServer server = null;
        try{
            // 加载配置
            AppConfig config = AppConfig.load(System.getenv("PKCE_OAUTH_CONFIG_PATH"));

            // 初始化 WEB OAuth 客户端
            PKCEOAuthClient oauth = new PKCEOAuthClient.PKCEOAuthBuilder()
                    .clientID(config.getClientId())
                    .baseURL(config.getCozeApiBase())
                    .build();

            // 启动服务器
            server = new TokenServer(oauth, config);
            server.start(PORT);
            // 保持主线程运行
            Thread.currentThread().join();
        }finally {
            server.stop();
        }

    }

}