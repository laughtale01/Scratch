#!/bin/bash
# Minecraft Collaboration Project - Deployment Script
# Usage: ./deploy.sh [version]

VERSION=${1:-1.0.0}
RELEASE_DIR="release/minecraft-collaboration-v${VERSION}"

echo "🚀 Starting deployment for version ${VERSION}..."

# Clean previous builds
echo "🧹 Cleaning previous builds..."
rm -rf release/
mkdir -p ${RELEASE_DIR}/{mods,scratch-extension,docs,config}

# Build Minecraft Mod
echo "🔨 Building Minecraft Mod..."
cd minecraft-mod
./gradlew clean build
if [ $? -ne 0 ]; then
    echo "❌ Minecraft Mod build failed!"
    exit 1
fi
cd ..

# Copy Minecraft Mod
echo "📦 Packaging Minecraft Mod..."
cp minecraft-mod/build/libs/minecraft-collaboration-mod-${VERSION}.jar ${RELEASE_DIR}/mods/

# Build Scratch Extension
echo "🔨 Building Scratch Extension..."
cd scratch-extension
npm install
npm run build
if [ $? -ne 0 ]; then
    echo "❌ Scratch Extension build failed!"
    exit 1
fi
cd ..

# Copy Scratch Extension
echo "📦 Packaging Scratch Extension..."
cp -r scratch-extension/dist/* ${RELEASE_DIR}/scratch-extension/ 2>/dev/null || mkdir -p ${RELEASE_DIR}/scratch-extension

# Copy Documentation
echo "📚 Copying documentation..."
cp -r docs/* ${RELEASE_DIR}/docs/
cp README.md ${RELEASE_DIR}/
cp DEPLOYMENT_GUIDE.md ${RELEASE_DIR}/

# Copy Configuration
echo "⚙️ Copying configuration..."
cat > ${RELEASE_DIR}/config/minecraft-collaboration.toml << EOF
[general]
  websocket_enabled = true
  websocket_port = 14711
  allow_external_connections = false
  rate_limit_per_second = 10
  max_connections = 10

[educational]
  classroom_mode_default = false
  restricted_blocks_enabled = true
  
[localization]
  default_language = "en_US"
EOF

# Create ZIP package
echo "📦 Creating release package..."
cd release
zip -r minecraft-collaboration-v${VERSION}.zip minecraft-collaboration-v${VERSION}/
cd ..

# Create checksums
echo "🔐 Generating checksums..."
cd release
sha256sum minecraft-collaboration-v${VERSION}.zip > minecraft-collaboration-v${VERSION}.zip.sha256
cd ..

echo "✅ Deployment package created successfully!"
echo "📦 Package location: release/minecraft-collaboration-v${VERSION}.zip"
echo ""
echo "📋 Next steps:"
echo "1. Test the package in a clean Minecraft installation"
echo "2. Create a GitHub release with tag v${VERSION}"
echo "3. Upload the ZIP file and checksum to the release"
echo "4. Update the documentation with the new version"