package com.coze.device.server;

import com.coze.device.model.TokenResponse;
import com.coze.openapi.client.auth.OAuthToken;
import com.coze.openapi.service.auth.DeviceOAuthClient;
import io.javalin.Javalin;

public class TokenServer {
    private final DeviceOAuthClient oauthClient;
    private Javalin app;
    private String refreshToken;

    public TokenServer(DeviceOAuthClient oauthClient, String refreshToken) {
        this.oauthClient = oauthClient;
        this.refreshToken = refreshToken;
    }

    public void start(int port) {
        app = Javalin.create()
                .get("/refresh_token", ctx -> {
                    try{
                        OAuthToken tokenResp = oauthClient.refreshToken(this.refreshToken);
                        this.refreshToken = tokenResp.getRefreshToken();
                        ctx.json(TokenResponse.convertToTokenResponse(tokenResp));
                    }catch (Exception e){
                        throw e;
                    }

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
        if (oauthClient != null) {
            oauthClient.shutdownExecutor();
        }
    }
}
