package main

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net"
	"net/http"
	"os"
	"strings"
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
	TokenType    string `json:"token_type"`
	AccessToken  string `json:"access_token"`
	RefreshToken string `json:"refresh_token"`
	ExpiresIn    string `json:"expires_in"`
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

	return &config, nil
}

func timestampToDateTime(timestamp int64) string {
	t := time.Unix(timestamp, 0)
	return t.Format("2006-01-02 15:04:05")
}

func readHTMLTemplate(filePath string) (string, error) {
	content, err := ioutil.ReadFile(filePath)
	if err != nil {
		return "", fmt.Errorf("failed to read template file: %v", err)
	}
	return string(content), nil
}

func renderTemplate(template string, data map[string]interface{}) string {
	result := template
	for key, value := range data {
		placeholder := "{{" + key + "}}"
		result = strings.ReplaceAll(result, placeholder, fmt.Sprintf("%v", value))
	}
	return result
}

func main() {
	log.SetFlags(0)

	config, err := loadConfig()
	if err != nil {
		log.Fatalf("Error loading config: %v", err)
	}

	oauth, err := coze.NewJWTOAuthClient(coze.NewJWTOAuthClientParam{
		ClientID:      config.ClientID,
		PublicKey:     config.PublicKeyID,
		PrivateKeyPEM: config.PrivateKey,
	}, coze.WithAuthBaseURL(config.CozeAPIBase))
	if err != nil {
		log.Fatalf("Error creating JWT OAuth client: %v\n", err)
	}

	listener, err := net.Listen("tcp", "127.0.0.1:8080")
	if err != nil {
		log.Fatalf("Port 8080 is already in use by another application. Please free up the port and try again")
	}
	listener.Close()

	fs := http.FileServer(http.Dir("websites/assets"))
	http.Handle("/assets/", http.StripPrefix("/assets/", fs))

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/" {
			http.NotFound(w, r)
			return
		}

		template, err := readHTMLTemplate("websites/index.html")
		if err != nil {
			http.Error(w, "Internal Server Error", http.StatusInternalServerError)
			return
		}

		data := map[string]interface{}{
			"client_type":   config.ClientType,
			"client_id":     config.ClientID,
			"coze_www_base": config.CozeDomain,
		}

		result := renderTemplate(template, data)
		w.Write([]byte(result))
	})

	http.HandleFunc("/login", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "/callback", http.StatusFound)
	})

	http.HandleFunc("/callback", func(w http.ResponseWriter, r *http.Request) {
		ctx := context.Background()
		resp, err := oauth.GetAccessToken(ctx, nil)
		if err != nil {
			template, parseErr := readHTMLTemplate("websites/error.html")
			if parseErr != nil {
				http.Error(w, "Internal Server Error", http.StatusInternalServerError)
				return
			}

			data := map[string]interface{}{
				"error":         fmt.Sprintf("Failed to get access token: %v", err),
				"coze_www_base": config.CozeDomain,
			}

			w.WriteHeader(http.StatusInternalServerError)
			result := renderTemplate(template, data)
			w.Write([]byte(result))
			return
		}

		expiresStr := fmt.Sprintf("%d (%s)", resp.ExpiresIn, timestampToDateTime(resp.ExpiresIn))
		tokenResp := TokenResponse{
			AccessToken:  resp.AccessToken,
			RefreshToken: "",
			ExpiresIn:    expiresStr,
		}

		// Check if it's an AJAX request
		if r.Header.Get("X-Requested-With") == "XMLHttpRequest" {
			w.Header().Set("Content-Type", "application/json")
			json.NewEncoder(w).Encode(tokenResp)
			return
		}

		// Otherwise render the callback template
		template, err := readHTMLTemplate("websites/callback.html")
		if err != nil {
			http.Error(w, "Internal Server Error", http.StatusInternalServerError)
			return
		}

		data := map[string]interface{}{
			"access_token":  tokenResp.AccessToken,
			"refresh_token": tokenResp.RefreshToken,
			"expires_in":    tokenResp.ExpiresIn,
		}

		result := renderTemplate(template, data)
		w.Write([]byte(result))
	})

	log.Printf("\nServer starting on 127.0.0.1:8080... (API Base: %s, Client Type: %s, Client ID: %s)\n",
		config.CozeAPIBase, config.ClientType, config.ClientID)
	if err := http.ListenAndServe("127.0.0.1:8080", nil); err != nil {
		log.Fatalf("Server failed to start: %v", err)
	}
}
