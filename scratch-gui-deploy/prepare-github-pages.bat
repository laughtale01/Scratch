@echo off
echo 🌐 GitHub Pages用 Scratch GUI 準備スクリプト
echo ==========================================
echo.

REM 1. scratch-guiをクローン（まだない場合）
if not exist "scratch-gui" (
    echo 📥 Scratch GUIをクローン中...
    git clone https://github.com/scratchfoundation/scratch-gui.git
    cd scratch-gui
    git checkout scratch-desktop-v3.30.0
    cd ..
)

REM 2. 依存関係のインストール
echo 📦 依存関係をインストール中...
cd scratch-gui
call npm install
call npm install react-split-pane

REM 3. カスタマイズファイルをコピー
echo 📝 カスタマイズファイルを適用中...
if exist "..\scratch-gui-custom\src\components\gui\gui.jsx" (
    copy /Y "..\scratch-gui-custom\src\components\gui\gui.jsx" "src\components\gui\gui.jsx"
)

REM 4. 拡張機能の設定を更新（GitHub Pages用）
echo 🔧 拡張機能の設定を更新中...
cd ..
node update-extension-url.js

REM 5. プロダクションビルド
echo 🏗️ プロダクションビルドを実行中...
cd scratch-gui
call npm run build

REM 6. GitHub Pages用ディレクトリを作成
echo 📁 GitHub Pages用ディレクトリを準備中...
cd ..
if exist "docs" rmdir /s /q docs
mkdir docs

REM 7. ビルド済みファイルをコピー
echo 📋 ビルド済みファイルをコピー中...
xcopy /E /I /Y scratch-gui\build\* docs\

REM 8. GitHub Pages用のindex.htmlを調整
echo 🔧 index.htmlを調整中...
powershell -Command "(Get-Content docs\index.html) -replace '/static/', './static/' | Set-Content docs\index.html"

echo.
echo ✅ GitHub Pages用の準備が完了しました！
echo.
echo 📝 次のステップ:
echo 1. git add docs/
echo 2. git commit -m "Add Scratch GUI for GitHub Pages"
echo 3. git push origin main
echo 4. GitHubリポジトリの Settings > Pages で Source を "Deploy from a branch" に設定
echo 5. Branch を "main" の "/docs" に設定
echo.
pause