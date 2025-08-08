@echo off
echo === Minecraft Mod Deployment ===
echo.

REM Delete old mod
echo Deleting old mod...
del "%APPDATA%\.minecraft\mods\minecraft-collaboration-mod-1.0.0-all.jar" 2>nul

REM Copy new mod
echo Copying new mod...
copy "minecraft-mod\build\libs\minecraft-collaboration-mod-1.0.0-all.jar" "%APPDATA%\.minecraft\mods\"

if %errorlevel%==0 (
    echo ✅ Mod deployed successfully!
    echo.
    echo 🎮 Please restart Minecraft to load the new mod with:
    echo    - security.authentication.enabled=false
    echo    - development.mode=true
) else (
    echo ❌ Failed to deploy mod
)

pause