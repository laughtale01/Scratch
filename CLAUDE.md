# Claude Code Configuration

## 🎉 Project Status Update (2025-01-25)

### ✅ Completed Features
1. **WebSocket Communication** - Fixed class loading issue with jarJar configuration
2. **Basic Commands** - Player position, chat, block operations
3. **Collaboration Features** - Invitation system, visit requests, world management
4. **Safety Features** - Return home, emergency return with full health restoration
5. **Single-player Support** - WebSocket server starts in client mode

### 📝 Implementation Summary
- Created `CollaborationManager` for managing invitations and visits
- Implemented `Invitation` and `VisitRequest` models
- Updated `CollaborationCommandHandler` with full feature implementation
- Added Japanese notifications for better user experience

## 📚 Interactive Documentation Update System

This project adopts a systematic approach to manage knowledge gained during development and reflect it in existing documentation.

### Documents to Reference

Always check the following documents before starting any work:

- `README.md` - Project overview, features, and setup instructions
- `HANDOVER_REPORT_2025-01-12.md` - Latest handover report with WebSocket issue details
- `docs/claude-code-handoff.md` - Claude Code handoff documentation
- `docs/architecture.md` - Architecture design guidelines (to be created)
- `docs/troubleshooting.md` - Troubleshooting guide (to be created)
- `docs/patterns.md` - Implementation patterns and best practices (to be created)
- `docs/dependencies.md` - Dependencies and API usage examples (to be created)
- `docs/tdd-guidelines.md` - TDD implementation guidelines (to be created)
- `docs/regression-prevention.md` - Regression prevention checklist (to be created)

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

### Minecraft Mod Development
- **Build**: `cd minecraft-mod && ./gradlew build`
- **Run Client**: `cd minecraft-mod && ./gradlew runClient`
- **Clean Build**: `cd minecraft-mod && ./gradlew clean build`

### Scratch Extension Development
- **Build**: `cd scratch-extension && npm run build`
- **Dev Mode**: `cd scratch-extension && npm run dev`
- **Install Dependencies**: `cd scratch-extension && npm install`

### Key Ports
- **WebSocket Server**: 14711
- **Collaboration Server**: 14712