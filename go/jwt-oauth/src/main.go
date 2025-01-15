package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net"
	"net/http"
	"os"
	"time"

	"github.com/coze-dev/coze-go"
)

const CozeOAuthConfigPath = "coze_oauth_config.json"

type Config struct {
	ClientType  string `json:"client_type"`
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

	return &config, nil
}

func main() {
	// Set log flags to remove timestamp
	log.SetFlags(0)

	// Load configuration from coze_oauth_config.json
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

	// Check if port 8080 is available
	listener, err := net.Listen("tcp", "127.0.0.1:8080")
	if err != nil {
		log.Fatalf("Port 8080 is already in use by another application. Please free up the port and try again")
	}
	listener.Close()

	// Define HTTP handler
	http.HandleFunc("/token", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
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

	// Start HTTP server in a separate goroutine
	go func() {
		log.Printf("\nServer starting on 127.0.0.1:8080... (API Base: %s, Client Type: %s, Client ID: %s)\n",
			config.CozeAPIBase, config.ClientType, config.ClientID)
		if err := http.ListenAndServe("127.0.0.1:8080", nil); err != nil {
			log.Fatalf("Server failed to start: %v", err)
		}
	}()

	// Wait a moment for the server to start
	time.Sleep(time.Second)

	// Make a POST request to the local /token endpoint
	log.Println("\nMaking request to /token endpoint to get access token...")
	resp, err := http.Get("http://127.0.0.1:8080/token")
	if err != nil {
		log.Fatalf("Failed to request token: %v", err)
	}
	defer resp.Body.Close()

	// Parse the response
	var tokenResp TokenResponse
	if err := json.NewDecoder(resp.Body).Decode(&tokenResp); err != nil {
		log.Fatalf("Failed to decode token response: %v", err)
	}

	// Print the access token information
	log.Printf("Successfully obtained access token:")
	log.Printf("Access Token: %s", tokenResp.AccessToken)
	expiresAt := time.Unix(tokenResp.ExpiresIn, 0)
	log.Printf("Token will expire at: %s", expiresAt.Format("2006-01-02 15:04:05"))

	log.Printf("\nServer is still running. You can get a new access token anytime using: curl http://127.0.0.1:8080/token")

	// Keep the main goroutine running
	select {}
}
