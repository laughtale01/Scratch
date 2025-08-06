@echo off
echo ========================================
echo   開発環境情報
echo ========================================
echo.

echo === システム情報 ===
echo OS: %OS%
echo Computer: %COMPUTERNAME%
echo User: %USERNAME%
echo.

echo === Java環境 ===
java -version 2>&1 | findstr /i "version"
echo JAVA_HOME: %JAVA_HOME%
echo.

echo === Node.js環境 ===
node -v
npm -v
echo.

echo === Git情報 ===
git --version
git config user.name
git config user.email
echo.

echo === プロジェクト情報 ===
echo プロジェクトパス: %CD%
echo.

echo === 利用可能なツール ===
echo ✅ dev-setup.bat   - 初期セットアップ
echo ✅ dev-start.bat   - 開発サーバー起動
echo ✅ dev-test.bat    - テスト実行
echo ✅ dev-build.bat   - ビルド実行
echo ✅ dev-clean.bat   - クリーンアップ
echo.

echo === VSCode拡張機能（推奨） ===
echo • Java Extension Pack
echo • Prettier
echo • ESLint
echo • GitLens
echo • Markdown All in One
echo • TODO Tree
echo.

echo === ポート使用状況 ===
netstat -an | findstr "14711"
if %errorlevel%==0 (
    echo ✅ WebSocket (14711) - 使用中
) else (
    echo ⭕ WebSocket (14711) - 未使用
)

netstat -an | findstr "8601"
if %errorlevel%==0 (
    echo ✅ Scratch Dev (8601) - 使用中
) else (
    echo ⭕ Scratch Dev (8601) - 未使用
)
echo.

pause