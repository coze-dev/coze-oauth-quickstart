package com.coze.pkce.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.coze.openapi.client.auth.GetPKCEAuthURLResp;
import com.coze.openapi.client.auth.OAuthToken;
import com.coze.openapi.service.auth.PKCEOAuthClient;
import com.coze.pkce.config.AppConfig;
import com.coze.pkce.model.TokenResponse;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

public class TokenServer {
  private final PKCEOAuthClient oauthClient;
  private final AppConfig appConfig;
  private Javalin app;

  public TokenServer(PKCEOAuthClient oauthClient, AppConfig appConfig) {
    this.oauthClient = oauthClient;
    this.appConfig = appConfig;
  }

  public void start(int port) {
    app =
        Javalin.create(
                config -> {
                  config.addStaticFiles(
                      staticFiles -> {
                        staticFiles.directory = "/assets";
                        staticFiles.location = Location.CLASSPATH;
                        staticFiles.hostedPath = "/assets";
                      });
                  config.addStaticFiles(
                      staticFiles -> {
                        staticFiles.directory = "/websites";
                        staticFiles.location = Location.CLASSPATH;
                      });
                })
            .get(
                "/",
                ctx -> {
                  Map<String, String> model = new HashMap<>();
                  model.put("client_type", appConfig.getClientType());
                  model.put("client_id", appConfig.getClientId());
                  String html = null;
                  try {
                    html = formatHtml(readFromResources("websites/index.html"), model);
                  } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                  }
                  ctx.contentType("text/html");
                  ctx.html(html);
                })
            .post(
                "/refresh_token",
                ctx -> {
                  OAuthToken oldToken = ctx.sessionAttribute(genTokenSessionKey());
                  if (oldToken == null) {
                    throw new RuntimeException(
                        "Authorization failed: No authorization code received.");
                  }
                  OAuthToken tokenResp = oauthClient.refreshToken(oldToken.getRefreshToken());
                  ctx.sessionAttribute(genTokenSessionKey(), tokenResp);
                  ctx.json(TokenResponse.convertToTokenResponse(tokenResp));
                })
            .get(
                "/callback",
                ctx -> {
                  String code = ctx.queryParam("code");
                  if (code == null) {
                    throw new RuntimeException(
                        "Authorization failed: No authorization code received.");
                  }
                  try {
                    String codeVerifier = ctx.sessionAttribute(genCodeVerifierSessionKey());
                    OAuthToken tokenResp =
                        oauthClient.getAccessToken(code, appConfig.getRedirectUri(), codeVerifier);
                    ctx.sessionAttribute(genTokenSessionKey(), tokenResp);
                    Map<String, String> model = new HashMap<>();
                    model.put("token_type", tokenResp.getTokenType());
                    model.put("access_token", tokenResp.getAccessToken());
                    model.put("refresh_token", tokenResp.getRefreshToken());
                    model.put(
                        "expires_in",
                        String.format(
                            "%d (%s)",
                            tokenResp.getExpiresIn(),
                            timestampToDateTime(tokenResp.getExpiresIn())));
                    String html = formatHtml(readFromResources("websites/callback.html"), model);
                    ctx.contentType("text/html");
                    ctx.result(html);

                  } catch (Exception e) {
                    throw new RuntimeException("Authorization failed: " + e.getMessage());
                  }
                })
            .get(
                "/login",
                ctx -> {
                  GetPKCEAuthURLResp resp =
                      oauthClient.genOAuthURL(appConfig.getRedirectUri(), "state");
                  ctx.sessionAttribute(genCodeVerifierSessionKey(), resp.getCodeVerifier());
                  ctx.redirect(resp.getAuthorizationURL());
                })
            .exception(
                Exception.class,
                (e, ctx) -> {
                  Map<String, String> model = new HashMap<>();
                  model.put("error", e.getMessage());
                  String html = null;
                  try {
                    html = formatHtml(readFromResources("websites/error.html"), model);
                  } catch (Exception e1) {
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
        return new String(IOUtils.toByteArray(inputStream), StandardCharsets.UTF_8);
      }
    } catch (IOException e) {
      throw new RuntimeException("read file failed: " + fileName, e);
    }
  }

  public static String timestampToDateTime(long timestamp) {
    LocalDateTime dateTime =
        LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }

  private String genCodeVerifierSessionKey() {
    return String.format("code_verifier_%s", appConfig.getClientId());
  }

  private String genTokenSessionKey() {
    return String.format("access_token_%s", appConfig.getClientId());
  }
}
