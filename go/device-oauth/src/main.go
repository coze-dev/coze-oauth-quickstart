package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"time"

	"github.com/coze-dev/coze-go"
)

const CozeOAuthConfigPath = "coze_oauth_config.json"

type Config struct {
	ClientType  string `json:"client_type"`
	ClientID    string `json:"client_id"`
	CozeDomain  string `json:"coze_www_base"`
	CozeAPIBase string `json:"coze_api_base"`
}

func loadConfig() (*Config, error) {
	configFile, err := os.ReadFile(CozeOAuthConfigPath)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, fmt.Errorf("coze_oauth_config.json not found in current directory")
		}
		return nil, fmt.Errorf("failed to read config file: %v", err)
	}

	var config Config
	if err := json.Unmarshal(configFile, &config); err != nil {
		return nil, fmt.Errorf("failed to parse config file: %v", err)
	}

	if config.ClientType != "device" {
		return nil, fmt.Errorf("invalid client type: %s. expected: device", config.ClientType)
	}

	return &config, nil
}

func timestampToDateTime(timestamp int64) string {
	t := time.Unix(timestamp, 0)
	return t.Format("2006-01-02 15:04:05")
}

func main() {
	log.SetFlags(0)

	config, err := loadConfig()
	if err != nil {
		log.Fatalf("Error loading config: %v", err)
	}

	oauth, err := coze.NewDeviceOAuthClient(
		config.ClientID,
		coze.WithAuthBaseURL(config.CozeAPIBase),
	)
	if err != nil {
		log.Fatalf("Error creating device OAuth client: %v\n", err)
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
	})
	if err != nil {
		log.Fatalf("Error getting access token: %v\n", err)
	}

	fmt.Printf("[device-oauth] access_token: %s\n", resp.AccessToken)
	fmt.Printf("[device-oauth] refresh_token: %s\n", resp.RefreshToken)
	expiresStr := timestampToDateTime(resp.ExpiresIn)
	fmt.Printf("[device-oauth] expires_in: %d (%s)\n", resp.ExpiresIn, expiresStr)
}
