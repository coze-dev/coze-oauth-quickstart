package com.coze.device;

import com.coze.device.config.AppConfig;
import com.coze.device.model.TokenResponse;
import com.coze.device.server.TokenServer;
import com.coze.device.utils.Client;
import com.coze.openapi.client.auth.DeviceAuthResp;
import com.coze.openapi.client.auth.OAuthToken;
import com.coze.openapi.client.exception.AuthErrorCode;
import com.coze.openapi.client.exception.CozeAuthException;
import com.coze.openapi.service.auth.DeviceOAuthClient;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
public class Main {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8080;
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final int SERVER_START_DELAY = 1000;

    public static void main(String[] args) {
        TokenServer server = null;
        try {
            AppConfig config = initializeConfig();
            DeviceOAuthClient oauth = createOAuthClient(config);
            OAuthToken token = verifyDeviceToken(oauth);
            
            if (token == null) {
                log.error("Failed to obtain access token.");
                return;
            }

            server = startServer(oauth, token, config);
            testTokenEndpoint();
            waitForShutdown();
            
        } catch (Exception e) {
            log.error("Application error", e);
            throw new RuntimeException(e);
        } finally {
            shutdownServer(server);
        }
    }

    private static AppConfig initializeConfig() {
        return AppConfig.load(System.getenv("DEVICE_OAUTH_CONFIG_PATH"));
    }

    private static DeviceOAuthClient createOAuthClient(AppConfig config) {
        return new DeviceOAuthClient.DeviceOAuthBuilder()
                .clientID(config.getClientId())
                .baseURL(config.getCozeApiBase())
                .build();
    }

    private static TokenServer startServer(DeviceOAuthClient oauth, OAuthToken token, AppConfig config) {
        TokenServer server = new TokenServer(oauth, token.getRefreshToken());
        server.start(PORT);
        printServerInfo(config);
        return server;
    }

    private static void shutdownServer(TokenServer server) {
        if (server != null) {
            server.stop();
        }
    }

    public static OAuthToken verifyDeviceToken(DeviceOAuthClient oauth) {
        DeviceAuthResp codeResp = oauth.getDeviceCode();
        printDeviceAuthInstructions(codeResp);
        
        try {
            OAuthToken tokenResp = oauth.getAccessToken(codeResp.getDeviceCode(), true);
            printTokenInfo(tokenResp);
            return tokenResp;
        } catch (CozeAuthException e) {
            handleAuthException(e, oauth);
        } catch (Exception e) {
            log.error("Unexpected error during device verification", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    private static void handleAuthException(CozeAuthException e, DeviceOAuthClient oauth) {
        if (AuthErrorCode.ACCESS_DENIED.equals(e.getCode())) {
            log.warn("Access denied, retrying...");
            verifyDeviceToken(oauth);
        } else if (AuthErrorCode.EXPIRED_TOKEN.equals(e.getCode())) {
            log.warn("Token expired, retrying...");
            verifyDeviceToken(oauth);
        } else {
            log.error("Unhandled error: {}", e.getCode());
            throw e;
        }
    }

    private static void printDeviceAuthInstructions(DeviceAuthResp codeResp) {
        log.info("Device code: {}", codeResp.getUserCode());
        log.info("Please visit the verification URL to complete the authorization: {}", 
                codeResp.getVerificationURL());
    }

    private static void printTokenInfo(OAuthToken tokenResp) {
        log.info("Successfully obtained access token:");
        log.info("Access Token: {}", tokenResp.getAccessToken());
        
        Instant expiresAt = Instant.ofEpochSecond(tokenResp.getExpiresIn());
        String formattedTime = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)
                .withZone(ZoneId.systemDefault())
                .format(expiresAt);
        
        log.info("Token will expire at: {}", formattedTime);
    }

    private static void printServerInfo(AppConfig config) {
        log.info("\nServer starting on {}:{}... (API Base: {}, Client Type: {}, Client ID: {})",
                HOST, PORT, config.getCozeApiBase(), "device client", config.getClientId());
    }

    private static void testTokenEndpoint() throws Exception {
        Thread.sleep(SERVER_START_DELAY);
        log.info("\nMaking request to /refresh_token endpoint to get new access token...");

        String url = String.format("http://%s:%d/refresh_token", HOST, PORT);
        TokenResponse tokenResp = Client.getToken(url);
        tokenResp.print();

        log.info("\nServer is still running. You can get a new access token anytime using: curl {}", url);
    }

    private static void waitForShutdown() throws InterruptedException {
        Thread.currentThread().join();
    }
}