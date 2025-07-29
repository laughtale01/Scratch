@echo off
cd minecraft-mod
call gradlew.bat clean build
echo Build completed with exit code: %ERRORLEVEL%
pause