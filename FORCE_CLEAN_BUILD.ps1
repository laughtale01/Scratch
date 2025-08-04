# PowerShell Script to force clean and rebuild
# 管理者権限で実行してください

Write-Host "================================" -ForegroundColor Cyan
Write-Host "  強制クリーン＆ビルドツール" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# scratch-guiディレクトリに移動
Set-Location -Path "D:\minecraft_collaboration_project\scratch-gui"

Write-Host "[1/5] プロセスを終了しています..." -ForegroundColor Yellow
# Node.jsとnpm関連のプロセスを終了
Get-Process node -ErrorAction SilentlyContinue | Stop-Process -Force
Get-Process npm -ErrorAction SilentlyContinue | Stop-Process -Force

# 少し待機
Start-Sleep -Seconds 2

Write-Host "[2/5] ファイルロックを解除しています..." -ForegroundColor Yellow
# Explorerでフォルダが開かれている場合、それも終了
$shell = New-Object -ComObject Shell.Application
$windows = $shell.Windows()
foreach ($window in $windows) {
    if ($window.LocationURL -like "*scratch-gui*") {
        $window.Quit()
    }
}

Write-Host "[3/5] buildとdistフォルダを削除しています..." -ForegroundColor Yellow
# フォルダを削除（複数の方法を試す）
if (Test-Path ".\build") {
    # 方法1: Remove-Item
    Remove-Item -Path ".\build" -Recurse -Force -ErrorAction SilentlyContinue
    
    # 方法2: cmd.exeのrmdir
    if (Test-Path ".\build") {
        cmd /c "rmdir /s /q build" 2>$null
    }
    
    # 方法3: .NET Framework
    if (Test-Path ".\build") {
        [System.IO.Directory]::Delete("$PWD\build", $true)
    }
}

if (Test-Path ".\dist") {
    Remove-Item -Path ".\dist" -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "[4/5] クリーンアップを確認しています..." -ForegroundColor Yellow
if (Test-Path ".\build") {
    Write-Host "⚠️ buildフォルダがまだ存在します。" -ForegroundColor Red
    Write-Host "以下を試してください：" -ForegroundColor Yellow
    Write-Host "1. PCを再起動する" -ForegroundColor White
    Write-Host "2. セーフモードで削除する" -ForegroundColor White
    Write-Host "3. 別のドライブにプロジェクトをコピーして試す" -ForegroundColor White
    exit 1
} else {
    Write-Host "✅ クリーンアップ完了！" -ForegroundColor Green
}

Write-Host "[5/5] ビルドを実行しています..." -ForegroundColor Yellow
npm run build

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "================================" -ForegroundColor Green
    Write-Host "  ✅ ビルド成功！" -ForegroundColor Green
    Write-Host "================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Scratchを開いています..." -ForegroundColor Cyan
    Start-Process ".\build\index.html"
} else {
    Write-Host ""
    Write-Host "================================" -ForegroundColor Red
    Write-Host "  ❌ ビルドに失敗しました" -ForegroundColor Red
    Write-Host "================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "開発サーバーで起動してみてください：" -ForegroundColor Yellow
    Write-Host "npm start" -ForegroundColor White
}

Write-Host ""
Write-Host "Enterキーを押して終了..." -ForegroundColor Gray
Read-Host