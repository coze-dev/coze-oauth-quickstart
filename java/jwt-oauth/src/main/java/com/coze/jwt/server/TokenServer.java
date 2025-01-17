package com.coze.jwt.server;

import com.coze.jwt.config.AppConfig;
import com.coze.jwt.model.TokenResponse;
import com.coze.openapi.client.auth.OAuthToken;


import com.coze.openapi.service.auth.JWTOAuthClient;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class TokenServer {
    private final JWTOAuthClient oauthClient;
    private Javalin app;
    private final AppConfig appConfig;

    public TokenServer(JWTOAuthClient oauthClient, AppConfig appConfig) {
        this.oauthClient = oauthClient;
        this.appConfig = appConfig;
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
                .get("/refresh_token", ctx -> {
                    OAuthToken tokenResp = oauthClient.getAccessToken();
                    ctx.sessionAttribute(genTokenSessionKey(), tokenResp);
                    ctx.json(TokenResponse.convertToTokenResponse(tokenResp));
                })
                .get("/callback", ctx -> {
                    try{
                        OAuthToken tokenResp = oauthClient.getAccessToken();
                        ctx.sessionAttribute(genTokenSessionKey(), tokenResp);
                        Map<String, String> model = Map.of(
                                "token_type", appConfig.getClientType(),
                                "access_token", tokenResp.getAccessToken(),
                                "refresh_token", "",
                                "expires_in",String.format("%d (%s)",
                                        tokenResp.getExpiresIn(),
                                        timestampToDateTime(tokenResp.getExpiresIn())
                                )
                        );
                        String html = formatHtml(readFromResources("websites/callback.html"), model);
                        ctx.contentType("text/html");
                        ctx.result(html);

                    }catch (Exception e){
                        e.printStackTrace();
                        throw new RuntimeException("Authorization failed: " + e.getMessage());
                    }

                })
                .get("/login", ctx -> {
                    ctx.redirect("/callback");
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

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

    private String formatHtml(String html, Map<String, String> model) {
        for (Map.Entry<String, String> entry : model.entrySet()) {
            html = html.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return html;
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