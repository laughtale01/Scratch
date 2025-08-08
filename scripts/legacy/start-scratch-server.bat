@echo off
echo ========================================
echo  Scratch GUI Local Server
echo ========================================
echo.
echo Starting local HTTP server for Scratch GUI...
echo Access at: http://localhost:8080
echo.
echo Press Ctrl+C to stop the server
echo.

cd scratch-gui\build
python -m http.server 8080

pause