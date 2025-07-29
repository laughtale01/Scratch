@echo off
chcp 65001 > nul
cls

echo.
echo  🎮 Minecraft × Scratch 簡単スタート 🎮
echo  ═══════════════════════════════════════════
echo.

REM 管理者権限チェック
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo  ⚠️  このスクリプトは管理者権限で実行してください
    echo.
    pause
    exit /b 1
)

echo  手順1: Minecraft準備中...
echo  ━━━━━━━━━━━━━━━━━━━━━━━━

REM JARファイルの確認とコピー
if exist "minecraft-mod\build\libs\minecraft-collaboration-mod-1.0.0-all.jar" (
    echo  ✅ Modファイル見つかりました
    
    REM modsフォルダの作成
    if not exist "%APPDATA%\.minecraft\mods" (
        mkdir "%APPDATA%\.minecraft\mods"
        echo  📁 modsフォルダを作成しました
    )
    
    REM 古いファイルを削除
    del /q "%APPDATA%\.minecraft\mods\minecraft-collaboration-*.jar" 2>nul
    
    REM 新しいファイルをコピー
    copy "minecraft-mod\build\libs\minecraft-collaboration-mod-1.0.0-all.jar" "%APPDATA%\.minecraft\mods\" >nul
    echo  ✅ Modをインストールしました
) else (
    echo  ❌ Modファイルが見つかりません
    echo     minecraft-mod\build\libs\minecraft-collaboration-mod-1.0.0-all.jar
    echo     先にビルドを実行してください
    pause
    exit /b 1
)

echo.
echo  手順2: 拡張機能サーバー起動中...
echo  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

REM Node.jsの確認
node --version >nul 2>&1
if %errorLevel% neq 0 (
    echo  ❌ Node.jsがインストールされていません
    echo     https://nodejs.org/ からダウンロードしてください
    pause
    exit /b 1
)

echo  ✅ Node.js確認完了

REM Scratch拡張のビルド確認
if not exist "scratch-extension\dist\minecraft-collaboration-extension.js" (
    echo  🔨 Scratch拡張をビルド中...
    cd scratch-extension
    call npm install
    call npm run build
    cd ..
    echo  ✅ ビルド完了
) else (
    echo  ✅ Scratch拡張準備完了
)

REM HTTPサーバーをバックグラウンドで起動
echo  🌐 HTTPサーバー起動中...
cd scratch-extension
start /B cmd /c "npx http-server dist -p 8080 --cors -s"
cd ..

REM サーバー起動待機
echo  ⏳ サーバー起動待機中...
timeout /t 3 /nobreak >nul

REM サーバー起動確認
powershell -Command "try { Invoke-WebRequest -Uri 'http://localhost:8080' -TimeoutSec 2 | Out-Null; exit 0 } catch { exit 1 }" >nul 2>&1
if %errorLevel% equ 0 (
    echo  ✅ HTTPサーバー起動完了
) else (
    echo  ⚠️  HTTPサーバーの起動確認ができませんでした
    echo     続行します...
)

echo.
echo  手順3: ブラウザでScratchページを開いています...
echo  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

REM 専用Minecraft×Scratchエディターを開く（ローカル版）
start "" "%CD%\scratch-minecraft.html"

echo.
echo  🌐 または、オンライン版も利用できます:
echo     https://laughtale01.github.io/Scratch/
echo     （GitHubでホストされた最新版）

echo.
echo  🎉 準備完了！
echo  ═══════════════════════════════════════════
echo.
echo  📝 使い方:
echo     1. Minecraftを起動してワールドに入る
echo     2. 開いたScratchエディターで左のブロックをドラッグ
echo     3. プログラムを組み立てて"▶️ 実行"ボタンをクリック
echo.
echo  🧱 基本的なプログラム:
echo     1. 🚩 緑の旗がクリックされたとき
echo     2. 🔌 Minecraftに接続する  
echo     3. 🧱 stone を X:0 Y:70 Z:0 に置く
echo.
echo  ❓ 問題がある場合:
echo     - Minecraft起動確認 → WebSocket server started メッセージ
echo     - ページ再読み込み → F5キー
echo     - スクリプト再実行 → このバッチファイル再実行
echo.

pause