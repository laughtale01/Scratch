@echo off
echo ===========================================
echo   Minecraft x Scratch Editor Launcher
echo ===========================================
echo.

:: Check if Python is installed
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python is not installed or not in PATH
    echo Please install Python from https://www.python.org/
    pause
    exit /b 1
)

echo [1/3] Starting local web server...
echo.
echo IMPORTANT: ブラウザで http://localhost:8080 を使用してください
echo           file:/// URLは使用しないでください！
echo.
start cmd /k "cd /d %~dp0 && python -m http.server 8080"

echo [2/3] Waiting for server to start...
timeout /t 3 /nobreak >nul

echo [3/3] Opening Scratch x Minecraft Editor...
start http://localhost:8080/scratch-minecraft-editor.html

echo.
echo ===========================================
echo   Editor is now running\!
echo.
echo   - Web Server: http://localhost:8080
echo   - Editor: http://localhost:8080/scratch-minecraft-editor.html
echo.
echo   Make sure Minecraft is running with the mod installed
echo   and you^'re in a world before using the extension.
echo.
echo   Press any key to view alternative URLs...
echo ===========================================
pause >nul

echo.
echo Alternative URLs to try:
echo.
echo 1. Official-style page with forked Scratch GUI:
echo    http://localhost:8080/scratch-official-minecraft.html
echo.
echo 2. Simple local version:
echo    http://localhost:8080/docs/scratch-local.html
echo.
echo 3. Direct Scratch GUI (need to add extension manually):
echo    http://localhost:8080/scratch-gui/build/index.html
echo.
pause
