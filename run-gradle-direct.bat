@echo off
cd /d "D:\minecraft_collaboration_project\minecraft-mod"
call direct-gradle-build.bat
exit /b %ERRORLEVEL%