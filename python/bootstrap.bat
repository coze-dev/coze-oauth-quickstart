@echo off
setlocal enabledelayedexpansion

REM Check if Python is installed
where python >nul 2>nul
if %errorlevel% neq 0 (
    echo Error: python is not installed
    echo Please visit https://www.python.org/downloads/ to install Python
    echo After installation, make sure 'python' command is available in your PATH
    exit /b 1
)

REM Check Python version is >= 3.8
for /f "tokens=*" %%i in ('python -c "import sys; print('.'.join(map(str, sys.version_info[:2])))"') do set pythonVersion=%%i
for /f "tokens=1,2 delims=." %%a in ("%pythonVersion%") do (
    set major=%%a
    set minor=%%b
)
if %major% LSS 3 (
    goto :version_error
) else if %major% EQU 3 (
    if %minor% LSS 8 (
        goto :version_error
    )
)
echo Using Python version: %pythonVersion%
goto :version_ok

:version_error
echo Error: Python version must be ^>= 3.8
echo Current version: %pythonVersion%
echo Please upgrade Python to 3.8 or higher
exit /b 1

:version_ok
REM Check if virtual environment exists
if exist .venv (
    echo Virtual environment already exists
    REM Check if already in virtual environment
    if not defined VIRTUAL_ENV (
        call .venv\Scripts\activate.bat
        echo Activated virtual environment
    )
) else (
    echo Creating new virtual environment and activating it...
    python -m venv .venv
    call .venv\Scripts\activate.bat
)

REM Check if dependencies are installed
echo Checking dependencies...
for /f "tokens=*" %%a in ('pip freeze') do (
    set "installedDeps=!installedDeps!%%a;"
)

for /f "tokens=*" %%i in (requirements.txt) do (
    if not "%%i"=="" (
        for /f "tokens=1,2 delims==" %%a in ("%%i") do (
            set "pkg=%%a==%%b"
            echo !installedDeps! | findstr /C:"!pkg!" >nul
            if !errorlevel! equ 0 (
                echo ✓ !pkg! installed
            ) else (
                echo Installing !pkg! ...
                pip install -q "!pkg!"
            )
        )
    )
)

REM Run the application
python main.py 