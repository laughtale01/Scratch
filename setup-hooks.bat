@echo off
REM Setup Git hooks for the project (Windows)

echo Setting up Git hooks...

REM Create .git/hooks directory if it doesn't exist
if not exist ".git\hooks" mkdir ".git\hooks"

REM Copy pre-commit hook
if exist ".githooks\pre-commit.bat" (
    copy ".githooks\pre-commit.bat" ".git\hooks\pre-commit" > nul
    echo Pre-commit hook installed
) else (
    echo Error: Pre-commit hook not found in .githooks\
    exit /b 1
)

REM Also copy the Unix version for Git Bash users
if exist ".githooks\pre-commit" (
    copy ".githooks\pre-commit" ".git\hooks\pre-commit.sh" > nul
)

echo.
echo Git hooks have been set up successfully!
echo The pre-commit hook will run automatically before each commit.
echo.
echo To skip hooks temporarily, use: git commit --no-verify