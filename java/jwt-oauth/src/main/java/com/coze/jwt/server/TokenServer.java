package com.coze.jwt.server;

import com.coze.jwt.config.AppConfig;
import com.coze.jwt.model.TokenResponse;
import com.coze.openapi.client.auth.OAuthToken;


import com.coze.openapi.service.auth.JWTOAuthClient;
import io.javalin.Javalin;

public class TokenServer {
    private final JWTOAuthClient oauthClient;
    private Javalin app;

    public TokenServer(JWTOAuthClient oauthClient, AppConfig appConfig) {
        this.oauthClient = oauthClient;
    }

    public void start(int port) {
        app = Javalin.create()
                .get("/token", ctx -> {
                    OAuthToken tokenResp = oauthClient.getAccessToken();
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