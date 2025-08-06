@echo off
echo ========================================
echo   開発環境クリーンアップツール
echo ========================================
echo.

echo 削除するものを選択してください：
echo 1. ビルド成果物のみ
echo 2. node_modules も含める（完全クリーン）
echo 3. キャッシュも含める（フルクリーン）
echo.
set /p choice="選択 (1-3): "

if "%choice%"=="1" goto clean_build
if "%choice%"=="2" goto clean_deps
if "%choice%"=="3" goto clean_all
goto invalid

:clean_build
echo.
echo ビルド成果物を削除中...
cd minecraft-mod
call gradlew.bat clean
cd ..\scratch-gui
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
cd ..
goto end

:clean_deps
echo.
echo ビルド成果物と依存関係を削除中...
cd minecraft-mod
call gradlew.bat clean
cd ..\scratch-gui
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
if exist node_modules rmdir /s /q node_modules
cd ..
goto end

:clean_all
echo.
echo すべてをクリーンアップ中...
cd minecraft-mod
call gradlew.bat clean
if exist .gradle rmdir /s /q .gradle
cd ..\scratch-gui
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
if exist node_modules rmdir /s /q node_modules
if exist .cache rmdir /s /q .cache
cd ..
goto end

:invalid
echo 無効な選択です
goto end

:end
echo.
echo ========================================
echo   クリーンアップ完了
echo ========================================
echo.
pause