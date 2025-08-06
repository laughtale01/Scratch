@echo off
echo ========================================
echo   開発サーバー起動ツール
echo ========================================
echo.

REM 2つのコマンドプロンプトを開いて並行実行
echo [1/2] Minecraft WebSocketサーバーを起動準備中...
start "Minecraft WebSocket Server" cmd /k "cd minecraft-mod && echo Minecraftを起動して /collab start を実行してください && pause"

echo [2/2] Scratch開発サーバーを起動中...
start "Scratch Dev Server" cmd /k "cd scratch-gui && npm start"

echo.
echo ========================================
echo   開発サーバーを起動しました
echo ========================================
echo.
echo 📋 次の手順：
echo 1. Minecraftを起動
echo 2. ワールドに入って /collab start を実行
echo 3. ブラウザで http://localhost:8601 を開く
echo 4. Scratch拡張機能から「Minecraft コラボレーション」を追加
echo.
echo 停止するには各ウィンドウで Ctrl+C を押してください
echo.
pause