package com.coze.web;

import com.coze.common.config.AppConfig;
import com.coze.common.model.TokenResponse;
import com.coze.common.utils.Client;
import com.coze.openapi.service.auth.WebOAuthClient;
import com.coze.web.server.TokenServer;

public class Main {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        TokenServer server = null;
        try{
            // 加载配置
            AppConfig config = AppConfig.load(System.getenv("WEB_OAUTH_CONFIG_PATH"));

            // 初始化 WEB OAuth 客户端
            WebOAuthClient oauth = createOAuthClient(config);

            // 启动服务器
            server = new TokenServer(oauth, config);
            server.start(PORT);
            printServerInfo(config);

            String verifyURL = oauth.getOAuthURL(config.getRedirectUri(), "state");
            System.out.println("Please visit the verification URL to complete the authorization: " + verifyURL);

            // 保持主线程运行
            Thread.currentThread().join();
        }finally {
            server.stop();
        }

    }

    private static WebOAuthClient createOAuthClient(AppConfig config) throws Exception {
        return new WebOAuthClient.WebOAuthBuilder()
                .clientID(config.getClientId())
                .clientSecret(config.getClientSecret())
                .baseURL(config.getCozeApiBase())
                .build();
    }

    private static void printServerInfo(AppConfig config) {
        System.out.printf("\nServer starting on %s:%d... (API Base: %s, Client Type: %s, Client ID: %s)%n",
                HOST, PORT, config.getCozeApiBase(), "web oauth client", config.getClientId());
    }

    private static void testTokenEndpoint() throws Exception {
        // 等待服务器启动
        Thread.sleep(1000);

        System.out.println("\nMaking request to /token endpoint to get access token...");
        
        // 发送请求
        TokenResponse tokenResp = Client.getToken(String.format("http://%s:%d/refresh_token", HOST, PORT));
        
        // 打印结果
        tokenResp.print();
        
        System.out.println("\nServer is still running. You can get a new access token anytime using: " +
                String.format("curl http://%s:%d/refresh_token", HOST, PORT));
    }

}