package com.coze.web.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.coze.openapi.client.auth.OAuthToken;
import com.coze.openapi.service.auth.WebOAuthClient;
import com.coze.web.config.AppConfig;
import com.coze.web.model.TokenResponse;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

public class TokenServer {
    private final WebOAuthClient oauthClient;
    private final AppConfig appConfig;
    private Javalin app;

    public TokenServer(WebOAuthClient oauthClient, AppConfig appConfig) {
        this.oauthClient = oauthClient;
        this.appConfig = appConfig;
    }

    public String readFromResources(String fileName) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            try (InputStream inputStream = classLoader.getResourceAsStream(fileName)) {
                if (inputStream == null) {
                    throw new IllegalArgumentException("file not found: " + fileName);
                }
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("read file failed: " + fileName, e);
        }
    }

    public void start(int port) {
        app = Javalin.create(config -> {
                    config.addStaticFiles(staticFiles -> {
                        staticFiles.directory = "/assets";
                        staticFiles.location = Location.CLASSPATH;
                        staticFiles.hostedPath = "/assets";
                    });
                    config.addStaticFiles(staticFiles -> {
                        staticFiles.directory = "/websites";
                        staticFiles.location = Location.CLASSPATH;
                    });
                })
                .get("/", ctx -> {
                Map<String, String> model = Map.of(
                    "client_type", appConfig.getClientType(),
                    "client_id", appConfig.getClientId()
                );
                String html = null;
                try{
                    html = formatHtml(readFromResources("websites/index.html"), model);
                }catch (Exception e){
                    e.printStackTrace();
                    throw e;
                }
                ctx.contentType("text/html");
                ctx.html(html);
                   
                })
                .get("/callback", ctx -> {
                    String code = ctx.queryParam("code");
                    if (code == null) {
                        throw new RuntimeException("Authorization failed: No authorization code received.");
                    }
                    try{
                        OAuthToken tokenResp = oauthClient.getAccessToken(code, appConfig.getRedirectUri());
                        ctx.sessionAttribute(genTokenSessionKey(), tokenResp);
                        Map<String, String> model = Map.of(
                                "token_type", appConfig.getClientType(),
                                "access_token", tokenResp.getAccessToken(),
                                "refresh_token", tokenResp.getRefreshToken(),
                                "expires_in",String.format("%d (%s)",
                                        tokenResp.getExpiresIn(),
                                        timestampToDateTime(tokenResp.getExpiresIn())
                                )
                        );
                        String html = formatHtml(readFromResources("websites/callback.html"), model);
                        ctx.contentType("text/html");
                        ctx.result(html);

                    }catch (Exception e){
                        throw new RuntimeException("Authorization failed: " + e.getMessage());
                    }

                })
                .get("/login", ctx -> {
                    String url = oauthClient.getOAuthURL(appConfig.getRedirectUri(), "state");
                    ctx.redirect(url);
                })
                .post("/refresh_token", ctx -> {
                    OAuthToken oldToken = ctx.sessionAttribute(genTokenSessionKey());
                    if (oldToken == null) {
                        throw new RuntimeException("Authorization failed: No authorization code received.");
                    }
                    OAuthToken tokenResp = oauthClient.refreshToken(oldToken.getRefreshToken());
                    ctx.sessionAttribute(genTokenSessionKey(), tokenResp);
                    ctx.json(TokenResponse.convertToTokenResponse(tokenResp));
                })
                .exception(Exception.class, (e, ctx) -> {
                    Map<String, String> model = Map.of(
                            "error", e.getMessage()
                    );
                    String html = null;
                    try{
                        html = formatHtml(readFromResources("websites/error.html"), model);
                    }catch (Exception e1){
                        ctx.status(500).result("Error getting html: " + e.getMessage());
                        return;
                    }
                    ctx.contentType("text/html");
                    ctx.result(html);
                })
                .start(port);
    }

    private String formatHtml(String html, Map<String, String> model) {
        for (Map.Entry<String, String> entry : model.entrySet()) {
            html = html.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return html;
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

    public static String timestampToDateTime(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault()
        );
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String genTokenSessionKey() {
        return String.format("access_token_%s", appConfig.getClientId());
    }
} 