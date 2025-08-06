@echo off
echo === Scratch GUI 再起動スクリプト ===
echo.

echo [1/3] 既存のNode.jsプロセスを停止中...
taskkill /F /IM node.exe /T 2>nul
timeout /t 2 >nul

echo [2/3] 最新の拡張機能をコピー中...
copy /Y "scratch-extension\src\index.js" "scratch-gui\src\lib\libraries\extensions\minecraft\index.js"
copy /Y "scratch-extension\src\index.js" "scratch-gui\static\extensions\minecraft-unified.js"
echo.

echo [3/3] Scratch GUI開発サーバーを起動中...
cd scratch-gui
start cmd /k "npm start"
echo.

echo === 完了！ ===
echo.
echo ブラウザで以下のURLにアクセスしてください:
echo http://localhost:8601/
echo.
echo 注意: キャッシュをクリアするには Ctrl+F5 を押してください。
echo.
pause