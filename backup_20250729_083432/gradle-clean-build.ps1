# PowerShell script to build Minecraft mod
Set-Location -Path $PSScriptRoot

Write-Host "Starting Minecraft Mod build..." -ForegroundColor Green
Write-Host "Current directory: $(Get-Location)"
Write-Host "JAVA_HOME: $env:JAVA_HOME"

# Run gradlew clean build
& .\gradlew.bat clean build

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build completed successfully!" -ForegroundColor Green
} else {
    Write-Host "Build failed with exit code: $LASTEXITCODE" -ForegroundColor Red
}

# Check if the jar file was created
$jarFile = "build\libs\minecraft-collaboration-mod-1.0.0.jar"
if (Test-Path $jarFile) {
    Write-Host "JAR file created: $jarFile" -ForegroundColor Green
} else {
    Write-Host "JAR file not found!" -ForegroundColor Red
}