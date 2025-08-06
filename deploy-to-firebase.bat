@echo off
echo ========================================
echo Firebase デプロイツール
echo ========================================
echo.

echo [1] Scratch GUI をビルド中...
cd scratch-gui
call npm run build
if errorlevel 1 (
    echo エラー: ビルドに失敗しました
    pause
    exit /b 1
)

echo.
echo [2] ファイルをpublicフォルダにコピー中...
cd ..
powershell -Command "Remove-Item -Path 'public' -Recurse -Force -ErrorAction SilentlyContinue"
powershell -Command "Copy-Item -Path 'scratch-gui\build\*' -Destination 'public' -Recurse -Force"

echo.
echo [3] Firebaseにデプロイ中...
echo.
echo 注意: 以下のコマンドを手動で実行してください:
echo.
echo   1. firebase use laughtale-scratch-ca803
echo   2. firebase deploy --only hosting
echo.
echo もしプロジェクトが見つからない場合は:
echo   firebase use --add
echo   そして laughtale-scratch-ca803 を選択
echo.
echo ========================================
pause