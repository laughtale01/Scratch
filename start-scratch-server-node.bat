@echo off
echo ========================================
echo  Scratch GUI Local Server (Node.js)
echo ========================================
echo.

REM Check if http-server is installed
where http-server >nul 2>nul
if %errorlevel% neq 0 (
    echo Installing http-server globally...
    npm install -g http-server
    echo.
)

echo Starting local HTTP server for Scratch GUI...
echo Access at: http://localhost:8080
echo.
echo Press Ctrl+C to stop the server
echo.

cd scratch-gui\build
http-server -p 8080 -c-1 --cors

pause