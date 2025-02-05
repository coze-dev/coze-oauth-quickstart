package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/coze-dev/coze-go"
)

const CozeOAuthConfigPath = "coze_oauth_config.json"

// tokenTransport is an http.RoundTripper that adds an Authorization header
type tokenTransport struct {
	accessToken string
}

func (t *tokenTransport) RoundTrip(req *http.Request) (*http.Response, error) {
	req.Header.Set("Authorization", "Bearer "+t.accessToken)
	return http.DefaultTransport.RoundTrip(req)
}

func loadConfig() (*coze.DeviceOAuthClient, error) {
	configFile, err := os.ReadFile(CozeOAuthConfigPath)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, fmt.Errorf("coze_oauth_config.json not found in current directory")
		}
		return nil, fmt.Errorf("failed to read config file: %v", err)
	}

	var oauthConfig coze.OAuthConfig
	if err := json.Unmarshal(configFile, &oauthConfig); err != nil {
		return nil, fmt.Errorf("failed to parse config file: %v", err)
	}

	oauth, err := coze.LoadOAuthAppFromConfig(oauthConfig)
	if err != nil {
		return nil, fmt.Errorf("failed to load OAuth config: %v", err)
	}

	deviceClient, ok := oauth.(*coze.DeviceOAuthClient)
	if !ok {
		return nil, fmt.Errorf("invalid OAuth client type: expected Device client")
	}
	return deviceClient, nil
}

func timestampToDateTime(timestamp int64) string {
	t := time.Unix(timestamp, 0)
	return t.Format("2006-01-02 15:04:05")
}

func main() {
	log.SetFlags(0)

	oauth, err := loadConfig()
	if err != nil {
		log.Fatalf("Error loading config: %v", err)
	}

	// Read raw config for logging
	configFile, err := os.ReadFile(CozeOAuthConfigPath)
	if err != nil {
		log.Fatalf("Error reading config file: %v", err)
	}
	var rawConfig struct {
		ClientType  string `json:"client_type"`
		ClientID    string `json:"client_id"`
		CozeAPIBase string `json:"coze_api_base"`
	}
	if err := json.Unmarshal(configFile, &rawConfig); err != nil {
		log.Fatalf("Error parsing config file: %v", err)
	}

	ctx := context.Background()
	deviceCode, err := oauth.GetDeviceCode(ctx, &coze.GetDeviceOAuthCodeReq{})
	if err != nil {
		log.Fatalf("Error getting device code: %v\n", err)
	}

	fmt.Println("Please visit the following url to authorize the app:")
	fmt.Printf("    URL: %s\n\n", deviceCode.VerificationURL)

	resp, err := oauth.GetAccessToken(ctx, &coze.GetDeviceOAuthAccessTokenReq{
		DeviceCode: deviceCode.DeviceCode,
		Poll:       true,
	})
	if err != nil {
		log.Fatalf("Error getting access token: %v\n", err)
	}

	fmt.Printf("[device-oauth] access_token: %s\n", resp.AccessToken)
	fmt.Printf("[device-oauth] refresh_token: %s\n", resp.RefreshToken)
	expiresStr := timestampToDateTime(resp.ExpiresIn)
	fmt.Printf("[device-oauth] expires_in: %d (%s)\n", resp.ExpiresIn, expiresStr)

	// Get user info
	client := coze.NewCozeAPI(coze.NewTokenAuth(resp.AccessToken),
		coze.WithBaseURL(rawConfig.CozeAPIBase),
	)
	if err != nil {
		log.Fatalf("Failed to create users client: %v", err)
	}

	user, err := client.Users.Me(context.Background())
	if err != nil {
		log.Fatalf("Failed to get user info: %v", err)
	}

	fmt.Printf("[user_info] user_id: %s\n", user.UserID)
	fmt.Printf("[user_info] user_name: %s\n", user.UserName)
	fmt.Printf("[user_info] nick_name: %s\n", user.NickName)
	fmt.Printf("[user_info] avatar_url: %s\n", user.AvatarURL)
}
