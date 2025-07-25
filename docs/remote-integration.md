# Remote Repository Integration Guide

## Overview
This document outlines best practices for Git operations, branch strategies, and collaboration workflows for the Minecraft Collaboration System.

## Git Configuration

### Initial Setup
```bash
# Configure user information
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# Recommended Git settings
git config --global pull.rebase false  # Use merge for pulls
git config --global init.defaultBranch main
git config --global core.autocrlf true  # Windows line endings
```

### Repository Setup
```bash
# Initialize repository
git init
git remote add origin https://github.com/username/minecraft-collaboration.git

# Set up tracking
git branch --set-upstream-to=origin/main main
```

## Branch Strategy

### Branch Types

#### Main Branches
- `main` - Production-ready code
- `develop` - Integration branch for features

#### Supporting Branches
- `feature/*` - New features
- `bugfix/*` - Bug fixes
- `hotfix/*` - Emergency fixes
- `release/*` - Release preparation

### Branch Naming Convention
```
feature/add-invitation-system
bugfix/fix-websocket-connection
hotfix/emergency-return-crash
release/v1.0.0
```

### Branch Workflow
```bash
# Create feature branch
git checkout -b feature/add-invitation-system

# Work on feature
git add .
git commit -m "feat: add invitation system base structure"

# Keep branch updated
git checkout main
git pull origin main
git checkout feature/add-invitation-system
git merge main

# Push feature branch
git push -u origin feature/add-invitation-system
```

## Commit Message Convention

### Format
```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Test additions or modifications
- `chore`: Build process or auxiliary tool changes

### Examples
```bash
# Feature commit
git commit -m "feat(scratch): add emergency return block

- Add new Scratch block for emergency return
- Implement WebSocket command handling
- Add Japanese label with emoji"

# Bug fix commit
git commit -m "fix(websocket): resolve ClassNotFoundException

- Add proper library configuration in build.gradle
- Include dependencies in JAR file
- Add defensive loading check

Fixes #123"
```

## Pull Request Template

### PR Title Format
```
[Type] Brief description
```

Examples:
- `[Feature] Add invitation system for collaborative learning`
- `[Bugfix] Fix WebSocket connection timeout`
- `[Docs] Update setup instructions for Windows`

### PR Description Template
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update

## Testing
- [ ] All tests pass
- [ ] New tests added for new functionality
- [ ] Regression tests pass
- [ ] Manual testing completed

## Checklist
- [ ] My code follows the project style guidelines
- [ ] I have performed a self-review
- [ ] I have commented my code where necessary
- [ ] I have updated the documentation
- [ ] My changes generate no new warnings
- [ ] No existing functionality was broken

## Screenshots (if applicable)
[Add screenshots for UI changes]

## Related Issues
Closes #[issue number]
```

## Code Review Process

### Before Submitting PR
1. **Self-Review Checklist**
   - [ ] Run all tests locally
   - [ ] Check for code style violations
   - [ ] Remove debug code and console logs
   - [ ] Update relevant documentation
   - [ ] Verify no regression

2. **Clean Up Commits**
   ```bash
   # Interactive rebase to clean history
   git rebase -i HEAD~3
   
   # Squash related commits
   # Fix commit messages
   ```

### Review Guidelines
- Focus on functionality and correctness
- Check for potential regressions
- Verify test coverage
- Ensure documentation is updated
- Consider performance implications

### Responding to Reviews
```bash
# Make requested changes
git commit -m "refactor: address review comments

- Extract WebSocket logic to separate class
- Add error handling for edge cases
- Improve variable naming"

# Push changes
git push origin feature/branch-name
```

## CI/CD Integration

### GitHub Actions Configuration
```yaml
name: CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        
    - name: Set up Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '24.4.0'
        
    - name: Test Minecraft Mod
      run: |
        cd minecraft-mod
        ./gradlew test
        
    - name: Test Scratch Extension
      run: |
        cd scratch-extension
        npm install
        npm test
```

### Pre-commit Hooks
```bash
# .git/hooks/pre-commit
#!/bin/sh

# Run tests before commit
echo "Running tests..."
cd minecraft-mod && ./gradlew test
cd ../scratch-extension && npm test

# Check for debugging code
if grep -r "console.log\|System.out.println" --include="*.java" --include="*.js" .; then
    echo "Error: Debug statements found!"
    exit 1
fi
```

## Merge Strategies

### Feature to Develop
```bash
# Update feature branch
git checkout feature/my-feature
git pull origin develop
git merge develop

# Resolve conflicts if any
# Test thoroughly

# Merge to develop
git checkout develop
git merge --no-ff feature/my-feature
git push origin develop
```

### Develop to Main (Release)
```bash
# Create release branch
git checkout -b release/v1.0.0 develop

# Version bump and final testing
# Update version numbers
# Run comprehensive tests

# Merge to main
git checkout main
git merge --no-ff release/v1.0.0
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin main --tags

# Back-merge to develop
git checkout develop
git merge --no-ff release/v1.0.0
```

## Conflict Resolution

### Common Conflicts

#### build.gradle Dependencies
```gradle
<<<<<<< HEAD
    implementation 'org.java_websocket:Java-WebSocket:1.5.4'
=======
    implementation 'org.java_websocket:Java-WebSocket:1.5.5'
>>>>>>> feature/update-deps
```

Resolution strategy:
1. Check compatibility of both versions
2. Test with newer version
3. Keep newer version if tests pass

#### Package.json
Always run `npm install` after resolving package.json conflicts

### Resolution Process
```bash
# Start merge
git merge feature/branch

# If conflicts occur
# 1. Open conflicted files
# 2. Resolve conflicts manually
# 3. Test the resolution

# Mark as resolved
git add resolved-file.java
git commit -m "merge: resolve conflicts in WebSocket handler"
```

## Repository Maintenance

### Cleaning Up
```bash
# Delete merged local branches
git branch -d feature/completed-feature

# Delete remote branches
git push origin --delete feature/completed-feature

# Prune remote tracking branches
git remote prune origin
```

### Backup Strategy
```bash
# Create backup branch before risky operations
git branch backup/before-major-refactor

# Create patch files for important changes
git format-patch -1 HEAD
```

## Security Considerations

### Sensitive Information
- Never commit API keys or passwords
- Use environment variables for configuration
- Add sensitive files to .gitignore

### .gitignore Template
```gitignore
# Build outputs
build/
dist/
*.jar

# IDE files
.idea/
.vscode/
*.iml

# Logs
logs/
*.log

# Environment files
.env
*.properties

# OS files
.DS_Store
Thumbs.db
```

## Update History
- 2025-01-14: Initial remote integration guide
- Defined branch strategy and commit conventions
- Added PR template and CI/CD configuration