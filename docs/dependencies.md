# Dependencies and API Usage Guide

## Overview
This document details all external dependencies used in the Minecraft Collaboration System and provides usage examples.

## Minecraft Mod Dependencies

### Core Dependencies

#### Minecraft Forge
- **Version**: 1.20.1-47.2.0
- **Purpose**: Mod loader and API
- **Configuration**:
```gradle
dependencies {
    minecraft 'net.minecraftforge:forge:1.20.1-47.2.0'
}
```

#### Java-WebSocket
- **Version**: 1.5.4
- **Purpose**: WebSocket server implementation
- **Issues**: Runtime ClassNotFoundException (under investigation)
- **Usage Example**:
```java
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class CollaborationWebSocketServer extends WebSocketServer {
    public CollaborationWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        // Process commands from Scratch
    }
}
```

#### SLF4J Logging
- **Version**: 2.0.9
- **Purpose**: Logging framework
- **Usage**:
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClass {
    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);
    
    public void doSomething() {
        logger.info("Doing something");
        logger.error("Error occurred", exception);
    }
}
```

### Build Dependencies

#### ForgeGradle
- **Version**: 5.1.+
- **Purpose**: Gradle plugin for Forge development
- **Configuration**:
```gradle
buildscript {
    repositories {
        maven { url = 'https://maven.minecraftforge.net' }
        mavenCentral()
    }
    dependencies {
        classpath group: 'net.minecraftforge.gradle', name: 'ForgeGradle', version: '5.1.+', changing: true
    }
}
```

## Scratch Extension Dependencies

### Runtime Dependencies

#### ws (WebSocket)
- **Version**: 8.18.0
- **Purpose**: WebSocket client for Node.js
- **Usage**:
```javascript
const WebSocket = require('ws');

class MinecraftConnection {
    constructor() {
        this.ws = new WebSocket('ws://localhost:14711');
        
        this.ws.on('open', () => {
            console.log('Connected to Minecraft');
        });
        
        this.ws.on('message', (data) => {
            console.log('Received:', data.toString());
        });
    }
    
    send(command) {
        if (this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(command);
        }
    }
}
```

### Development Dependencies

#### Webpack
- **Version**: 5.97.1
- **Purpose**: Module bundler
- **Configuration**: See webpack.config.js

#### Babel
- **Version**: 7.x
- **Purpose**: JavaScript transpiler
- **Presets**:
  - @babel/preset-env
  - @babel/preset-react

#### ESLint
- **Version**: 8.57.1
- **Purpose**: Code linting
- **Configuration**:
```json
{
    "extends": "scratch",
    "env": {
        "browser": true,
        "es6": true
    }
}
```

## API Usage Examples

### Minecraft Forge Events
```java
@Mod.EventBusSubscriber(modid = MinecraftCollaborationMod.MODID)
public class EventHandler {
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        // Handle player join
    }
    
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // Initialize collaboration server
    }
}
```

### Scratch Extension API
```javascript
class MinecraftExtension {
    getInfo() {
        return {
            id: 'minecraftcollab',
            name: 'Minecraft協調学習',
            blocks: [
                {
                    opcode: 'connect',
                    blockType: BlockType.COMMAND,
                    text: '🔌 Minecraftに接続'
                }
            ]
        };
    }
    
    connect() {
        return new Promise((resolve) => {
            this.ws = new WebSocket('ws://localhost:14711');
            this.ws.onopen = () => resolve();
        });
    }
}
```

## Dependency Management Best Practices

### Version Pinning
Always specify exact versions for production dependencies:
```json
{
  "dependencies": {
    "ws": "8.18.0"  // Good: exact version
    // "ws": "^8.18.0"  // Avoid: can introduce breaking changes
  }
}
```

### Dependency Updates
1. Test thoroughly before updating major versions
2. Check changelogs for breaking changes
3. Update one dependency at a time
4. Run full test suite after updates

### Security Considerations
- Regularly run `npm audit` for JavaScript dependencies
- Monitor security advisories for Java dependencies
- Never commit sensitive data in dependency configurations

## Troubleshooting Dependency Issues

### Maven Repository Issues
If dependencies fail to download:
```gradle
repositories {
    // Add backup repositories
    maven { url = 'https://maven.minecraftforge.net' }
    maven { url = 'https://repo1.maven.org/maven2/' }
    mavenCentral()
    mavenLocal()
}
```

### Node Module Issues
```bash
# Clear everything and reinstall
rm -rf node_modules package-lock.json
npm cache clean --force
npm install
```

## Update History
- 2025-01-14: Initial dependency documentation
- Listed all current dependencies with usage examples