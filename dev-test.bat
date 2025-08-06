@echo off
echo ========================================
echo   テスト実行ツール
echo ========================================
echo.

echo テストの種類を選択してください：
echo 1. Minecraft MODテスト
echo 2. Scratch拡張機能テスト
echo 3. 統合テスト（全体）
echo 4. パフォーマンステスト
echo.
set /p choice="選択 (1-4): "

if "%choice%"=="1" goto minecraft_test
if "%choice%"=="2" goto scratch_test
if "%choice%"=="3" goto integration_test
if "%choice%"=="4" goto performance_test
goto invalid

:minecraft_test
echo.
echo Minecraft MODテストを実行中...
cd minecraft-mod
call gradlew.bat test
cd ..
goto end

:scratch_test
echo.
echo Scratch拡張機能テストを実行中...
cd scratch-gui
call npm test
cd ..
goto end

:integration_test
echo.
echo 統合テストを実行中...
cd minecraft-mod
call gradlew.bat test --tests "*IntegrationTest"
cd ..
goto end

:performance_test
echo.
echo パフォーマンステストを実行中...
cd minecraft-mod
call gradlew.bat test --tests "*PerformanceTest" "*BenchmarkTest"
cd ..
goto end

:invalid
echo 無効な選択です
goto end

:end
echo.
echo ========================================
echo   テスト実行完了
echo ========================================
echo.
echo テストレポート：
echo   minecraft-mod\build\reports\tests\test\index.html
echo.
pause