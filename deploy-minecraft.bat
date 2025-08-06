@echo off
REM Minecraft Collaboration Project - Deployment Script for Windows
REM Usage: deploy-minecraft.bat [version]

SET VERSION=%1
IF "%VERSION%"=="" SET VERSION=1.0.0
SET RELEASE_DIR=release\minecraft-collaboration-v%VERSION%

echo Starting deployment for version %VERSION%...

REM Clean previous builds
echo Cleaning previous builds...
IF EXIST release\ rmdir /S /Q release
mkdir %RELEASE_DIR%\mods
mkdir %RELEASE_DIR%\scratch-extension
mkdir %RELEASE_DIR%\docs
mkdir %RELEASE_DIR%\config

REM Build Minecraft Mod
echo Building Minecraft Mod...
cd minecraft-mod
call gradlew.bat clean build
IF %ERRORLEVEL% NEQ 0 (
    echo Minecraft Mod build failed!
    exit /b 1
)
cd ..

REM Copy Minecraft Mod
echo Packaging Minecraft Mod...
copy minecraft-mod\build\libs\minecraft-collaboration-mod-%VERSION%.jar %RELEASE_DIR%\mods\

REM Build Scratch Extension
echo Building Scratch Extension...
cd scratch-extension
call npm install
call npm run build
IF %ERRORLEVEL% NEQ 0 (
    echo Scratch Extension build failed!
    exit /b 1
)
cd ..

REM Copy Scratch Extension
echo Packaging Scratch Extension...
IF EXIST scratch-extension\dist xcopy /E /I scratch-extension\dist %RELEASE_DIR%\scratch-extension

REM Copy Documentation
echo Copying documentation...
xcopy /E /I docs %RELEASE_DIR%\docs
copy README.md %RELEASE_DIR%\
copy DEPLOYMENT_GUIDE.md %RELEASE_DIR%\

REM Copy Configuration
echo Copying configuration...
(
echo [general]
echo   websocket_enabled = true
echo   websocket_port = 14711
echo   allow_external_connections = false
echo   rate_limit_per_second = 10
echo   max_connections = 10
echo.
echo [educational]
echo   classroom_mode_default = false
echo   restricted_blocks_enabled = true
echo.
echo [localization]
echo   default_language = "en_US"
) > %RELEASE_DIR%\config\minecraft-collaboration.toml

REM Create ZIP package
echo Creating release package...
cd release
powershell Compress-Archive -Path minecraft-collaboration-v%VERSION% -DestinationPath minecraft-collaboration-v%VERSION%.zip
cd ..

REM Create checksums
echo Generating checksums...
cd release
certutil -hashfile minecraft-collaboration-v%VERSION%.zip SHA256 > minecraft-collaboration-v%VERSION%.zip.sha256
cd ..

echo.
echo Deployment package created successfully!
echo Package location: release\minecraft-collaboration-v%VERSION%.zip
echo.
echo Next steps:
echo 1. Test the package in a clean Minecraft installation
echo 2. Create a GitHub release with tag v%VERSION%
echo 3. Upload the ZIP file and checksum to the release
echo 4. Update the documentation with the new version
pause