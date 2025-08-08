@echo off
REM ===================================
REM Minecraft with Collaboration MOD Launcher
REM ===================================

echo ===================================
echo Minecraft Collaboration MOD Launcher
echo ===================================
echo.

REM Check if MOD is installed
if exist "%APPDATA%\.minecraft\mods\minecraft-collaboration-mod-1.0.0-all.jar" (
    echo [OK] MOD is installed
) else (
    echo [WARNING] MOD not found in mods folder
    echo Installing MOD...
    copy "minecraft-mod\build\libs\minecraft-collaboration-mod-1.0.0-all.jar" "%APPDATA%\.minecraft\mods\"
    echo MOD installed!
)

echo.
echo Starting Minecraft Launcher...
echo.
echo IMPORTANT REMINDERS:
echo 1. Select Forge 1.20.1 profile
echo 2. After loading, use /collab start to enable WebSocket
echo 3. Open Scratch at http://localhost:8602
echo.

REM Try to start Minecraft Launcher
if exist "%APPDATA%\.minecraft\launcher.exe" (
    start "" "%APPDATA%\.minecraft\launcher.exe"
) else if exist "C:\Program Files (x86)\Minecraft Launcher\MinecraftLauncher.exe" (
    start "" "C:\Program Files (x86)\Minecraft Launcher\MinecraftLauncher.exe"
) else if exist "C:\Program Files\Minecraft Launcher\MinecraftLauncher.exe" (
    start "" "C:\Program Files\Minecraft Launcher\MinecraftLauncher.exe"
) else (
    echo [ERROR] Minecraft Launcher not found!
    echo Please start Minecraft manually.
)

echo.
pause