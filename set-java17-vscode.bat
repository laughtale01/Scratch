@echo off
REM VSCode内でClaude Code実行時のJava 17環境設定スクリプト
REM このスクリプトはVSCodeターミナル内でJava 17を有効にします

echo ========================================
echo Java 17 Environment Setup for VSCode
echo ========================================
echo.

REM Java 17のパスを設定
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

REM 現在のJavaバージョンを確認
echo Current Java Version:
java -version 2>&1
echo.

REM Gradleが正しいJavaを使用するように設定
set GRADLE_OPTS=-Dorg.gradle.java.home="%JAVA_HOME%"

echo Java 17 environment has been configured successfully!
echo.
echo JAVA_HOME: %JAVA_HOME%
echo.
echo You can now run Gradle commands with Java 17:
echo   cd minecraft-mod
echo   gradlew.bat build
echo.
echo ========================================