@echo off
echo Starting local HTTP server for Scratch extension...
echo.
echo Extension URL: http://localhost:8080/scratch-extension/src/index.js
echo.
echo Use this URL in TurboWarp:
echo 1. Open https://turbowarp.org/editor
echo 2. Click Extension button (bottom-left)
echo 3. Choose "Custom Extension"
echo 4. Enter: http://localhost:8080/scratch-extension/src/index.js
echo.
python -m http.server 8080 --bind 127.0.0.1
pause