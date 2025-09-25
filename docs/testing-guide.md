# 🧪 Testing Guide - Minecraft Collaboration System

Comprehensive guide for testing, coverage measurement, and quality checks.

---

## 📋 Overview

This document provides procedures for test execution, coverage measurement, and quality validation for the Minecraft Collaboration educational software project.

---

## 🎯 Test Structure

### Test Categories

#### 1. **Unit Tests**
- Individual class and method testing
- Mockito for dependency mocking  
- Fast execution
- **Tag**: No specific tag (default)
- **Command**: `./gradlew unitTest`

#### 2. **Integration Tests**
- Multi-component interaction testing
- **Tag**: `@Tag("integration")`
- Actual workflow verification
- **Command**: `./gradlew safeIntegrationTest`

#### 3. **Performance Tests**
- BatchBlockPlacer performance measurement
- Large-scale data operation validation
- **Tag**: `@Tag("performance")`
- **Command**: `./gradlew performanceTest`

#### 4. **Minecraft-Dependent Tests**
- Tests requiring full Minecraft environment
- **Tag**: `@Tag("minecraft-dependent")`  
- **Status**: Separated due to environment complexity

#### 5. **Docker-Required Tests**
- Tests requiring Docker/TestContainers
- **Tag**: `@Tag("docker-required")`
- **Status**: Optional execution when Docker available

---

## 🚀 Test Execution Methods

### Quick Test Commands

```bash
cd minecraft-mod

# Fast unit tests only (recommended for development)
./gradlew unitTest

# Safe integration tests (excludes problematic environments)
./gradlew safeIntegrationTest

# Combined safe testing
./gradlew comprehensiveTest

# All tests including Docker tests (if available)
./gradlew fullTest
```

### Quality Gate Testing

```bash
# Quality Gate Level 1: Unit tests with success rate validation
./gradlew qualityGateLevel1

# Quality Gate Level 2: Integration tests + coverage
./gradlew qualityGateLevel2  

# Quality Gate Level 3: Full quality audit
./gradlew qualityGateLevel3

# Educational quality validation
./gradlew educationalQualityGate

# Comprehensive quality check
./gradlew comprehensiveQualityCheck
```

### Legacy Test Commands

```bash
# Original test execution (includes problematic tests)
./gradlew test

# All integration tests (may fail due to environment issues)
./gradlew integrationTest

# Specific test class execution
./gradlew test --tests "CollaborationManagerTest"
./gradlew test --tests "*WebSocket*"
```

### Test Report Access

```bash
# HTML report access (Windows)
start minecraft-mod\build\reports\tests\unitTest\index.html
start minecraft-mod\build\reports\tests\safeIntegrationTest\index.html

# HTML report access (Mac/Linux)
open minecraft-mod/build/reports/tests/unitTest/index.html
open minecraft-mod/build/reports/tests/safeIntegrationTest/index.html
```

---

## 📊 Coverage Measurement

### Coverage Report Generation

```bash
cd minecraft-mod

# Generate coverage report
./gradlew test jacocoTestReport

# Coverage with quality gate validation  
./gradlew qualityGateLevel2
```

### Coverage Report Access

```bash
# HTML coverage report (Windows)
start minecraft-mod\build\reports\jacoco\html\index.html

# HTML coverage report (Mac/Linux)  
open minecraft-mod/build/reports/jacoco/html/index.html
```

### Coverage Targets

- **Overall**: 70%+ (Warning threshold in quality gates)
- **Critical Classes**: 90%+
  - CollaborationManager
  - WebSocketHandler
  - CollaborationMessageProcessor
  - AuthenticationManager
  - RateLimiter

---

## ⚙️ Quality Assurance

### Code Quality Checks

```bash
# Checkstyle validation
./gradlew checkstyleMain

# PMD analysis
./gradlew pmdMain

# SpotBugs analysis
./gradlew spotbugsMain

# Combined quality check
./gradlew qualityCheck
```

### Quality Gate Standards

#### Level 1 (Commit Gate)
- ✅ Unit test success rate ≥ 85%
- ✅ Basic compilation successful
- ✅ No critical security issues

#### Level 2 (PR Gate)  
- ✅ Level 1 requirements
- ✅ Integration test success rate ≥ 80%
- ⚠️ Code coverage ≥ 70% (warning if below)

