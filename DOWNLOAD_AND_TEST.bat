@echo off
echo ================================================
echo   Minecraft x Scratch 自動ダウンロード＆テスト
echo ================================================
echo.

echo GitHubからプロジェクトをダウンロードしています...
echo リポジトリ: https://github.com/laughtale01/Scratch
echo.

REM デスクトップに移動
cd /d %USERPROFILE%\Desktop

REM 既存のフォルダがあれば削除
if exist minecraft_collaboration_project (
    echo 既存のプロジェクトフォルダを削除しています...
    rmdir /s /q minecraft_collaboration_project
)

REM GitHubからクローン
echo [1/5] プロジェクトをダウンロード中...
git clone https://github.com/laughtale01/Scratch.git minecraft_collaboration_project
if errorlevel 1 (
    echo.
    echo ========================================
    echo エラー: ダウンロードに失敗しました
    echo ========================================
    echo Gitがインストールされているか確認してください
    echo インストール方法: https://git-scm.com/download/win
    pause
    exit /b 1
)

cd minecraft_collaboration_project

echo.
echo [2/5] Scratchの依存関係をインストール中...
cd scratch-gui
call npm install
if errorlevel 1 (
    echo.
    echo ========================================
    echo エラー: npm install が失敗しました
    echo ========================================
    echo Node.jsがインストールされているか確認してください
    echo インストール方法: https://nodejs.org/
    pause
    exit /b 1
)

echo.
echo [3/5] Scratchをビルド中...
call npm run build
if errorlevel 1 (
    echo エラー: Scratchのビルドが失敗しました
    pause
    exit /b 1
)
cd ..

echo.
echo [4/5] Minecraft MODをビルド中...
cd minecraft-mod
call gradlew.bat build
if errorlevel 1 (
    echo.
    echo ========================================
    echo エラー: MODのビルドが失敗しました
    echo ========================================
    echo Java 17がインストールされているか確認してください
    echo 確認コマンド: java -version
    pause
    exit /b 1
)
cd ..

echo.
echo [5/5] ブラウザでScratchを開いています...
start scratch-gui\build\index.html

echo.
echo ================================================
echo   ✅ セットアップ完了！
echo ================================================
echo.
echo 📋 次にやること：
echo.
echo 1. Minecraft Launcher を開く
echo 2. Forge 1.20.1 のプロファイルを選択
echo 3. 一度起動して、MODフォルダを作成
echo 4. 以下のMODファイルをMODフォルダにコピー：
echo    %USERPROFILE%\Desktop\minecraft_collaboration_project\minecraft-mod\build\libs\minecraft-collaboration-mod-1.0.0.jar
echo.
echo 5. Minecraftを再起動
echo 6. シングルプレイでワールドを作成
echo 7. チャット画面（Tキー）で以下のコマンドを実行：
echo    /collab start
echo.
echo 8. ブラウザのScratchで：
echo    - 左下の「拡張機能」ボタンをクリック
echo    - 「Minecraft コラボレーション」を選択
echo.
echo ================================================
echo 準備ができたらEnterキーを押してください
pause >nul