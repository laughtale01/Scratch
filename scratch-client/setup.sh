#!/bin/bash

set -e  # Exit on error

echo "========================================"
echo "MinecraftEdu Scratch Client - Setup"
echo "========================================"
echo ""

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "[1/4] Checking Node.js..."
if ! command -v node &> /dev/null; then
    echo "ERROR: Node.js is not installed or not in PATH"
    echo "Please install Node.js 16.x or higher from https://nodejs.org/"
    exit 1
fi

NODE_VERSION=$(node --version | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 16 ]; then
    echo "ERROR: Node.js 16 or higher is required"
    echo "Current version: $(node --version)"
    exit 1
fi
echo "Node.js: OK ($(node --version))"
echo ""

echo "[2/4] Installing Scratch VM dependencies..."
cd "$SCRIPT_DIR/scratch-vm"
npm install

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to install scratch-vm dependencies"
    exit 1
fi
echo "scratch-vm: OK"
echo ""

echo "[3/4] Installing Scratch GUI dependencies..."
cd "$SCRIPT_DIR/scratch-gui"
npm install

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to install scratch-gui dependencies"
    exit 1
fi
echo "scratch-gui: OK"
echo ""

echo "[4/4] Linking scratch-vm to scratch-gui..."
cd "$SCRIPT_DIR/scratch-vm"
npm link || echo "WARNING: npm link failed, continuing anyway..."

cd "$SCRIPT_DIR/scratch-gui"
npm link scratch-vm || echo "WARNING: npm link scratch-vm failed"
echo ""

echo "========================================"
echo "Setup Complete!"
echo "========================================"
echo ""
echo "Next steps:"
echo ""
echo "1. Start Scratch development server:"
echo "   cd scratch-gui"
echo "   npm start"
echo ""
echo "2. Open browser to http://localhost:8601/"
echo ""
echo "3. Add Minecraft extension from extension menu"
echo ""
echo "4. Connect to Minecraft server (make sure MOD is running)"
echo ""
