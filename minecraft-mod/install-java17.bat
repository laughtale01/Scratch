@echo off
echo Installing Java 17...

REM Download and install Eclipse Temurin JDK 17
echo Downloading Eclipse Temurin JDK 17...
curl -L -o temurin-jdk17.msi "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.12%2B7/OpenJDK17U-jdk_x64_windows_hotspot_17.0.12_7.msi"

if not exist temurin-jdk17.msi (
    echo Failed to download Java 17
    exit /b 1
)

echo Installing Java 17...
msiexec /i temurin-jdk17.msi /quiet ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome INSTALLDIR="C:\Program Files\Eclipse Adoptium\jdk-17.0.12.7-hotspot\"

echo Setting JAVA_HOME...
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.12.7-hotspot"

echo Updating PATH...
setx PATH "%PATH%;C:\Program Files\Eclipse Adoptium\jdk-17.0.12.7-hotspot\bin"

echo Java 17 installation completed
echo Please restart your command prompt to use the new Java version

REM Cleanup
del temurin-jdk17.msi

pause