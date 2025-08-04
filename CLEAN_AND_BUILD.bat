@echo off
echo ========================================
echo   クリーンビルド実行ツール
echo ========================================
echo.

cd /d D:\minecraft_collaboration_project\scratch-gui

echo [1/3] 古いフォルダを削除しています...

REM PowerShellを使って強制削除
powershell -Command "Remove-Item -Path '.\build' -Recurse -Force -ErrorAction SilentlyContinue"
powershell -Command "Remove-Item -Path '.\dist' -Recurse -Force -ErrorAction SilentlyContinue"

echo [2/3] クリーンアップ完了
echo.

echo [3/3] ビルドを実行しています...
call npm run build

if %errorlevel% neq 0 (
    echo.
    echo ========================================
    echo   ビルドエラーが発生しました
    echo ========================================
    echo.
    echo 代わりに開発サーバーを起動します...
    echo.
    call npm start
) else (
    echo.
    echo ========================================
    echo   ✅ ビルド成功！
    echo ========================================
    echo.
    echo ブラウザでScratchを開いています...
    start build\index.html
)

pause