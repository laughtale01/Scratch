Set-Location -Path "D:\minecraft_collaboration_project\minecraft-mod"
& ".\gradlew.bat" clean build
Write-Host "Build completed with exit code: $LASTEXITCODE"