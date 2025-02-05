package main

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"strings"
	"time"

	"github.com/coze-dev/coze-go"
	"github.com/gorilla/sessions"
)

const (
	CozeOAuthConfigPath = "coze_oauth_config.json"
	RedirectURI         = "http://127.0.0.1:8080/callback"
)

type TokenResponse struct {
	TokenType    string `json:"token_type"`
	AccessToken  string `json:"access_token"`
	RefreshToken string `json:"refresh_token"`
	ExpiresIn    string `json:"expires_in"`
}

var store = sessions.NewCookieStore([]byte("secret-key"))

func loadConfig() (*coze.WebOAuthClient, error) {
	configFile, err := os.ReadFile(CozeOAuthConfigPath)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, fmt.Errorf("coze_oauth_config.json not found in current directory")
		}
		return nil, fmt.Errorf("failed to read config file: %v", err)
	}

	oauth, err := coze.LoadOAuthAppFromConfig(configFile)
	if err != nil {
		return nil, fmt.Errorf("failed to load OAuth config: %v", err)
	}

	config, ok := oauth.(*coze.WebOAuthClient)
	if !ok {
		return nil, fmt.Errorf("invalid OAuth client type: expected Web client")
	}
	return config, nil
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

func handleError(w http.ResponseWriter, err error) {
	template, parseErr := readHTMLTemplate("websites/error.html")
	if parseErr != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	// Read config to get domain info
	configFile, readErr := os.ReadFile(CozeOAuthConfigPath)
	if readErr != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	var rawConfig struct {
		CozeDomain string `json:"coze_www_base"`
	}
	if jsonErr := json.Unmarshal(configFile, &rawConfig); jsonErr != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	data := map[string]interface{}{
		"error":         err.Error(),
		"coze_www_base": rawConfig.CozeDomain,
	}

	w.WriteHeader(http.StatusInternalServerError)
	result := renderTemplate(template, data)
	w.Write([]byte(result))
}

func main() {
	log.SetFlags(0)

	oauth, err := loadConfig()
	if err != nil {
		log.Fatalf("Error loading config: %v", err)
	}

	// Read raw config for UI display
	configFile, err := os.ReadFile(CozeOAuthConfigPath)
	if err != nil {
		log.Fatalf("Error reading config file: %v", err)
	}
	var rawConfig struct {
		ClientType  string `json:"client_type"`
		ClientID    string `json:"client_id"`
		CozeAPIBase string `json:"coze_api_base"`
		CozeDomain  string `json:"coze_www_base"`
	}
	if err := json.Unmarshal(configFile, &rawConfig); err != nil {
		log.Fatalf("Error parsing config file: %v", err)
	}

	fs := http.FileServer(http.Dir("assets"))
	http.Handle("/assets/", http.StripPrefix("/assets/", fs))

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/" {
			http.NotFound(w, r)
			return
		}

		template, err := readHTMLTemplate("websites/index.html")
		if err != nil {
			handleError(w, fmt.Errorf("failed to read template: %v", err))
			return
		}

		data := map[string]interface{}{
			"client_type": rawConfig.ClientType,
			"client_id":   rawConfig.ClientID,
		}

		result := renderTemplate(template, data)
		w.Write([]byte(result))
	})

	http.HandleFunc("/login", func(w http.ResponseWriter, r *http.Request) {
		ctx := context.Background()
		authURL := oauth.GetOAuthURL(ctx, &coze.GetWebOAuthURLReq{
			RedirectURI: RedirectURI,
			State:       "random",
		})
		http.Redirect(w, r, authURL, http.StatusFound)
	})

	http.HandleFunc("/callback", func(w http.ResponseWriter, r *http.Request) {
		code := r.URL.Query().Get("code")
		if code == "" {
			handleError(w, fmt.Errorf("authorization failed: no authorization code received"))
			return
		}

		ctx := context.Background()
		resp, err := oauth.GetAccessToken(ctx, &coze.GetWebOAuthAccessTokenReq{
			Code:        code,
			RedirectURI: RedirectURI,
		})
		if err != nil {
			handleError(w, fmt.Errorf("failed to get access token: %v", err))
			return
		}

		// Store token in session
		session, _ := store.Get(r, "oauth_token")
		session.Values["token_type"] = "Bearer"
		session.Values["access_token"] = resp.AccessToken
		session.Values["refresh_token"] = resp.RefreshToken
		session.Values["expires_in"] = resp.ExpiresIn
		session.Save(r, w)

		expiresStr := fmt.Sprintf("%d (%s)", resp.ExpiresIn, timestampToDateTime(resp.ExpiresIn))

		template, err := readHTMLTemplate("websites/callback.html")
		if err != nil {
			handleError(w, fmt.Errorf("failed to read template: %v", err))
			return
		}

		data := map[string]interface{}{
			"token_type":    "Bearer",
			"access_token":  resp.AccessToken,
			"refresh_token": resp.RefreshToken,
			"expires_in":    expiresStr,
			"coze_www_base": rawConfig.CozeDomain,
		}

		result := renderTemplate(template, data)
		w.Write([]byte(result))
	})

	http.HandleFunc("/refresh_token", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
			return
		}

		var requestData struct {
			RefreshToken string `json:"refresh_token"`
		}

		if err := json.NewDecoder(r.Body).Decode(&requestData); err != nil {
			http.Error(w, "Invalid request body", http.StatusBadRequest)
			return
		}

		if requestData.RefreshToken == "" {
			http.Error(w, "No refresh token provided", http.StatusBadRequest)
			return
		}

		ctx := context.Background()
		resp, err := oauth.RefreshToken(ctx, requestData.RefreshToken)
		if err != nil {
			http.Error(w, fmt.Sprintf("Failed to refresh token: %v", err), http.StatusInternalServerError)
			return
		}

		// Update session
		session, _ := store.Get(r, "oauth_token")
		session.Values["token_type"] = "Bearer"
		session.Values["access_token"] = resp.AccessToken
		session.Values["refresh_token"] = resp.RefreshToken
		session.Values["expires_in"] = resp.ExpiresIn
		session.Save(r, w)

		expiresStr := fmt.Sprintf("%d (%s)", resp.ExpiresIn, timestampToDateTime(resp.ExpiresIn))

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"token_type":    "Bearer",
			"access_token":  resp.AccessToken,
			"refresh_token": resp.RefreshToken,
			"expires_in":    expiresStr,
		})
	})

	log.Printf("Server starting on http://127.0.0.1:8080 (API Base: %s, Client Type: %s, Client ID: %s)\n",
		rawConfig.CozeAPIBase, rawConfig.ClientType, rawConfig.ClientID)
	if err := http.ListenAndServe("127.0.0.1:8080", nil); err != nil {
		log.Fatalf("Server failed to start: %v", err)
	}
}
