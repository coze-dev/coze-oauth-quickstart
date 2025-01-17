package com.coze.device;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.coze.openapi.client.auth.DeviceAuthResp;
import com.coze.openapi.client.auth.LoadAuthConfig;
import com.coze.openapi.client.auth.OAuthToken;
import com.coze.openapi.client.exception.AuthErrorCode;
import com.coze.openapi.client.exception.CozeAuthException;
import com.coze.openapi.service.auth.DeviceOAuthClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
  private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
  private static final String configFilePath = "coze_oauth_config.json";

  public static void main(String[] args) {
    try {
      DeviceOAuthClient oauth =
          DeviceOAuthClient.loadFromConfig(new LoadAuthConfig(configFilePath));
      OAuthToken token = verifyDeviceToken(oauth);

      if (token == null) {
        log.error("Failed to obtain access token.");
      }
    } catch (Exception e) {
      log.error("Application error", e);
      throw new RuntimeException(e);
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
    log.info(
        "Please visit the verification URL to complete the authorization: {}",
        codeResp.getVerificationURL());
  }

  private static void printTokenInfo(OAuthToken tokenResp) {
    log.info("Successfully obtained access token:");
    log.info("Access Token: {}", tokenResp.getAccessToken());
    log.info("Refresh Token: {}", tokenResp.getRefreshToken());

    log.info(
        "Token will expire at: {}",
        DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochSecond(tokenResp.getExpiresIn())));
  }
}
