# Troubleshooting Guide

## Overview
This document provides solutions to common issues encountered during development and deployment of the Minecraft Collaboration System.

## WebSocket Issues

### ClassNotFoundException: org.java_websocket.server.WebSocketServer ✅ 解決済み

**問題**: Minecraft mod実行時にWebSocketサーバー起動でランタイムエラー
```
java.lang.ClassNotFoundException: org.java_websocket.server.WebSocketServer
```

**解決方法** (2025-01-25):
1. ✅ jarJar設定を有効化して依存関係を適切にパッケージング
2. ✅ シングルプレイヤーモードでのWebSocketサーバー初期化を追加
3. ✅ FMLClientSetupEventでサーバーを起動するように修正

**最終的な解決策**:
```gradle
// build.gradle
jarJar.enable()

dependencies {
    minecraft 'net.minecraftforge:forge:1.20.1-47.2.0'
    jarJar(group: 'org.java-websocket', name: 'Java-WebSocket', version: '[1.5.4,)')
    implementation "org.java-websocket:Java-WebSocket:1.5.4"
}
```

```java
// MinecraftCollaborationMod.java
@OnlyIn(Dist.CLIENT)
private void doClientStuff(final FMLClientSetupEvent event) {
    event.enqueueWork(() -> {
        if (collaborationServer == null) {
            collaborationServer = new CollaborationServer(WEBSOCKET_PORT, COLLABORATION_PORT, null);
            collaborationServer.start();
        }
    });
}
```

### Connection Refused: localhost:14711

**Problem**: Scratch extension cannot connect to Minecraft
**Solutions**:
1. Verify Minecraft is running with the mod loaded
2. Check Windows Firewall settings
3. Ensure port 14711 is not in use: `netstat -an | findstr 14711`
4. Run Minecraft with administrator privileges if needed

## Build Issues

### Gradle Build Failures

**Problem**: `./gradlew build` fails with Java version error
**Solution**:
```bash
# Check Java version
java -version

# For Minecraft 1.20.1, ensure Java 17 is used
# Update gradle.properties:
org.gradle.java.home=C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.15.6-hotspot
```

### Node.js Build Issues

**Problem**: `npm install` fails with permission errors
**Solutions**:
1. Run PowerShell as Administrator
2. Clear npm cache: `npm cache clean --force`
3. Delete node_modules and package-lock.json, then reinstall

## Runtime Issues

### Minecraft Crashes on Mod Load

**Problem**: Minecraft crashes when loading the collaboration mod
**Common Causes**:
1. Incompatible Forge version
2. Missing dependencies
3. Class loading conflicts

**Debug Steps**:
1. Check logs/latest.log for specific errors
2. Verify Forge version matches build.gradle
3. Test with minimal mod setup

### Scratch Blocks Not Appearing

**Problem**: Custom blocks don't show in Scratch
**Solutions**:
1. Ensure extension is properly built: `npm run build`
2. Check browser console for errors
3. Verify extension URL is correct
4. Clear browser cache and reload

## Development Environment Issues

### PowerShell Command Execution

**Problem**: Commands fail when copy-pasted in PowerShell
**Solution**: Type commands manually or use proper line continuation:
```powershell
./gradlew `
  clean `
  build
```

### Port Already in Use

**Problem**: "Address already in use" error
**Solution**:
```bash
# Windows: Find process using port
netstat -ano | findstr :14711

# Kill process by PID
taskkill /PID <process_id> /F
```

## Common Error Messages

### "WebSocket library not found!"
- **Meaning**: Java-WebSocket library not available at runtime
- **Action**: Review build.gradle dependency configuration

### "Connection timeout"
- **Meaning**: WebSocket connection attempt timed out
- **Action**: Check network settings and firewall

### "Invalid command format"
- **Meaning**: Malformed message from Scratch
- **Action**: Verify message format in Scratch extension

## Debug Tools and Commands

### Minecraft Mod Debugging
```bash
# Enable debug logging
./gradlew runClient --debug

# Check dependency tree
./gradlew dependencies

# Clean everything
./gradlew clean cleanCache
```

### Scratch Extension Debugging
```javascript
// Add debug logging
console.log('[MinecraftExt] Sending command:', command);

// Monitor WebSocket state
setInterval(() => {
    console.log('WebSocket state:', this.ws.readyState);
}, 5000);
```

## Update History
- 2025-01-14: Initial troubleshooting guide created
- 2025-01-25: WebSocket ClassNotFoundException問題を解決、jarJar設定とシングルプレイヤー対応を追加