#!/bin/bash

echo "========================================"
echo "MinecraftEdu MOD - Build Only"
echo "========================================"
echo ""

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
MOD_DIR="$(dirname "$SCRIPT_DIR")/minecraft-mod"

echo "Building MOD..."
echo "This may take 5-15 minutes on first build..."
echo ""

cd "$MOD_DIR"
chmod +x gradlew
./gradlew build

if [ $? -ne 0 ]; then
    echo ""
    echo "Build FAILED"
    exit 1
fi

echo ""
echo "========================================"
echo "Build SUCCESS!"
echo "========================================"
echo ""
echo "Built file location:"
find "$MOD_DIR/build/libs" -name "*.jar" -type f
echo ""
