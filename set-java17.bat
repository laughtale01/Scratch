@echo off
REM Set Java 17 environment for Minecraft Collaboration Project
echo Setting Java 17 environment...

set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

echo JAVA_HOME set to: %JAVA_HOME%
echo.
echo Verifying Java version:
java -version
echo.
echo Java 17 environment configured successfully!