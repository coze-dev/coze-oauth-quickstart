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

type TokenResponse struct {
	TokenType    string `json:"token_type"`
	AccessToken  string `json:"access_token"`
	RefreshToken string `json:"refresh_token"`
	ExpiresIn    string `json:"expires_in"`
}

func loadConfig() (*coze.JWTOAuthClient, error) {
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

	oauth, err := coze.LoadOAuthAppFromConfig(&oauthConfig)
	if err != nil {
		return nil, fmt.Errorf("failed to load OAuth config: %v", err)
	}

	jwtClient, ok := oauth.(*coze.JWTOAuthClient)
	if !ok {
		return nil, fmt.Errorf("invalid OAuth client type: expected JWT client")
	}
	return jwtClient, nil
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

	oauth, err := loadConfig()
	if err != nil {
		log.Fatalf("Error loading config: %v", err)
	}

	listener, err := net.Listen("tcp", "127.0.0.1:8080")
	if err != nil {
		log.Fatalf("Port 8080 is already in use by another application. Please free up the port and try again")
	}
	listener.Close()

	fs := http.FileServer(http.Dir("assets"))
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

		// Read config again to get client info
		configFile, err := os.ReadFile(CozeOAuthConfigPath)
		if err != nil {
			http.Error(w, "Internal Server Error", http.StatusInternalServerError)
			return
		}
		var rawConfig struct {
			ClientType string `json:"client_type"`
			ClientID   string `json:"client_id"`
			CozeDomain string `json:"coze_www_base"`
		}
		if err := json.Unmarshal(configFile, &rawConfig); err != nil {
			http.Error(w, "Internal Server Error", http.StatusInternalServerError)
			return
		}

		data := map[string]interface{}{
			"client_type":   rawConfig.ClientType,
			"client_id":     rawConfig.ClientID,
			"coze_www_base": rawConfig.CozeDomain,
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

			// Read config again to get domain info
			configFile, err := os.ReadFile(CozeOAuthConfigPath)
			if err != nil {
				http.Error(w, "Internal Server Error", http.StatusInternalServerError)
				return
			}
			var rawConfig struct {
				CozeDomain string `json:"coze_www_base"`
			}
			if err := json.Unmarshal(configFile, &rawConfig); err != nil {
				http.Error(w, "Internal Server Error", http.StatusInternalServerError)
				return
			}

			data := map[string]interface{}{
				"error":         fmt.Sprintf("Failed to get access token: %v", err),
				"coze_www_base": rawConfig.CozeDomain,
			}

			w.WriteHeader(http.StatusInternalServerError)
			result := renderTemplate(template, data)
			w.Write([]byte(result))
			return
		}

		expiresStr := fmt.Sprintf("%d (%s)", resp.ExpiresIn, timestampToDateTime(resp.ExpiresIn))
		tokenResp := TokenResponse{
			TokenType:    "Bearer",
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
			"token_type":    tokenResp.TokenType,
			"access_token":  tokenResp.AccessToken,
			"refresh_token": tokenResp.RefreshToken,
			"expires_in":    tokenResp.ExpiresIn,
		}

		result := renderTemplate(template, data)
		w.Write([]byte(result))
	})

	// Read config again to get client info for logging
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

	log.Printf("\nServer starting on http://127.0.0.1:8080 (API Base: %s, Client Type: %s, Client ID: %s)\n",
		rawConfig.CozeAPIBase, rawConfig.ClientType, rawConfig.ClientID)
	if err := http.ListenAndServe("127.0.0.1:8080", nil); err != nil {
		log.Fatalf("Server failed to start: %v", err)
	}
}
