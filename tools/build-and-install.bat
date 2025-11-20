@echo off
setlocal enabledelayedexpansion

echo ========================================
echo MinecraftEdu MOD - Build and Install
echo ========================================
echo.

REM 現在のディレクトリを保存
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "MOD_DIR=%PROJECT_ROOT%\minecraft-mod"
set "MINECRAFT_MODS=%APPDATA%\.minecraft\mods"

echo [1/5] Checking Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java JDK 17 and try again
    pause
    exit /b 1
)
echo Java: OK
echo.

echo [2/5] Checking Gradle Wrapper...
if not exist "%MOD_DIR%\gradlew.bat" (
    echo ERROR: gradlew.bat not found
    echo Please ensure you are in the correct directory
    pause
    exit /b 1
)
echo Gradle Wrapper: OK
echo.

echo [3/5] Building MOD...
echo This may take 5-15 minutes on first build...
echo.
cd /d "%MOD_DIR%"
call gradlew.bat build
if errorlevel 1 (
    echo.
    echo ERROR: Build failed
    echo Check the error messages above
    pause
    exit /b 1
)
echo.
echo Build: SUCCESS
echo.

echo [4/5] Locating built JAR file...
set "JAR_FILE="
for %%F in ("%MOD_DIR%\build\libs\*.jar") do (
    set "JAR_FILE=%%F"
)

if not defined JAR_FILE (
    echo ERROR: Built JAR file not found
    echo Expected location: %MOD_DIR%\build\libs\
    pause
    exit /b 1
)
echo Found: !JAR_FILE!
echo.

echo [5/5] Installing to Minecraft...
if not exist "%MINECRAFT_MODS%" (
    echo Creating mods directory: %MINECRAFT_MODS%
    mkdir "%MINECRAFT_MODS%"
)

echo Copying to: %MINECRAFT_MODS%
copy /Y "!JAR_FILE!" "%MINECRAFT_MODS%\" >nul
if errorlevel 1 (
    echo ERROR: Failed to copy JAR file
    echo Please check permissions
    pause
    exit /b 1
)
echo.

echo ========================================
echo Installation Complete!
echo ========================================
echo.
echo MOD installed to: %MINECRAFT_MODS%
echo.
echo Next steps:
echo 1. Install Minecraft Forge 1.20.1 (if not already installed)
echo    Download from: https://files.minecraftforge.net/
echo.
echo 2. Launch Minecraft with forge-1.20.1 profile
echo.
echo 3. Check that MinecraftEdu MOD appears in the Mods menu
echo.
echo 4. Create/open a world and look for this message in logs:
echo    "WebSocket server started on port 14711"
echo.
pause
