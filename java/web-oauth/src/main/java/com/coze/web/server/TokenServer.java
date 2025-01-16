package com.coze.web.server;

import com.coze.openapi.client.auth.OAuthToken;
import com.coze.openapi.service.auth.WebOAuthClient;
import com.coze.web.config.AppConfig;
import com.coze.web.model.TokenResponse;
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
                .get("/", ctx -> {
                    ctx.html("<h1>Coze Web OAuth Quickstart</h1>\n" +
                            "<p><a href=\"/login\">Login</a></p>");
                })
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
                .get("/login", ctx -> {
                    String url = oauthClient.getOAuthURL(appConfig.getRedirectUri(), "state");
                    System.out.println(url);
                    ctx.redirect(url);
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