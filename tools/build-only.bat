@echo off
echo ========================================
echo MinecraftEdu MOD - Build Only
echo ========================================
echo.

set "SCRIPT_DIR=%~dp0"
set "MOD_DIR=%SCRIPT_DIR%..\minecraft-mod"

echo Building MOD...
echo This may take 5-15 minutes on first build...
echo.

cd /d "%MOD_DIR%"
call gradlew.bat build

if errorlevel 1 (
    echo.
    echo Build FAILED
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build SUCCESS!
echo ========================================
echo.
echo Built file location:
dir /b "%MOD_DIR%\build\libs\*.jar"
echo.
echo Full path:
cd
echo \build\libs\
echo.
pause
