@echo off
cd /d "D:\minecraft_collaboration_project\minecraft-mod"

echo Downloading Gradle wrapper...
powershell -Command "& {Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-7.6.4-bin.zip' -OutFile 'gradle-7.6.4-bin.zip'}"

echo Extracting Gradle...
powershell -Command "& {Expand-Archive -Path 'gradle-7.6.4-bin.zip' -DestinationPath '.' -Force}"

echo Setting up Gradle wrapper...
if exist "gradle-7.6.4\bin\gradle.bat" (
    call gradle-7.6.4\bin\gradle.bat wrapper --gradle-version=7.6.4
    echo Gradle wrapper setup completed.
) else (
    echo ERROR: Gradle not found after extraction.
)

echo Cleaning up...
del gradle-7.6.4-bin.zip
rmdir /s /q gradle-7.6.4

pause