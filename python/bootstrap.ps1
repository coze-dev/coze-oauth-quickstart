#!/usr/bin/env pwsh

# Check if Python is installed
if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
    Write-Error "Error: python is not installed"
    Write-Host "Please visit https://www.python.org/downloads/ to install Python"
    Write-Host "After installation, make sure 'python' command is available in your PATH"
    exit 1
}

# Check Python version is >= 3.8
$pythonVersion = python -c "import sys; print('.'.join(map(str, sys.version_info[:2])))"
if ([version]$pythonVersion -lt [version]"3.8") {
    Write-Error "Error: Python version must be >= 3.8"
    Write-Host "Current version: $pythonVersion"
    Write-Host "Please upgrade Python to 3.8 or higher"
    exit 1
} else {
    Write-Host "Using Python version: $pythonVersion"
}

# Check if virtual environment exists
if (Test-Path ".venv") {
    Write-Host "Virtual environment already exists"
    # Check if already in virtual environment
    if (-not $env:VIRTUAL_ENV) {
        .\.venv\Scripts\Activate.ps1
        Write-Host "Activated virtual environment"
    }
} else {
    Write-Host "Creating new virtual environment and activating it..."
    python -m venv .venv
    .\.venv\Scripts\Activate.ps1
}

# Check if dependencies are installed
Write-Host "Checking dependencies..."
$installedDeps = pip freeze
foreach($line in Get-Content requirements.txt) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    $pkgName = $line.Split('==')[0]
    $pkgVersion = $line.Split('==')[1]
    if ($installedDeps -match "^$pkgName==$pkgVersion$") {
        Write-Host "✓ $pkgName==$pkgVersion installed"
    } else {
        Write-Host "Installing $pkgName==$pkgVersion ..."
        pip install -q "$pkgName==$pkgVersion"
    }
}

# Run the application
python main.py 