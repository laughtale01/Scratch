@echo off
echo ========================================
echo   ビルドエラー修正ツール
echo ========================================
echo.

cd /d D:\minecraft_collaboration_project\scratch-gui

echo 古いビルドフォルダを削除しています...

REM buildフォルダを強制削除
if exist build (
    taskkill /F /IM node.exe 2>nul
    timeout /t 2 /nobreak >nul
    rmdir /s /q build 2>nul
    if exist build (
        echo buildフォルダを削除できません。手動で削除してください。
        echo フォルダ: D:\minecraft_collaboration_project\scratch-gui\build
        explorer D:\minecraft_collaboration_project\scratch-gui
        pause
    )
)

REM distフォルダを強制削除
if exist dist (
    rmdir /s /q dist 2>nul
)

echo.
echo クリーンアップ完了！
echo.
echo ビルドを開始します...
call npm run build

if errorlevel 1 (
    echo.
    echo ========================================
    echo   エラーが発生しました
    echo ========================================
    echo 以下を試してください：
    echo 1. すべてのブラウザを閉じる
    echo 2. Visual Studio Codeなどのエディタを閉じる
    echo 3. このバッチファイルを再度実行
    pause
) else (
    echo.
    echo ========================================
    echo   ✅ ビルド成功！
    echo ========================================
    echo.
    echo Scratchを開いています...
    start build\index.html
    echo.
    pause
)