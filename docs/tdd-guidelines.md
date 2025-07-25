# Test-Driven Development (TDD) Guidelines

## Overview
This document provides comprehensive TDD guidelines for the Minecraft Collaboration System. ALL development MUST follow these TDD principles without exception.

## TDD Workflow - MANDATORY

### Phase 1: RED (Write Failing Tests)
1. **Understand Requirements**: Clearly define what needs to be built
2. **Write Test First**: Create test before any implementation
3. **Run Test**: Verify test fails (RED)
4. **Multiple Tests**: Write tests for happy path, edge cases, and errors

### Phase 2: GREEN (Make Tests Pass)
1. **Minimal Implementation**: Write just enough code to pass
2. **Run Tests**: Verify all tests pass (GREEN)
3. **No Over-Engineering**: Resist adding unnecessary features

### Phase 3: REFACTOR (Improve Code)
1. **Clean Code**: Refactor while keeping tests green
2. **Run Tests Again**: Ensure refactoring didn't break anything
3. **Commit**: Save your progress with all tests passing

## Java Testing (Minecraft Mod)

### Test Structure
```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class CollaborationCommandHandlerTest {
    private CollaborationCommandHandler handler;
    
    @BeforeEach
    void setUp() {
        handler = new CollaborationCommandHandler();
    }
    
    @Test
    @DisplayName("Should parse invite command correctly")
    void testParseInviteCommand() {
        // Given
        String command = "invite(steve)";
        
        // When
        ParsedCommand result = handler.parse(command);
        
        // Then
        assertEquals("invite", result.getCommand());
        assertEquals("steve", result.getArgs()[0]);
    }
    
    @Test
    @DisplayName("Should handle malformed commands gracefully")
    void testMalformedCommand() {
        // Given
        String command = "invalid_format";
        
        // When/Then
        assertThrows(InvalidCommandException.class, 
            () -> handler.parse(command));
    }
}
```

### Mock Creation Before Implementation
```java
// Step 1: Create interface/mock first
public interface WebSocketHandler {
    void send(String message);
    void close();
    boolean isOpen();
}

// Step 2: Write tests using mock
@Test
void testSendMessage() {
    // Given
    WebSocketHandler mockHandler = mock(WebSocketHandler.class);
    when(mockHandler.isOpen()).thenReturn(true);
    
    CollaborationServer server = new CollaborationServer(mockHandler);
    
    // When
    server.broadcast("Hello");
    
    // Then
    verify(mockHandler).send("Hello");
}

// Step 3: Implement real class only after tests are written
public class WebSocketHandlerImpl implements WebSocketHandler {
    // Implementation here...
}
```

## JavaScript Testing (Scratch Extension)

### Test Setup
```javascript
// test/setup.js
const { JSDOM } = require('jsdom');
const dom = new JSDOM();
global.window = dom.window;
global.document = window.document;
global.WebSocket = require('ws');

// test/MinecraftExtension.test.js
const test = require('node:test');
const assert = require('assert');
const MinecraftExtension = require('../src/index.js');

test('MinecraftExtension', async (t) => {
    await t.test('should connect to WebSocket', async () => {
        // Given
        const ext = new MinecraftExtension();
        
        // When
        const result = await ext.connect();
        
        // Then
        assert.strictEqual(ext.isConnected(), true);
    });
    
    await t.test('should handle connection failure', async () => {
        // Given
        const ext = new MinecraftExtension();
        ext.wsUrl = 'ws://invalid:0000';
        
        // When/Then
        await assert.rejects(
            ext.connect(),
            /Connection failed/
        );
    });
});
```

### Mock-First Development
```javascript
// Step 1: Define expected behavior with mocks
class MinecraftExtension {
    constructor() {
        // TODO: Initialize WebSocket connection
        // TODO: Set up message handlers
        // TODO: Implement reconnection logic
    }
    
    async connect() {
        // TODO: Create WebSocket connection
        // TODO: Wait for open event
        // TODO: Handle errors
        throw new Error('Not implemented');
    }
    
    async invite(friendName) {
        // TODO: Validate friend name
        // TODO: Send invite command
        // TODO: Wait for response
        throw new Error('Not implemented');
    }
}

// Step 2: Write comprehensive tests
// Step 3: Implement methods one by one
```

## Testing Best Practices

