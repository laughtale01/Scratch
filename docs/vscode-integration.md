# 📝 VSCode Integration Guide

**Created**: 2025-08-08  
**Version**: 1.0.0

## 🎯 Overview

This guide documents the VSCode integration optimizations implemented for the Minecraft×Scratch collaboration project. The integration ensures seamless development with automatic Java 17 configuration, one-click builds, and optimized workflows.

## 🔧 Configuration Files

### `.vscode/settings.json`
Main workspace settings with the following optimizations:
- **Java 17 Runtime**: Explicitly configured as default JDK
- **Terminal Integration**: Git Bash as default with Java 17 path injection
- **File Associations**: Proper syntax highlighting for all project files
- **Search Optimization**: Excludes node_modules but includes build artifacts for inspection

Key Features:
```json
{
  "java.configuration.runtimes": [{
    "name": "JavaSE-17",
    "path": "C:\\Program Files\\Java\\jdk-17",
    "default": true
  }],
  "terminal.integrated.env.windows": {
    "JAVA_HOME": "C:\\Program Files\\Java\\jdk-17"
  }
}
```

### `.vscode/tasks.json`
Automated build tasks with Java 17 auto-configuration:

#### Available Tasks
1. **Build Minecraft Mod** - Standard Gradle build
2. **Build Mod (jarJar)** - Creates deployable JAR with dependencies
3. **Deploy Mod to Minecraft** - Auto-copies to .minecraft/mods
4. **Clean Minecraft Mod** - Cleans build artifacts
5. **Test Minecraft Mod** - Runs test suite
6. **Run Minecraft Client** - Launches game with mod
7. **Start Scratch Dev Server** - Starts Scratch GUI on port 8601
8. **Set Java 17 Environment** - Configures Java environment

All Gradle tasks automatically use `set-java17-vscode.bat` to ensure correct Java version.

### `.vscode/launch.json`
Debug configurations for full-stack development:
- Minecraft Client debugging
- Scratch GUI debugging
- WebSocket connection testing
- Combined full-stack debugging

### `.vscode/extensions.json`
Recommended extensions for optimal development:
- Java Extension Pack
- ESLint & Prettier
- GitLens
- Markdown support
- Todo Tree

## 🚀 Quick Start

### Initial Setup
1. Open project in VSCode
2. Install recommended extensions when prompted
3. Open integrated terminal (Ctrl+`)
4. Run: `./set-java17-vscode.bat`

### Common Workflows

#### Build and Deploy Mod
1. Press `Ctrl+Shift+P`
2. Type "Tasks: Run Task"
3. Select "Build Mod (jarJar)"
4. Select "Deploy Mod to Minecraft"

#### Start Development Servers
1. Open terminal
2. Run Scratch GUI: `cd scratch-gui && npm start`
3. Launch Minecraft with deployed mod

#### Debug Full Stack
1. Press F5
2. Select "Full Stack Debug" configuration
3. Both Minecraft and Scratch will launch with debuggers attached

## 🔨 Helper Scripts

### `set-java17-vscode.bat`
Configures Java 17 environment specifically for VSCode terminal sessions:
- Sets JAVA_HOME to Java 17 installation
- Updates PATH to prioritize Java 17
- Configures Gradle to use Java 17
- Displays current Java version for verification

Usage:
```bash
# Run once per terminal session
./set-java17-vscode.bat

# Verify configuration
java -version  # Should show 17.0.12
```

## 🎮 Task Runner Commands

### Using VSCode Command Palette
1. `Ctrl+Shift+P` → "Tasks: Run Task"
2. Select desired task from list
3. Monitor output in integrated terminal

### Keyboard Shortcuts
- `Ctrl+Shift+B` - Run default build task (Build Minecraft Mod)
- `Ctrl+Shift+T` - Run test task

### Terminal Commands
All tasks can also be run directly:
```bash
# With automatic Java 17 configuration
cd minecraft-mod
../set-java17-vscode.bat && gradlew.bat jarJar

# Deploy to Minecraft
copy minecraft-mod\build\libs\*.jar %APPDATA%\.minecraft\mods\
```

## 🐛 Troubleshooting

### Java Version Issues
If you see Java 21 or other versions:
1. Run `set-java17-vscode.bat` in terminal
2. Restart VSCode if issues persist
3. Check JAVA_HOME environment variable

### Build Failures
1. Ensure Java 17 is active: `java -version`
2. Clean build: Run "Clean Minecraft Mod" task
3. Delete `.gradle` folder and rebuild

### Task Not Found
1. Ensure you're in project root
2. Reload window: `Ctrl+Shift+P` → "Developer: Reload Window"
3. Check `.vscode/tasks.json` exists

## 📊 Performance Tips

### Optimize Build Times
- Use "Build Mod (jarJar)" with `-x test -x checkstyleMain` flags
- Keep `.gradle` cache between sessions
- Use incremental builds when possible

### Memory Management
- Increase Gradle heap: Add `-Xmx2g` to gradle.properties
- Configure VSCode Java heap in settings
- Close unnecessary applications during builds

## 🔄 Updates and Maintenance

### Keeping Settings Current
- Review `.vscode/settings.json` after major Java updates
- Update paths if Java installation location changes
- Sync settings across team using version control

### Extension Updates
- Keep Java Extension Pack updated
- Review new recommended extensions quarterly
- Test compatibility after major VSCode updates

## 📚 Additional Resources

- [VSCode Java Documentation](https://code.visualstudio.com/docs/languages/java)
- [Gradle VSCode Integration](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-gradle)
- [Project README](../README.md)
- [CLAUDE.md Configuration](../CLAUDE.md)

---

## 🎯 Summary

The VSCode integration provides:
- ✅ Automatic Java 17 configuration
- ✅ One-click build and deployment
- ✅ Integrated debugging for full stack
- ✅ Optimized workspace settings
- ✅ Task automation for common workflows

This setup significantly improves development efficiency and reduces configuration errors.