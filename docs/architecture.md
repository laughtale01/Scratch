# Architecture Design Guidelines

## Overview
This document describes the architectural design of the Minecraft Collaboration System, focusing on the interaction between Minecraft Forge mod and Scratch 3.0 extension.

## System Architecture

### High-Level Architecture
```
┌─────────────────┐     WebSocket      ┌──────────────────┐
│ Scratch 3.0     │ ◄─────────────────► │ Minecraft 1.20.1 │
│ Extension        │     Port 14711     │ Forge Mod        │
└─────────────────┘                     └──────────────────┘
         │                                       │
         │                                       │
    JavaScript                              Java 17
    Node.js v24                         Forge 47.2.0
```

### Component Architecture

#### Minecraft Mod Components
```
MinecraftCollaborationMod/
├── Network Layer
│   ├── WebSocketHandler         # WebSocket server (port 14711)
│   └── CollaborationMessageProcessor # Message parsing & routing
├── Server Layer
│   ├── CollaborationServer      # Collaboration server (port 14712)
│   └── CollaborationCoordinator # World management & permissions
├── Command Layer
│   └── CollaborationCommandHandler # Command execution
└── Entity Layer (Future)
    └── CollaborationEntities    # Custom entities for visualization
```

#### Scratch Extension Components
```
MinecraftExtension/
├── Connection Manager
│   └── WebSocket Client         # Connection to Minecraft
├── Block Definitions
│   ├── Connection Blocks        # Connect/disconnect
│   ├── Invitation Blocks        # Friend invitations
│   ├── Visit Blocks            # World visits
│   └── Utility Blocks          # Chat, position, etc.
└── Message Handler
    └── Command Formatter        # Format messages for Minecraft
```

## Communication Protocol

### Message Format
```
command(arg1,arg2,arg3)
```

### Command Types
1. **Connection Commands**
   - `connect()` - Establish connection
   - `disconnect()` - Close connection
   - `ping()` - Keep-alive

2. **Collaboration Commands**
   - `invite(player_name)` - Invite player
   - `visit_request(world_name)` - Request visit
   - `approve_visit(visitor_name)` - Approve visit
   - `return_home()` - Return to own world
   - `emergency_return()` - Force return

3. **Utility Commands**
   - `get_position()` - Get player coordinates
   - `send_chat(message)` - Send chat message
   - `get_world()` - Get current world name

### Response Format
```json
{
  "type": "response|event|error",
  "command": "original_command",
  "data": {
    // Response data
  },
  "status": "success|error",
  "message": "Human readable message"
}
```

## Data Flow

### Connection Flow
```
1. Scratch Extension → WebSocket Connect → Minecraft Mod
2. Minecraft Mod → Validate → Send Welcome Message
3. Scratch Extension → Store Connection State
4. Both → Maintain heartbeat every 30s
```

### Invitation Flow
```
1. Player A (Scratch) → invite(Player B) → Minecraft
2. Minecraft → Validate permissions → Store invitation
3. Minecraft → Notify Player B → Event message
4. Player B (Scratch) → Show invitation notification
5. Player B → Accept/Reject → Update state
```

### Visit Flow
```
1. Visitor → visit_request(host_world) → Minecraft
2. Minecraft → Check invitation → Notify host
3. Host → approve_visit(visitor) → Minecraft
4. Minecraft → Teleport visitor → Update world state
5. Both players → Receive world change notification
```

## Security Architecture

### Access Control
- **Local Network Only**: Connections restricted to localhost/LAN
- **Invitation System**: No unsolicited connections
- **Permission Levels**:
  - Guest: Basic movement and chat
  - Friend: Build and interact
  - Host: Full control in their world

### Safety Features
- **Time Limits**: Configurable session duration
- **Emergency Return**: Instant return to home world
- **Teacher Override**: Admin commands for classroom management
- **Activity Logging**: All actions logged for review

## State Management

### Minecraft Mod State
```java
public class CollaborationState {
    // Player states
    Map<UUID, PlayerCollaborationData> playerStates;
    
    // Active invitations
    Map<String, Invitation> pendingInvitations;
    
    // World visit sessions
    Map<UUID, VisitSession> activeSessions;
    
    // Connection management
    Map<WebSocket, PlayerConnection> connections;
}
```

### Scratch Extension State
```javascript
class ExtensionState {
    constructor() {
        this.connected = false;
        this.currentWorld = 'my_world';
        this.invitations = [];
        this.visitRequests = [];
        this.wsConnection = null;
    }
}
```

## Scalability Considerations

### Current Limitations
- Single server instance
- Local network only
- Maximum 20 concurrent connections
- In-memory state storage

### Future Enhancements
1. **Distributed Architecture**: Multiple server support
2. **Persistent Storage**: Database for state management
3. **Cloud Integration**: Optional cloud relay for remote learning
4. **Load Balancing**: Distribution of collaboration sessions

## Error Handling Strategy

### Fail-Safe Principles
1. **Graceful Degradation**: Features disable cleanly on error
2. **User Notification**: Clear error messages for children
3. **Automatic Recovery**: Reconnection and state restoration
4. **Data Protection**: No data loss on disconnection

### Error Categories
- **Connection Errors**: Network issues, server down
- **Permission Errors**: Unauthorized actions
- **State Errors**: Invalid state transitions
- **Resource Errors**: Server capacity, memory

## Development Guidelines

### Adding New Features
1. Define Scratch block interface first
2. Design command protocol
3. Implement Minecraft handler
4. Add state management
5. Implement error handling
6. Write comprehensive tests
7. Update documentation

### Code Organization
- **Single Responsibility**: Each class has one purpose
- **Dependency Injection**: Loose coupling between components
- **Event-Driven**: Use events for cross-component communication
- **Immutable State**: Prefer immutable data structures

## Testing Architecture

### Unit Testing
- Mock WebSocket connections
- Test command parsing
- Validate state transitions
- Error handling verification

### Integration Testing
- Full connection lifecycle
- Multi-player scenarios
- Permission validation
- Performance testing

### End-to-End Testing
- Scratch to Minecraft flow
- Complete user workflows
- Error recovery scenarios
- Load testing

## Update History
- 2025-01-14: Initial architecture documentation
- Defined component structure and communication protocol