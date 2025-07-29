# Manual Gradle installation script
$gradleVersion = "7.6.4"
$gradleZip = "gradle-$gradleVersion-bin.zip"
$gradleUrl = "https://services.gradle.org/distributions/$gradleZip"
$gradleDir = "gradle-$gradleVersion"

Write-Host "Downloading Gradle $gradleVersion..." -ForegroundColor Green

# Download Gradle
Invoke-WebRequest -Uri $gradleUrl -OutFile $gradleZip

Write-Host "Extracting Gradle..." -ForegroundColor Green

# Extract Gradle
Expand-Archive -Path $gradleZip -DestinationPath "." -Force

# Remove zip file
Remove-Item $gradleZip

Write-Host "Gradle $gradleVersion has been downloaded to: $gradleDir" -ForegroundColor Green
Write-Host ""
Write-Host "To build the Minecraft mod, run:" -ForegroundColor Yellow
Write-Host "  cd minecraft-mod" -ForegroundColor Cyan
Write-Host "  ..\$gradleDir\bin\gradle.bat clean build" -ForegroundColor Cyan