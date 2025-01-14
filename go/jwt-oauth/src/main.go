package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"

	"github.com/coze-dev/coze-go"
)

type Config struct {
	ClientType  string `json:"client_type"`
	AppID       string `json:"app_id"`
	ClientID    string `json:"client_id"`
	PrivateKey  string `json:"private_key"`
	PublicKeyID string `json:"public_key_id"`
	CozeDomain  string `json:"coze_domain"`
	CozeAPIBase string `json:"coze_api_base"`
}

type TokenResponse struct {
	AccessToken string `json:"access_token"`
	ExpiresIn   int64  `json:"expires_in"`
}

func loadConfig() (*Config, error) {
	// Read config file from current directory
	configFile, err := os.ReadFile("config.json")
	if err != nil {
		if os.IsNotExist(err) {
			return nil, fmt.Errorf("config.json not found in current directory")
		}
		return nil, fmt.Errorf("failed to read config file: %v", err)
	}

	var config Config
	if err := json.Unmarshal(configFile, &config); err != nil {
		return nil, fmt.Errorf("failed to parse config file: %v", err)
	}

	return &config, nil
}

func main() {
	// Load configuration from config.json
	config, err := loadConfig()
	if err != nil {
		log.Fatalf("Error loading config: %v", err)
	}

	// Initialize the JWT OAuth client
	oauth, err := coze.NewJWTOAuthClient(coze.NewJWTOAuthClientParam{
		ClientID:      config.ClientID,
		PublicKey:     config.PublicKeyID,
		PrivateKeyPEM: config.PrivateKey,
	}, coze.WithAuthBaseURL(config.CozeAPIBase))
	if err != nil {
		log.Fatalf("Error creating JWT OAuth client: %v\n", err)
	}

	// Define HTTP handler
	http.HandleFunc("/token", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
			return
		}

		ctx := context.Background()
		resp, err := oauth.GetAccessToken(ctx, nil)
		if err != nil {
			http.Error(w, fmt.Sprintf("Error getting access token: %v", err), http.StatusInternalServerError)
			return
		}

		tokenResp := TokenResponse{
			AccessToken: resp.AccessToken,
			ExpiresIn:   resp.ExpiresIn,
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(tokenResp)
	})

	fmt.Printf("Server starting on :8080... (API Base: %s)\n", config.CozeAPIBase)
	if err := http.ListenAndServe(":8080", nil); err != nil {
		log.Fatalf("Server failed to start: %v", err)
	}
}