#### Level 3 (Release Gate)
- ✅ Level 2 requirements
- ⚠️ Checkstyle issues ≤ 50 (warning threshold)
- ⚠️ PMD violations ≤ 20 (warning threshold)
- ⚠️ SpotBugs issues ≤ 10 (warning threshold)

#### Educational Quality Gate
- ✅ Security implementation adequate
- ⚠️ Documentation coverage ≥ 60%
- ✅ Educational appropriateness validated

---

## 🧪 Test Environment Setup

### Prerequisites

#### Java Environment
- **Java 17** (Required - NOT Java 21)
- **JAVA_HOME** properly configured
- **PATH** includes Java 17 bin directory

#### Development Tools
- **Gradle**: Used via wrapper (`./gradlew`)
- **Git**: For version control integration
- **Docker**: Optional (for docker-required tests)

#### IDE Configuration
- **VSCode**: Optimized configuration in `.vscode/settings.json`
- **Tasks**: Predefined tasks for easy test execution
- **Terminal**: Git Bash configured as default

### Environment Validation

```bash
# Verify Java version
java -version
# Should show: 17.0.12

# Verify Gradle functionality
./gradlew --version

# Verify basic build
./gradlew compileJava

# Verify test environment
./gradlew unitTest --info
```

---

## 🔍 Troubleshooting

### Common Test Issues

#### 1. **"Task 'unitTest' not found"**
- **Solution**: Ensure using updated build.gradle with test separation
- **Fallback**: Use `./gradlew test -Dtest.tags=unit`

#### 2. **Java Version Conflicts**
- **Solution**: Run `set-java17-vscode.bat` in VSCode terminal
- **Verification**: `java -version` should show 17.x

#### 3. **Docker Integration Test Failures**
- **Expected**: Docker tests are optional and may fail without Docker
- **Solution**: Use `./gradlew safeIntegrationTest` instead

#### 4. **Minecraft Environment Errors**
- **Expected**: `NoClassDefFoundError: net.minecraft.world.level.block.Blocks`
- **Solution**: Use unit tests for development, integration tests for verification

#### 5. **Memory Issues**
- **Solution**: Increase heap size in gradle.properties:
  ```properties
  org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
  ```

### Test Performance Optimization

#### Parallel Execution
- Unit tests: Auto-configured for optimal parallelism
- Integration tests: Sequential to avoid resource conflicts

#### Resource Management
- ResourceManager handles cleanup automatically
- WebSocket connections properly closed after tests
- Thread pools shutdown gracefully

---

## 📈 Test Metrics & Reporting

### Current Test Status (as of 2025-08-08)

```
Unit Tests:        287 total, 257 passed (89.5% success)
Integration Tests: Safe subset, filtered for stability  
Performance Tests: Baseline established
Quality Gates:     Level 1-2 operational, Level 3 configured
Coverage:          Improving, target 70%+ overall
```

### Historical Improvements

- **2025-08-08**: Test separation strategy implemented
- **2025-08-05**: Quality gate system introduced  
- **2025-07-29**: Test infrastructure overhauled
- **2025-07-28**: Performance testing baseline

### Success Criteria

#### Development Workflow
- ✅ Fast unit tests (< 30 seconds for immediate feedback)
- ✅ Stable integration tests (no environment dependencies)
- ✅ Automated quality validation
- ✅ Clear failure diagnostics

#### Educational Requirements  
- ✅ Child-safe testing (no inappropriate content)
- ✅ Performance suitable for classroom use
- ✅ Reliable operation in school networks
- ✅ Teacher-friendly error messages

---

## 📚 Related Documentation

### Testing-Related Guides
- [TDD Guidelines](tdd-guidelines.md) - Test-driven development practices
- [Regression Prevention](regression-prevention.md) - Preventing regressions
- [Architecture](architecture.md) - System design for testability
- [Quality Gates](../minecraft-mod/quality-gates.gradle) - Automated quality validation

### Integration Guides  
- [Integration Test Guide](testing/INTEGRATION_TEST_GUIDE.md) - Detailed integration testing
- [VSCode Integration](vscode-integration.md) - IDE-integrated testing
- [Deployment Guide](DEPLOYMENT_GUIDE.md) - Production testing procedures

---

*Last Updated: 2025-08-08*  
*Language: English*  
*Testing Framework: JUnit 5 + Mockito + Gradle*