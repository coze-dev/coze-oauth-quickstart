package com.coze.pkce.server;

import com.coze.common.config.AppConfig;
import com.coze.common.model.TokenResponse;
import com.coze.openapi.client.auth.GetPKCEAuthURLResp;
import com.coze.openapi.client.auth.OAuthToken;
import com.coze.openapi.service.auth.PKCEOAuthClient;
import io.javalin.Javalin;

public class TokenServer {
    private final PKCEOAuthClient oauthClient;
    private final AppConfig appConfig;
    private String codeVerifier;
    private String refreshToken;
    private Javalin app;

    public TokenServer(PKCEOAuthClient oauthClient, AppConfig appConfig) {
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
                    OAuthToken tokenResp = oauthClient.getAccessToken(code, appConfig.getRedirectUri(), codeVerifier);
                    this.refreshToken = tokenResp.getRefreshToken();
                    ctx.json(TokenResponse.convertToTokenResponse(tokenResp));
                })
                .get("/login", ctx -> {
                    GetPKCEAuthURLResp resp = oauthClient.genOAuthURL(appConfig.getRedirectUri(), "state");
                    this.codeVerifier = resp.getCodeVerifier();
                    ctx.redirect(resp.getAuthorizationURL());
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