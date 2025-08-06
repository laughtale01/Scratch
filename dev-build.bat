@echo off
echo ========================================
echo   プロジェクトビルドツール
echo ========================================
echo.

echo [1/3] ビルドディレクトリをクリーンアップ中...
cd minecraft-mod
call gradlew.bat clean
cd ..\scratch-gui
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
cd ..

echo.
echo [2/3] Minecraft MODをビルド中...
cd minecraft-mod
call gradlew.bat build
if %errorlevel% neq 0 (
    echo ❌ MODビルドに失敗しました
    cd ..
    pause
    exit /b 1
)
cd ..

echo.
echo [3/3] Scratch GUIをビルド中...
cd scratch-gui
call npm run build
if %errorlevel% neq 0 (
    echo ❌ Scratchビルドに失敗しました
    cd ..
    pause
    exit /b 1
)
cd ..

echo.
echo ========================================
echo   ✅ ビルド成功！
echo ========================================
echo.
echo 成果物の場所：
echo   MOD: minecraft-mod\build\libs\minecraft-collaboration-mod-1.0.0.jar
echo   Scratch: scratch-gui\build\index.html
echo.
pause