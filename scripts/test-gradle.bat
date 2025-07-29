@echo off
echo Testing Gradle execution...
echo.
echo Current directory: %CD%
echo JAVA_HOME: %JAVA_HOME%
echo.

cd /d "D:\minecraft_collaboration_project\minecraft-mod"
echo Changed to: %CD%
echo.

echo Checking Java version:
java -version
echo.

echo Running Gradle wrapper:
call gradlew.bat --version

echo.
echo Test completed.