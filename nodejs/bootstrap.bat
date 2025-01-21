#!/bin/bash

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "Error: Node.js is not installed"
    echo "Please visit https://nodejs.org/ to install Node.js"
    echo "After installation, make sure 'node' command is available in your PATH"
    exit 1
fi

# Check Node.js version is >= 14
node_version=$(node -v | cut -c 2-)
if [ "$(printf '%s\n' "14.0.0" "$node_version" | sort -V | head -n1)" != "14.0.0" ]; then
    echo "Error: Node.js version must be >= 14"
    echo "Current version: $node_version"
    echo "Please upgrade Node.js to 14 or higher"
    exit 1
else
    echo "Using Node.js version: $node_version"
fi

# Check if node_modules directory exists
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
else
    echo "Dependencies already installed"
fi

# Check if package.json exists
if [ ! -f "package.json" ]; then
    echo "Error: package.json not found"
    exit 1
fi

# Run the application
npm start
