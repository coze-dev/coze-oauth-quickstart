package com.coze.jwt.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.coze.openapi.client.auth.OAuthToken;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("expires_in")
  private Long expiresIn;

  public static TokenResponse convertToTokenResponse(OAuthToken oauthToken) {
    return TokenResponse.builder()
        .accessToken(oauthToken.getAccessToken())
        .expiresIn(Long.valueOf(oauthToken.getExpiresIn()))
        .build();
  }

  public void print() {
    System.out.println("Successfully refresh access token:");
    System.out.println("Access Token: " + this.accessToken);
    Instant expiresAt = Instant.ofEpochSecond(this.expiresIn);
    System.out.println(
        "Token will expire at: "
            + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(expiresAt));
  }
}
