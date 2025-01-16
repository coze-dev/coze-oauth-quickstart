#!/bin/bash

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    echo "Error: python3 is not installed"
    echo "Please visit https://www.python.org/downloads/ to install Python"
    echo "After installation, make sure 'python3' command is available in your PATH"
    exit 1
fi

# Check Python version is >= 3.8
python_version=$(python3 -c 'import sys; print(".".join(map(str, sys.version_info[:2])))')
if [ "$(printf '%s\n' "3.8" "$python_version" | sort -V | head -n1)" != "3.8" ]; then
    echo "Error: Python version must be >= 3.8"
    echo "Current version: $python_version"
    echo "Please upgrade Python to 3.8 or higher"
    exit 1
else
    echo "Using Python version: $python_version"
fi

# Check if .venv directory exists
if [ -d ".venv" ]; then
    echo "Virtual environment already exists"
    # Check if already in virtual environment
    if [ -z "$VIRTUAL_ENV" ]; then
        source .venv/bin/activate
        echo "Activated virtual environment"
    fi
else
    echo "Creating new virtual environment and activating it..."
    python3 -m venv .venv
    source .venv/bin/activate
fi

# Check dependencies installed
echo "Checking dependencies..."
installed_deps=$(pip freeze)
while IFS= read -r line || [[ -n "$line" ]]; do
    [ -z "$line" ] && continue # skip empty line
    pkg_name=$(echo "$line" | cut -d'=' -f1)
    pkg_version=$(echo "$line" | cut -d'=' -f3)
    if echo "$installed_deps" | grep -q "^$pkg_name==$pkg_version$"; then
        echo "✓ $pkg_name==$pkg_version installed"
    else
        echo "Installing $pkg_name==$pkg_version ..."
        pip install -q "$pkg_name==$pkg_version"
    fi
done < requirements.txt

# Run the application
python3 main.py
