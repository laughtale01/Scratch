@echo off
echo ========================================
echo   開発環境セットアップツール
echo ========================================
echo.

REM Java確認
echo [1/6] Java環境を確認中...
java -version 2>nul
if %errorlevel% neq 0 (
    echo ❌ Javaがインストールされていません
    echo 👉 https://adoptium.net/ からJava 17をインストールしてください
    pause
    exit /b 1
)

REM Node.js確認
echo [2/6] Node.js環境を確認中...
node -v 2>nul
if %errorlevel% neq 0 (
    echo ❌ Node.jsがインストールされていません
    echo 👉 https://nodejs.org/ からインストールしてください
    pause
    exit /b 1
)

REM Git確認
echo [3/6] Git環境を確認中...
git --version 2>nul
if %errorlevel% neq 0 (
    echo ❌ Gitがインストールされていません
    echo 👉 https://git-scm.com/ からインストールしてください
    pause
    exit /b 1
)

REM VSCode拡張機能インストール
echo [4/6] VSCode拡張機能をインストール中...
code --install-extension vscjava.vscode-java-pack
code --install-extension esbenp.prettier-vscode
code --install-extension dbaeumer.vscode-eslint
code --install-extension eamodio.gitlens
code --install-extension yzhang.markdown-all-in-one
code --install-extension gruntfuggly.todo-tree

REM npm依存関係インストール
echo [5/6] Scratch GUI依存関係をインストール中...
cd scratch-gui
call npm install
cd ..

REM Gradleラッパー初期化
echo [6/6] Gradle環境を準備中...
cd minecraft-mod
call gradlew.bat --version
cd ..

echo.
echo ========================================
echo   ✅ 開発環境のセットアップ完了！
echo ========================================
echo.
echo 以下のコマンドが使用可能です：
echo   - dev-start.bat : 開発サーバー起動
echo   - dev-test.bat  : テスト実行
echo   - dev-build.bat : ビルド実行
echo   - dev-clean.bat : クリーンアップ
echo.
echo VSCodeで開く場合：
echo   code .
echo.
pause