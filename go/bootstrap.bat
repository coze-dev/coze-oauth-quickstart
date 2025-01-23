@echo off
REM Check if Go is installed
where go >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: Go is not installed on your system.
    echo To install Go, please visit: https://golang.org/doc/install
    echo After installation, make sure 'go' command is available in your PATH
    exit /b 1
)

REM Print Go version
echo Using Go version:
go version

REM Build the application
echo Building application...
if not exist build mkdir build
go build -o build\main.exe main.go
if %ERRORLEVEL% NEQ 0 (
    echo Error: Failed to build the application
    exit /b 1
)

REM Run the application
echo Starting the application...
build\main.exe