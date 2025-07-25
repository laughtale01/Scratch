@echo off
echo 🌐 Minecraft拡張機能サーバーを起動します...
echo.

cd scratch-extension

REM ビルド確認
if not exist "dist\minecraft-collaboration-extension.js" (
    echo 📦 拡張機能をビルド中...
    call npm run build
)

echo 🚀 HTTPサーバーを起動中 (ポート: 8000)...
echo.
echo アクセスURL: http://localhost:8000/dist/minecraft-collaboration-extension.js
echo.
echo 終了するには Ctrl+C を押してください
echo.

REM Python 3でHTTPサーバーを起動
python -m http.server 8000

REM Pythonがない場合はNode.jsのhttp-serverを使用
if errorlevel 1 (
    echo Python が見つかりません。Node.js の http-server を使用します...
    if not exist node_modules\http-server (
        call npm install -g http-server
    )
    http-server -p 8000 --cors
)