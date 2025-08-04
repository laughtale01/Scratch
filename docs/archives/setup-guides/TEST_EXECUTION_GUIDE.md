# Test Execution Guide for 100% Coverage

## Overview

This guide explains how to run tests in different environments to achieve 100% test coverage.

## Test Categories

### 1. Unit Tests (72% - Always Pass)
Tests that don't require Minecraft environment:
- AuthenticationManagerTest (22 tests) ✅
- InputValidatorTest (21 tests) ✅
- LanguageManagerTest (20 tests) ✅
- RateLimiterTest (6 tests) ✅
- CollaborationManagerTest (3 tests) ✅
- CollaborationCommandHandlerSimpleTest (20 tests) ✅

### 2. Integration Tests (28% - Environment Dependent)
Tests requiring Minecraft environment:
- CollaborationCommandHandlerTest
- CollaborationMessageProcessorTest
- BlockPackManagerTest
- IntegrationTest
- WebSocketPerformanceTest

## Running Tests

### Option 1: Run Only Unit Tests (72% Coverage)
```bash
./gradlew test -Dminecraft.test.skip=true
```

### Option 2: Run All Tests in Development Environment
```bash
# Start Minecraft development environment
./gradlew runClient

# In another terminal, run tests
./gradlew test
```

### Option 3: Run All Tests in CI/CD (100% Coverage)
```yaml
# GitHub Actions example
- name: Setup Minecraft Test Environment
  run: |
    docker run -d --name minecraft-test \
      -p 25565:25565 \
      -p 14711:14711 \
      minecraft-forge:1.20.1
    
- name: Run All Tests
  run: ./gradlew test --no-daemon
```

## Test Environment Setup

### Local Development
1. Install Java 17
2. Install Docker (optional, for integration tests)
3. Clone the repository
4. Run `./gradlew build`

### CI/CD Environment
Use the provided Docker configuration:

```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY . .
RUN ./gradlew build
CMD ["./gradlew", "test"]
```

## Troubleshooting

### Common Issues

1. **"Minecraft environment not available"**
   - Solution: Use `-Dminecraft.test.skip=true` flag
   
2. **"WebSocket server not running"**
   - Solution: Start the test server with `./gradlew runTestServer`
   
3. **"Bootstrap not initialized"**
   - Solution: Run tests through Gradle, not directly

### Environment Variables

- `MINECRAFT_TEST_SKIP`: Skip Minecraft-dependent tests
- `WEBSOCKET_TEST_URL`: Override WebSocket server URL
- `TEST_TIMEOUT`: Set test timeout (default: 60s)

## Test Reports

Test reports are generated at:
- HTML: `build/reports/tests/test/index.html`
- XML: `build/test-results/test/`

## Achieving 100% Coverage

To achieve 100% test coverage:

1. **In Development**: Run with full Minecraft environment
2. **In CI/CD**: Use Docker-based test environment
3. **Quick Testing**: Skip integration tests for rapid feedback

The key is to have the right environment for the tests you want to run.