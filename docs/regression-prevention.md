# Regression Prevention Checklist

## Overview
This document provides a comprehensive checklist and guidelines to prevent regression in the Minecraft Collaboration System. NEVER compromise existing functionality.

## Pre-Development Checklist

### 1. Understand Current Functionality
- [ ] List ALL features that might be affected
- [ ] Document current behavior with examples
- [ ] Identify dependent components
- [ ] Review existing tests

### 2. Impact Analysis
```markdown
## Impact Analysis Template
Feature: [Feature being modified]
Dependencies:
- [ ] Component A: [How it uses this feature]
- [ ] Component B: [How it uses this feature]

Potential Risks:
- [ ] Risk 1: [Description]
- [ ] Risk 2: [Description]

Mitigation:
- [ ] Mitigation 1: [Strategy]
- [ ] Mitigation 2: [Strategy]
```

### 3. Create Safety Net
- [ ] Write regression tests for current behavior
- [ ] Create integration tests for dependent features
- [ ] Set up continuous testing
- [ ] Document expected behaviors

## During Development Checklist

### Code Modification Rules
1. **Minimal Change Principle**
   - [ ] Change only what's necessary
   - [ ] Preserve existing interfaces
   - [ ] Maintain backward compatibility
   - [ ] Keep original logic when possible

2. **Continuous Validation**
   - [ ] Run tests after each change
   - [ ] Verify no unexpected test failures
   - [ ] Check for performance regression
   - [ ] Monitor memory usage

3. **Feature Flags (When Applicable)**
```java
public class FeatureFlags {
    // Use feature flags for risky changes
    public static final boolean USE_NEW_WEBSOCKET = 
        System.getProperty("collaboration.newWebSocket", "false").equals("true");
    
    public void handleConnection() {
        if (USE_NEW_WEBSOCKET) {
            // New implementation
        } else {
            // Original implementation preserved
        }
    }
}
```

## Critical Areas - Special Attention Required

### WebSocket Communication
**Current Functionality**: 
- Port 14711 for Scratch communication
- Message format: `command(args)`
- Auto-reconnection on disconnect

**Regression Tests Required**:
```java
@Test
public void testWebSocketBackwardCompatibility() {
    // Test old message format still works
    String oldFormat = "invite(player1)";
    handler.process(oldFormat);
    // Verify correct processing
}

@Test 
public void testPortCompatibility() {
    // Ensure port 14711 still works
    assertEquals(14711, server.getPort());
}
```

### Scratch Block Interface
**Current Functionality**:
- 12 implemented blocks with Japanese labels
- Specific opcodes that must not change
- Return value formats

**Protection Strategy**:
```javascript
// Never change existing opcodes
const PROTECTED_OPCODES = [
    'connectMinecraft',
    'inviteFriend', 
    'visitRequest',
    // ... other opcodes
];

// Validate no opcodes removed
function validateBlocks(blocks) {
    PROTECTED_OPCODES.forEach(opcode => {
        if (!blocks.find(b => b.opcode === opcode)) {
            throw new Error(`Protected opcode ${opcode} was removed!`);
        }
    });
}
```

### Player State Management
**Current Functionality**:
- Player position tracking
- World assignment
- Invitation system

**Regression Prevention**:
```java
public class PlayerState {
    // Preserve original state structure
    private final Map<String, Object> legacyState = new HashMap<>();
    
    // New features use separate storage
    private final Map<String, Object> extendedState = new HashMap<>();
    
    // Original methods unchanged
    public String getWorld() {
        return (String) legacyState.get("world");
    }
}
```

## Testing Strategy

### Regression Test Suite
```bash
# Create dedicated regression test suite
src/test/regression/
├── WebSocketRegressionTest.java
├── CommandProcessingRegressionTest.java
├── StateManagementRegressionTest.java
└── ScratchInterfaceRegressionTest.java
```

### Automated Regression Detection
```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RegressionTestSuite {
    private static Map<String, Object> baselineResults;
    
    @BeforeAll
    static void captureBaseline() {
        // Capture current behavior as baseline
        baselineResults = captureCurrentBehavior();
    }
    
    @Test
    @Order(1)
    void compareWebSocketBehavior() {
        Map<String, Object> current = captureWebSocketBehavior();
        assertEquals(baselineResults.get("websocket"), current);
    }
}
```

### Performance Regression Tests
```javascript
// Measure and compare performance
const performanceBaseline = {
    connectionTime: 100, // ms
    messageProcessing: 5, // ms
    memoryUsage: 50 * 1024 * 1024 // 50MB
};

test('Performance regression check', async () => {
    const start = Date.now();
    await extension.connect();
    const connectionTime = Date.now() - start;
    
    expect(connectionTime).toBeLessThan(performanceBaseline.connectionTime * 1.1);
});
```

## Post-Development Checklist

### Final Verification
- [ ] All regression tests pass
- [ ] No performance degradation
- [ ] Original features work identically
- [ ] New features don't interfere with old ones

### Documentation Update
- [ ] Update affected documentation
- [ ] Note any behavior changes
- [ ] Update API documentation
- [ ] Add migration guide if needed

### Rollback Plan
- [ ] Create rollback procedure
- [ ] Test rollback process
- [ ] Document rollback steps
- [ ] Identify rollback triggers

## Common Regression Scenarios

### Scenario 1: Library Updates
**Risk**: Updated library breaks existing functionality
**Prevention**:
```gradle
// Lock versions for critical dependencies
dependencies {
    implementation 'org.java_websocket:Java-WebSocket:1.5.4' // DO NOT UPDATE without extensive testing
}
```

### Scenario 2: Refactoring
**Risk**: Refactored code changes behavior
**Prevention**:
1. Use IDE refactoring tools
2. Run tests continuously
3. Refactor in small steps
4. Keep old code until new code is verified

### Scenario 3: Bug Fixes
**Risk**: Fix introduces new bugs
**Prevention**:
```java
// Always add regression test for bug fixes
@Test
public void testBugFix_Issue123_DoesNotBreakExistingFeature() {
    // Test that fix doesn't break related features
}
```

## Emergency Procedures

### If Regression Is Detected
1. **STOP** all development immediately
2. **REVERT** to last known good state
3. **ANALYZE** what went wrong
4. **FIX** with proper regression tests
5. **DOCUMENT** lessons learned

### Regression Hotfix Template
```java
public class RegressionHotfix {
    // Temporary flag to disable problematic code
    private static final boolean REGRESSION_DETECTED = true;
    
    public void riskyMethod() {
        if (REGRESSION_DETECTED) {
            // Fall back to original implementation
            originalImplementation();
            logger.warn("Using fallback due to regression");
        } else {
            // New implementation
            newImplementation();
        }
    }
}
```

## Continuous Monitoring

### Automated Checks
```yaml
# CI/CD regression check
regression-check:
  script:
    - ./gradlew regressionTest
    - npm run test:regression
  only:
    - merge_requests
    - main
```

### Metrics to Monitor
- Test execution time
- Memory usage
- Connection stability
- Error rates
- User-reported issues

## Update History
- 2025-01-14: Initial regression prevention checklist
- Focus on protecting existing WebSocket and Scratch functionality