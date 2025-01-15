package com.coze.web.server;

import com.coze.common.config.AppConfig;
import com.coze.common.model.TokenResponse;
import com.coze.openapi.client.auth.OAuthToken;
import com.coze.openapi.service.auth.WebOAuthClient;
import io.javalin.Javalin;

public class TokenServer {
    private final WebOAuthClient oauthClient;
    private final AppConfig appConfig;
    private String refreshToken;
    private Javalin app;

    public TokenServer(WebOAuthClient oauthClient, AppConfig appConfig) {
        this.oauthClient = oauthClient;
        this.appConfig = appConfig;
    }

    public void start(int port) {
        app = Javalin.create()
            .get("/refresh_token", ctx -> {
                OAuthToken tokenResp = oauthClient.refreshToken(refreshToken);
                this.refreshToken = tokenResp.getRefreshToken();
                ctx.json(TokenResponse.convertToTokenResponse(tokenResp));
            })
            .get("/callback", ctx -> {
                String code = ctx.queryParam("code");
                if (code == null) {
                    ctx.status(400).result("Missing code parameter");
                    return;
                }
                OAuthToken tokenResp = oauthClient.getAccessToken(code, appConfig.getRedirectUri());
                this.refreshToken = tokenResp.getRefreshToken();
                ctx.json(TokenResponse.convertToTokenResponse(tokenResp));
            })
            .exception(Exception.class, (e, ctx) -> {
                ctx.status(500).result("Error getting access token: " + e.getMessage());
            })
            .start(port);
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }
} 