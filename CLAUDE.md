# Claude Code Configuration

## 🎯 Project Overview (Updated 2025-08-08 - Phase 2 Stabilization)

This project uses the **official Scratch GUI fork** with a custom Minecraft extension, following the standard Scratch extension development approach.

### Architecture
- **scratch-gui/**: Official Scratch fork (v5.1.88) with Minecraft extension added
- **minecraft-mod/**: Original Minecraft Forge mod for WebSocket communication
- **scratch-extension/**: Source code for the Minecraft extension

## 🎉 Project Status Update (2025-08-08 - Latest)

### Phase 2 Stabilization Complete
- **CI/CD Pipeline**: GitHub Actions configured for automated builds
- **Java Environment**: Permanent Java 17 configuration via gradle.properties
- **Documentation**: Updated to reflect current state
- **Extension Consolidation**: Reduced from 23 variants to single unified version
- **Development Server**: Running on port 8601

### Known Issues Requiring Attention
- **Test Suite**: 316 compilation errors - requires major refactoring
- **Firebase Deployment**: Needs FIREBASE_TOKEN secret configuration
- **Production Verification**: Deployment status needs confirmation

## 📜 Previous Status (2025-08-08 Morning)

### 🚀 Major Architecture Enhancement
The project has evolved into an enterprise-grade system with comprehensive monitoring, predictive analytics, and advanced resource management capabilities. This positions the system for large-scale educational deployments.

### ✅ Implemented Features (Code Complete)
1. **WebSocket Communication** - Fully operational with reflection-based client handler
   - Fixed NoClassDefFoundError with reflection approach
   - Supports both JSON and legacy message formats
   - Verified with test-websocket.html debug tool
2. **Basic Commands** - All commands working and tested
   - ping → pong response ✅
   - chat → message display in Minecraft ✅
   - getPlayerPos → actual coordinates returned ✅
3. **Collaboration Features (Fully Implemented)**
   - Invitation system with expiration management
   - Visit requests with auto-teleportation
   - Home position tracking and return functionality
   - Emergency return with health/hunger/effects restoration
4. **Building Features (All Implemented)**
   - Circle creation (hollow structure)
   - Sphere creation (outer shell only)
   - Wall creation (with ground detection)
   - House building (with doors, windows, roof)
   - Area filling (3D range)
5. **Security Features (Fully Working)**
   - Rate limiting (10 commands/second with auto-reset)
   - IP restrictions (local network only)
   - Dangerous block/command filtering
   - Connection limit (max 10 connections)
6. **Single-player Support** - WebSocket server starts in client mode
7. **Quality Assurance System (NEW - 2025-07-29)**
   - Comprehensive test coverage: 136 tests (6,700% increase)
   - Security validation: XSS/SQL injection prevention verified
   - Authentication system: Token-based security implemented
   - Multi-language support: 7 languages implemented
   - Educational features: Block restrictions and difficulty levels implemented
8. **Enterprise Monitoring System (NEW - 2025-08-05)**
   - Advanced alert management with severity levels and lifecycle tracking
   - Predictive analytics using statistical analysis and anomaly detection
   - Real-time health monitoring of system resources
   - Multi-channel notification framework
   - Complex rule evaluation engine with trend analysis

### 📝 Implementation Summary
- **CollaborationManager**: Complete implementation with all features
- **CollaborationCoordinator**: Async processing with CompletableFuture
- **RateLimiter**: Full implementation with cleanup scheduler
- **Scratch Extension**: Cache-based real-time data synchronization
- **Building Functions**: All geometry calculations implemented
- **Test Infrastructure**: 7 comprehensive test classes (currently in test.disabled/)
  - AuthenticationManagerTest (22 tests)
  - InputValidatorTest (24 tests)
  - LanguageManagerTest (19 tests)
  - CollaborationCommandHandlerTest (21 tests)
  - CollaborationMessageProcessorTest (11 tests)
  - BlockPackManagerTest (13 tests)
  - IntegrationTest (26 tests)
  - **Note**: Tests need to be re-enabled by renaming test.disabled/ to test/

## 🚀 QUICK START FOR CLAUDE CODE

### 🔧 Java Environment (Updated 2025-08-08)
- **Required**: Java 17 (NOT Java 21)
- **Location**: `C:\Program Files\Java\jdk-17`
- **Helper Scripts**: 
  - `set-java17.bat` - Configure environment (general use)
  - `set-java17-vscode.bat` - Configure for VSCode terminal
- **Verification**: `java -version` should show "17.0.12"

### VSCode Integration (Updated 2025-08-08)
- **Optimized Settings**: `.vscode/settings.json` configured for Java 17
- **Task Runner**: Use `Ctrl+Shift+P` → `Tasks: Run Task` for automated builds
- **Terminal**: Git Bash configured as default with Java 17 path
- **Available Tasks**:
  - Build Minecraft Mod - Standard build
  - Build Mod (jarJar) - Deployment build
  - Deploy Mod to Minecraft - Auto-deploy to .minecraft/mods
  - Set Java 17 Environment - Configure Java environment

### Essential Steps:
1. **Set Java Environment**: Run `set-java17-vscode.bat` in VSCode terminal
2. **Scratch GUI Server**: Start with `cd scratch-gui && npm start` (port 8601)
3. **Build Minecraft Mod**: `cd minecraft-mod && ./gradlew jarJar -x test -x checkstyleMain`
4. **Deploy Mod**: Use VSCode task "Deploy Mod to Minecraft" or manual copy
5. **Access Scratch**: http://localhost:8601 → Extension button → Select "Minecraft コラボレーション"

### WebSocket Testing:
- Use `test-websocket.html` for detailed WebSocket debugging
- WebSocket port: 14711
- Connection status displayed in Minecraft with DebugMod

## 📚 Interactive Documentation Update System

This project adopts a systematic approach to manage knowledge gained during development and reflect it in existing documentation.

### Documents to Reference

Always check the following documents before starting any work:

#### 🔴 CRITICAL - Start Here:
- `docs/INDEX.md` - Master document index (START HERE)
- `README.md` - Project overview and current status
- `scratch-gui/README.md` - Scratch GUI fork documentation

#### 📊 Project Status:
- `PROJECT_STATUS_REPORT_2025-07-29.md` - Latest comprehensive status

#### 🔧 Development Resources:
- `docs/architecture.md` - Architecture design guidelines
- `docs/troubleshooting.md` - Troubleshooting guide
- `docs/patterns.md` - Implementation patterns
- `docs/dependencies.md` - Dependencies and APIs
- `docs/setup/UNIFIED_SETUP_GUIDE.md` - Setup instructions

#### 🛡️ Quality & Testing:
- `docs/tdd-guidelines.md` - TDD implementation guidelines
- `docs/regression-prevention.md` - Regression prevention checklist
- `docs/testing-guide.md` - Testing procedures

### 🚫 CRITICAL: Regression Prevention

**ABSOLUTE RULE: PREVENT ANY REGRESSION**

Claude Code must NEVER:
- Remove existing functionality without explicit user approval
- Modify code outside the intended scope
- Delete or alter working features during fixes
- Make changes that break existing tests

**Before ANY modification:**
1. Identify ALL existing functionality that might be affected
2. Create/run tests to verify current behavior
3. Explicitly confirm with user what should remain unchanged
4. Document what will NOT be modified in the implementation plan

**If unsure about impact:** STOP and ask for clarification rather than risk regression.

### 🧪 Test-Driven Development (TDD) - MANDATORY

**ALL development MUST follow strict TDD principles:**

#### Phase 1: Mock & Test Creation
1. **Create function mocks** with:
   - Clear function signatures
   - Expected input/output types
   - TODO list of implementation steps
2. **Write comprehensive tests** covering:
   - Happy path scenarios
   - Edge cases
   - Error conditions
3. **Verify all tests FAIL** (red phase)

#### Phase 2: Implementation
1. **Implement logic step by step** following the TODO list
2. **Run tests frequently** during implementation
3. **Ensure all tests PASS** (green phase)

#### Phase 3: Verification
1. **Run complete test suite**
2. **Verify NO regressions** in existing functionality
3. **Confirm ALL new tests pass**

**NO EXCEPTIONS**: This TDD flow must be strictly followed for every development task.

### Update Rules

#### Proposal Timing
Propose documentation updates in the following situations:
1. **After resolving errors or issues**
2. **When discovering efficient implementation patterns**
3. **When establishing new API/library usage patterns**
4. **When existing documentation is found outdated/incorrect**
5. **When discovering frequently referenced information**
6. **After completing code review corrections**

#### Proposal Format
💡 Documentation Update Proposal: [Situation description]
【Update Content】 [Specific additions/modifications]
【Update Candidates】
[File path 1] - [Reason]
[File path 2] - [Reason]
New file creation - [Reason]

Where should this be added? (Select number or skip)

#### Approval Process
1. User selects update destination
2. Display actual update content preview
3. User provides final approval (yes/edit/no)
4. Update file after approval

### Integration with Existing Documentation

- Follow existing format and style conventions
- Reference related existing content when applicable
- Include update history with dates (YYYY-MM-DD format)
- **ALL additions to CLAUDE.md and related files MUST be in English**

### Important Constraints

1. **Never update files without user approval**
2. **Only add content - never delete or modify existing content without explicit permission**
3. **Never record sensitive information (API keys, passwords, etc.)**
4. **Follow project conventions and style guides**
5. **Prevent any regression in existing functionality**
6. **Strictly follow TDD principles**

### Document Management

To prevent CLAUDE.md from becoming too large, split files according to these criteria:

- **When exceeding 100 lines**: Propose splitting related content into separate files
- **Recommended split structure**:
  - `docs/update-system.md` - Update system rules
  - `docs/project-specific.md` - Project-specific configurations
  - `docs/references.md` - List of reference documents
  - `docs/tdd-guidelines.md` - TDD implementation guidelines
  - `docs/regression-prevention.md` - Regression prevention checklist
- **Keep only overview and links in CLAUDE.md**: Move details to individual files

## Project-Specific Commands

### Scratch GUI (Official Fork)
- **Install Dependencies**: `cd scratch-gui && npm install`
- **Build**: `cd scratch-gui && npm run build`
- **Development Server**: `cd scratch-gui && npm start`
- **Access**: http://localhost:8601/

### Minecraft Mod Development
- **Build**: `cd minecraft-mod && ./gradlew build`
- **Run Client**: `cd minecraft-mod && ./gradlew runClient`
- **Clean Build**: `cd minecraft-mod && ./gradlew clean build`

### Deployment
- **Quick Deploy**: `deploy.bat` (Windows) or `./deploy.sh` (Mac/Linux)
- **Firebase Deploy**: `firebase deploy --only hosting`

### Key Ports
- **WebSocket Server**: 14711
- **Scratch GUI Dev Server**: 8601