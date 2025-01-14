package main

import (
	"encoding/json"
	"os"
	"testing"
)

func TestUpdatePrivateKey(t *testing.T) {
	// Read the PEM file
	pemContent, err := os.ReadFile("x.pem")
	if err != nil {
		t.Fatalf("Failed to read PEM file: %v", err)
	}

	// Read the current config
	configFile, err := os.ReadFile("config.json")
	if err != nil {
		t.Fatalf("Failed to read config file: %v", err)
	}

	var config Config
	if err := json.Unmarshal(configFile, &config); err != nil {
		t.Fatalf("Failed to parse config file: %v", err)
	}

	// Update the private key
	config.PrivateKey = string(pemContent)

	// Write back to config.json
	updatedConfig, err := json.MarshalIndent(config, "", "    ")
	if err != nil {
		t.Fatalf("Failed to marshal config: %v", err)
	}

	if err := os.WriteFile("config.json", updatedConfig, 0644); err != nil {
		t.Fatalf("Failed to write config file: %v", err)
	}
}
