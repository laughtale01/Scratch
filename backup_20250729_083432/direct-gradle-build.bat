@echo off
setlocal

echo Setting up environment...
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.15.6-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo JAVA_HOME: %JAVA_HOME%
echo.

cd /d "%~dp0"
echo Current directory: %CD%
echo.

echo Checking Java version:
java -version 2>&1
echo.

echo Running Gradle build...
java -Xmx3G -Dfile.encoding=UTF-8 -Duser.country=US -Duser.language=en -Duser.variant -cp gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain clean build

echo.
echo Build process completed with exit code: %ERRORLEVEL%

endlocal
pause