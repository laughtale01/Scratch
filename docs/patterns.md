# Implementation Patterns and Best Practices

## Overview
This document collects efficient implementation patterns discovered during the development of the Minecraft Collaboration System.

## WebSocket Communication Patterns

### Connection Management
```java
// Pattern: Graceful WebSocket initialization with error handling
public class WebSocketHandler {
    private WebSocketServer server;
    
    public void start() {
        try {
            // Check if WebSocket library is available
            Class.forName("org.java_websocket.server.WebSocketServer");
            
            server = new WebSocketServer(new InetSocketAddress(14711)) {
                @Override
                public void onOpen(WebSocket conn, ClientHandshake handshake) {
                    logger.info("New connection from: " + conn.getRemoteSocketAddress());
                }
                // ... other handlers
            };
            server.start();
        } catch (ClassNotFoundException e) {
            logger.error("WebSocket library not found!", e);
        }
    }
}
```

### Message Processing
```java
// Pattern: Command pattern for message handling
public interface CommandHandler {
    void handle(String[] args, WebSocket connection);
}

Map<String, CommandHandler> commandHandlers = new HashMap<>();
commandHandlers.put("invite", new InviteCommandHandler());
commandHandlers.put("visit", new VisitCommandHandler());
```

## Scratch Extension Patterns

### Block Definition Pattern
```javascript
// Pattern: Consistent block definition structure
{
    opcode: 'inviteFriend',
    blockType: BlockType.COMMAND,
    text: '📧 [FRIEND]さんを招待',
    arguments: {
        FRIEND: {
            type: ArgumentType.STRING,
            defaultValue: 'friend_name'
        }
    }
}
```

### WebSocket Client Pattern
```javascript
// Pattern: Robust WebSocket client with reconnection
class WebSocketClient {
    constructor(url) {
        this.url = url;
        this.reconnectInterval = 5000;
        this.shouldReconnect = true;
        this.connect();
    }
    
    connect() {
        this.ws = new WebSocket(this.url);
        this.ws.onclose = () => {
            if (this.shouldReconnect) {
                setTimeout(() => this.connect(), this.reconnectInterval);
            }
        };
    }
}
```

## Error Handling Patterns

### Defensive Programming
```java
// Pattern: Always validate inputs
public void processCommand(String command) {
    if (command == null || command.trim().isEmpty()) {
        logger.warn("Received empty command");
        return;
    }
    
    String[] parts = command.split("\\(");
    if (parts.length < 2) {
        logger.warn("Invalid command format: " + command);
        return;
    }
    // Process command...
}
```

### Child-Friendly Error Messages
```javascript
// Pattern: User-friendly error messages for children
showError(error) {
    const friendlyMessages = {
        'CONNECTION_FAILED': '❌ Minecraftに接続できません。Minecraftが起動しているか確認してください。',
        'TIMEOUT': '⏰ 接続がタイムアウトしました。もう一度試してください。',
        'INVALID_FRIEND': '❓ その友達は見つかりませんでした。名前を確認してください。'
    };
    
    return friendlyMessages[error.code] || '😔 エラーが発生しました。先生に相談してください。';
}
```

## Gradle Build Patterns

### Dependency Management
```gradle
// Pattern: Proper library inclusion for Forge mods
configurations {
    library
    implementation.extendsFrom library
}

dependencies {
    minecraft 'net.minecraftforge:forge:1.20.1-47.2.0'
    library 'org.java_websocket:Java-WebSocket:1.5.4'
}

// Include libraries in JAR
jar {
    from configurations.library.asFileTree.files.collect { zipTree(it) }
}
```

## Update History
- 2025-01-14: Initial patterns documented