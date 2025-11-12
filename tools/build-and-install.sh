#!/bin/bash

set -e  # Exit on error

echo "========================================"
echo "MinecraftEdu MOD - Build and Install"
echo "========================================"
echo ""

# Detect OS
if [[ "$OSTYPE" == "darwin"* ]]; then
    MINECRAFT_MODS="$HOME/Library/Application Support/minecraft/mods"
else
    MINECRAFT_MODS="$HOME/.minecraft/mods"
fi

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
MOD_DIR="$PROJECT_ROOT/minecraft-mod"

echo "[1/5] Checking Java..."
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH"
    echo "Please install Java JDK 17 and try again"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F. '{print $1}')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "ERROR: Java 17 or higher is required"
    echo "Current version: $JAVA_VERSION"
    exit 1
fi
echo "Java: OK (version $JAVA_VERSION)"
echo ""

echo "[2/5] Checking Gradle Wrapper..."
if [ ! -f "$MOD_DIR/gradlew" ]; then
    echo "ERROR: gradlew not found"
    echo "Please ensure you are in the correct directory"
    exit 1
fi
echo "Gradle Wrapper: OK"
echo ""

echo "[3/5] Building MOD..."
echo "This may take 5-15 minutes on first build..."
echo ""
cd "$MOD_DIR"
chmod +x gradlew
./gradlew build

if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: Build failed"
    echo "Check the error messages above"
    exit 1
fi

echo ""
echo "Build: SUCCESS"
echo ""

echo "[4/5] Locating built JAR file..."
JAR_FILE=$(find "$MOD_DIR/build/libs" -name "*.jar" -type f | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "ERROR: Built JAR file not found"
    echo "Expected location: $MOD_DIR/build/libs/"
    exit 1
fi
echo "Found: $JAR_FILE"
echo ""

echo "[5/5] Installing to Minecraft..."
if [ ! -d "$MINECRAFT_MODS" ]; then
    echo "Creating mods directory: $MINECRAFT_MODS"
    mkdir -p "$MINECRAFT_MODS"
fi

echo "Copying to: $MINECRAFT_MODS"
cp "$JAR_FILE" "$MINECRAFT_MODS/"

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to copy JAR file"
    echo "Please check permissions"
    exit 1
fi
echo ""

echo "========================================"
echo "Installation Complete!"
echo "========================================"
echo ""
echo "MOD installed to: $MINECRAFT_MODS"
echo ""
echo "Next steps:"
echo "1. Install Minecraft Forge 1.20.1 (if not already installed)"
echo "   Download from: https://files.minecraftforge.net/"
echo ""
echo "2. Launch Minecraft with forge-1.20.1 profile"
echo ""
echo "3. Check that MinecraftEdu MOD appears in the Mods menu"
echo ""
echo "4. Create/open a world and look for this message in logs:"
echo "   \"WebSocket server started on port 14711\""
echo ""
