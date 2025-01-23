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

# Build the application
echo "Building application..."
go build -o build/main main.go || {
    echo "Error: Failed to build the application"
    exit 1
}

# Run the application
echo "Starting the application..."
./build/main