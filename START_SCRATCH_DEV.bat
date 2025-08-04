@echo off
echo ========================================
echo   Scratch 開発サーバー起動
echo ========================================
echo.
echo ビルドをスキップして、開発サーバーで起動します。
echo これが最も簡単で確実な方法です。
echo.

cd /d D:\minecraft_collaboration_project\scratch-gui

echo 開発サーバーを起動しています...
echo （Ctrl+C で停止できます）
echo.
echo しばらくお待ちください（1-2分）...
echo ブラウザが自動的に開きます。
echo.

npm start

pause