#!/bin/bash
# Setup Git hooks for the project

echo "Setting up Git hooks..."

# Create .git/hooks directory if it doesn't exist
mkdir -p .git/hooks

# Copy pre-commit hook
if [ -f ".githooks/pre-commit" ]; then
    cp .githooks/pre-commit .git/hooks/pre-commit
    chmod +x .git/hooks/pre-commit
    echo "✅ Pre-commit hook installed"
else
    echo "❌ Pre-commit hook not found in .githooks/"
    exit 1
fi

# For Windows users, create a wrapper that calls the .bat file
if [ -f ".githooks/pre-commit.bat" ]; then
    cat > .git/hooks/pre-commit.cmd << 'EOF'
@echo off
call "%~dp0\..\..\\.githooks\pre-commit.bat"
EOF
    echo "✅ Windows pre-commit hook installed"
fi

echo ""
echo "Git hooks have been set up successfully!"
echo "The pre-commit hook will run automatically before each commit."
echo ""
echo "To skip hooks temporarily, use: git commit --no-verify"