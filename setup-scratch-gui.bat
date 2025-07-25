@echo off
echo 🎮 Minecraft対応Scratch GUIセットアップ開始...

REM 1. Scratch GUIのクローン
if not exist "scratch-gui" (
    echo 📥 Scratch GUIをクローン中...
    git clone https://github.com/LLK/scratch-gui.git
    cd scratch-gui
    git checkout v3.0.0
) else (
    echo ✅ Scratch GUIは既に存在します
    cd scratch-gui
)

REM 2. カスタムファイルのコピー
echo 📋 カスタムファイルをコピー中...
xcopy /E /Y ..\scratch-gui-custom\src\* .\src\

REM 3. 依存関係のインストール
echo 📦 依存関係をインストール中...
call npm install
call npm install react-split-pane

REM 4. Minecraft拡張機能の登録案内
echo.
echo ⚠️ 手動で以下の作業を行ってください:
echo.
echo 1. src/lib/libraries/extensions/index.jsx を開く
echo 2. 以下を追加:
echo    import minecraft from './minecraft/index.js';
echo 3. export default [ の後に追加:
echo    minecraft,
echo.

REM 5. 完了メッセージ
echo ✅ セットアップ完了！
echo.
echo 📝 次のステップ:
echo 1. cd scratch-gui
echo 2. npm start
echo 3. ブラウザで http://localhost:8601 を開く
echo 4. 拡張機能からMinecraftを選択
echo.
echo ⚠️ 注意事項:
echo - Minecraft拡張サーバー（ポート8000）を起動してください
echo - Minecraft + Modが起動していることを確認してください
pause