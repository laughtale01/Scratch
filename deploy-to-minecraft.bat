@echo off
echo ========================================
echo Minecraft Collaboration Mod Deployment
echo ========================================
echo.

REM Check if Java 17 is available
echo Checking Java version...
java -version 2>&1 | findstr "17" > nul
if errorlevel 1 (
    echo [ERROR] Java 17 is required but not found!
    echo Please install Java 17 or run set-java17.bat first
    exit /b 1
)

REM Build the mod
echo.
echo Building the mod...
cd minecraft-mod
call gradlew.bat clean jarJar -x test -x checkstyleMain
if errorlevel 1 (
    echo [ERROR] Build failed!
    exit /b 1
)
cd ..

REM Check if build succeeded
if not exist "minecraft-mod\build\libs\minecraft-collaboration-mod-1.0.0-all.jar" (
    echo [ERROR] JAR file not found!
    exit /b 1
)

REM Get the Minecraft mods directory
set MINECRAFT_DIR=%APPDATA%\.minecraft
set MODS_DIR=%MINECRAFT_DIR%\mods

REM Create mods directory if it doesn't exist
if not exist "%MODS_DIR%" (
    echo Creating mods directory...
    mkdir "%MODS_DIR%"
)

REM Backup existing mod if present
if exist "%MODS_DIR%\minecraft-collaboration-mod-*.jar" (
    echo Backing up existing mod...
    for %%f in ("%MODS_DIR%\minecraft-collaboration-mod-*.jar") do (
        move "%%f" "%%f.backup" > nul
    )
)

REM Copy the new mod
echo.
echo Deploying mod to Minecraft...
copy "minecraft-mod\build\libs\minecraft-collaboration-mod-1.0.0-all.jar" "%MODS_DIR%\" /Y
if errorlevel 1 (
    echo [ERROR] Failed to copy mod to Minecraft!
    exit /b 1
)

echo.
echo ========================================
echo DEPLOYMENT SUCCESSFUL!
echo ========================================
echo.
echo Mod deployed to: %MODS_DIR%
echo.
echo Next steps:
echo 1. Start Minecraft with Forge 1.20.1
echo 2. The mod will load automatically
echo 3. Start Scratch GUI (cd scratch-gui && npm start)
echo 4. WebSocket server starts on port 14711
echo.
echo To test the connection:
echo - In Minecraft: Check for "WebSocket server started" message
echo - In Scratch: Load the Minecraft extension
echo.
pause