### Test Naming Convention
```java
// Java
@Test
@DisplayName("Should [expected behavior] when [condition]")
void should_ExpectedBehavior_When_Condition() {
    // Test implementation
}

// JavaScript
test('should [expected behavior] when [condition]', () => {
    // Test implementation
});
```

### Test Organization
```
src/
├── main/
│   └── java/
│       └── com/yourname/minecraftcollaboration/
└── test/
    └── java/
        └── com/yourname/minecraftcollaboration/
            ├── unit/           # Unit tests
            ├── integration/    # Integration tests
            └── e2e/           # End-to-end tests
```

### Edge Cases to Test

#### Network Issues
- Connection timeout
- Connection refused
- Unexpected disconnection
- Message corruption
- High latency

#### User Input
- Empty strings
- Special characters
- Very long strings
- Null/undefined values
- Injection attempts

#### State Management
- Concurrent operations
- State corruption
- Recovery from errors
- Memory leaks
- Race conditions

## Regression Prevention Testing

### Before ANY Modification
```javascript
// 1. Identify affected features
const affectedFeatures = [
    'WebSocket connection',
    'Message parsing',
    'Invitation system'
];

// 2. Write regression tests
affectedFeatures.forEach(feature => {
    test(`Regression: ${feature} should work as before`, () => {
        // Test current behavior
    });
});

// 3. Run all existing tests
// npm test

// 4. Only then make changes
```

### Continuous Regression Testing
```bash
# Run before any commit
npm test
./gradlew test

# Run specific test suites
npm test -- --grep "regression"
./gradlew test --tests "*RegressionTest"
```

## Test Data Management

### Test Fixtures
```java
public class TestFixtures {
    public static final String VALID_PLAYER_NAME = "Steve";
    public static final String INVALID_PLAYER_NAME = "";
    public static final String VALID_COMMAND = "invite(Steve)";
    public static final String MALFORMED_COMMAND = "invite(";
    
    public static WebSocket createMockConnection() {
        WebSocket mock = mock(WebSocket.class);
        when(mock.isOpen()).thenReturn(true);
        return mock;
    }
}
```

### Test Utilities
```javascript
// test/utils.js
function createMockWebSocket() {
    return {
        readyState: WebSocket.OPEN,
        send: jest.fn(),
        close: jest.fn(),
        addEventListener: jest.fn()
    };
}

function waitForConnection(ext, timeout = 5000) {
    return new Promise((resolve, reject) => {
        const start = Date.now();
        const check = () => {
            if (ext.isConnected()) {
                resolve();
            } else if (Date.now() - start > timeout) {
                reject(new Error('Connection timeout'));
            } else {
                setTimeout(check, 100);
            }
        };
        check();
    });
}

module.exports = { createMockWebSocket, waitForConnection };
```

## Testing Commands

### Java (Minecraft Mod)
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "CollaborationCommandHandlerTest"

# Run with coverage
./gradlew test jacocoTestReport

# Continuous testing
./gradlew test --continuous
```

### JavaScript (Scratch Extension)
```bash
# Run all tests
npm test

# Run with coverage
npm test -- --coverage

# Watch mode
npm test -- --watch

# Specific test file
npm test MinecraftExtension.test.js
```

## Test Coverage Requirements

### Minimum Coverage
- **Overall**: 80% minimum
- **Critical paths**: 95% minimum
- **Error handling**: 100% required

### Coverage Reports
```bash
# Generate coverage report
npm test -- --coverage
./gradlew jacocoTestReport

# View coverage
open coverage/index.html           # JavaScript
open build/reports/jacoco/test/html/index.html  # Java
```

## Anti-Patterns to Avoid

### ❌ Writing Tests After Implementation
```javascript
// WRONG: Implementation first
function processCommand(cmd) {
    // Complex implementation
}

// Then trying to add tests
test('processCommand works', () => {
    // Difficult to test, tightly coupled
});
```

### ❌ Testing Implementation Details
```java
// WRONG: Testing private methods
@Test
void testPrivateMethod() {
    // Don't test private methods directly
    // Test through public interface
}
```

### ❌ Skipping Edge Cases
```javascript
// WRONG: Only happy path
test('invite sends command', () => {
    extension.invite('friend');
    // What about null? Empty string? Special chars?
});
```

## Update History
- 2025-01-14: Initial TDD guidelines created
- Emphasized mandatory TDD workflow
- Added comprehensive examples for Java and JavaScript