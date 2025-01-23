#!/usr/bin/env pwsh

# Check if NodeJS is installed
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Error "Error: Node.js is not installed"
    Write-Host "Please visit https://nodejs.org/ to install Node.js"
    Write-Host "After installation, make sure 'node' command is available in your PATH"
    exit 1
}

# Check Node.js version is >= 14
$nodeVersion = (node -v).Substring(1)
$minVersion = [version]"14.0.0"
$currentVersion = [version]$nodeVersion
if ($currentVersion -lt $minVersion) {
    Write-Error "Error: Node.js version must be >= 14"
    Write-Host "Current version: $nodeVersion"
    Write-Host "Please upgrade Node.js to 14 or higher"
    exit 1
} else {
    Write-Host "Using Node.js version: $nodeVersion"
}

# Check if node_modules directory exists
if (-not (Test-Path "node_modules")) {
    Write-Host "Installing dependencies..."
    npm install
} else {
    Write-Host "Dependencies already installed"
}

# Check if package.json exists
if (-not (Test-Path "package.json")) {
    Write-Error "Error: package.json not found"
    exit 1
}

# Run the application
npm start