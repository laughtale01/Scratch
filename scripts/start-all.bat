@echo off
echo 🎮 Minecraft協調学習システム - 統合起動
echo =====================================
echo.

REM 1. Minecraft拡張サーバーを新しいウィンドウで起動
echo 1️⃣ 拡張機能サーバーを起動中...
start "Extension Server" cmd /k start-extension-server.bat

REM 少し待機
timeout /t 3 /nobreak > nul

REM 2. Minecraftを新しいウィンドウで起動
echo 2️⃣ Minecraft (Mod付き) を起動中...
start "Minecraft" cmd /k run-minecraft.bat

REM 少し待機
timeout /t 5 /nobreak > nul

REM 3. Scratch GUIを起動（存在する場合）
if exist "scratch-gui" (
    echo 3️⃣ Scratch GUIを起動中...
    cd scratch-gui
    start "Scratch GUI" cmd /k npm start
    cd ..
) else (
    echo ⚠️ Scratch GUIがインストールされていません
    echo setup-scratch-gui.bat を実行してください
)

echo.
echo ✅ すべてのコンポーネントを起動しました！
echo.
echo 📝 使い方:
echo 1. Minecraftが完全に起動するまで待つ
echo 2. Scratch GUI (http://localhost:8601) を開く
echo 3. 拡張機能からMinecraftを選択
echo 4. プログラミングを開始！
echo.
echo 終了する場合は、各ウィンドウを個別に閉じてください
pause