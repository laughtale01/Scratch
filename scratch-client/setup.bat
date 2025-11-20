@echo off
echo ========================================
echo MinecraftEdu Scratch Client - Setup
echo ========================================
echo.

set "SCRIPT_DIR=%~dp0"

echo [1/4] Checking Node.js...
node --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Node.js is not installed or not in PATH
    echo Please install Node.js 16.x or higher from https://nodejs.org/
    pause
    exit /b 1
)
echo Node.js: OK
echo.

echo [2/4] Installing Scratch VM dependencies...
cd /d "%SCRIPT_DIR%scratch-vm"
call npm install
if errorlevel 1 (
    echo ERROR: Failed to install scratch-vm dependencies
    pause
    exit /b 1
)
echo scratch-vm: OK
echo.

echo [3/4] Installing Scratch GUI dependencies...
cd /d "%SCRIPT_DIR%scratch-gui"
call npm install
if errorlevel 1 (
    echo ERROR: Failed to install scratch-gui dependencies
    pause
    exit /b 1
)
echo scratch-gui: OK
echo.

echo [4/4] Linking scratch-vm to scratch-gui...
cd /d "%SCRIPT_DIR%scratch-vm"
call npm link
if errorlevel 1 (
    echo WARNING: npm link failed, continuing anyway...
)

cd /d "%SCRIPT_DIR%scratch-gui"
call npm link scratch-vm
if errorlevel 1 (
    echo WARNING: npm link scratch-vm failed
    echo You may need to run this manually later
)
echo.

echo ========================================
echo Setup Complete!
echo ========================================
echo.
echo Next steps:
echo.
echo 1. Start Scratch development server:
echo    cd scratch-gui
echo    npm start
echo.
echo 2. Open browser to http://localhost:8601/
echo.
echo 3. Add Minecraft extension from extension menu
echo.
echo 4. Connect to Minecraft server (make sure MOD is running)
echo.
pause
