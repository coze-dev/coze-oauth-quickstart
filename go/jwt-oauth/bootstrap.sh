#!/bin/bash

# Check if Go is installed
if ! command -v go &> /dev/null; then
    echo "Error: Go is not installed on your system."
    echo "To install Go, please visit: https://golang.org/doc/install"
    echo "After installation, make sure 'go' command is available in your PATH"
    exit 1
fi

# Print Go version
echo "Using Go version: $(go version)"

# Change to src directory and build
cd src || {
    echo "Error: src directory not found"
    exit 1
}

# Build the binary
echo "Building application..."
go build -o coze_jwt_quickstart main.go || {
    echo "Error: Failed to build the application"
    exit 1
}

# Run the binary
echo "Starting the application..."
./coze_jwt_quickstart
