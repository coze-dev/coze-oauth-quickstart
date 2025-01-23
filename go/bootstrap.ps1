# Check if Go is installed
if (-not (Get-Command "go" -ErrorAction SilentlyContinue)) {
    Write-Error "Error: Go is not installed on your system."
    Write-Host "To install Go, please visit: https://golang.org/doc/install"
    Write-Host "After installation, make sure 'go' command is available in your PATH"
    exit 1
}

# Print Go version
Write-Host "Using Go version: $(go version)"

# Build the application
Write-Host "Building application..."
if (-not (Test-Path "build")) {
    New-Item -ItemType Directory -Path "build"
}

try {
    go build -o build/main.exe main.go
} catch {
    Write-Error "Error: Failed to build the application"
    exit 1
}

# Run the application
Write-Host "Starting the application..."
& "./build/main.exe" 