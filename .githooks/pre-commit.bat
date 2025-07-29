@echo off
REM Pre-commit hook for Minecraft Collaboration Project (Windows)
REM Runs quality checks before allowing commit

echo Running pre-commit checks...

REM Check if we're in the right directory
if not exist "minecraft-mod\build.gradle" (
    echo Error: Not in project root directory
    exit /b 1
)

REM Check for changes in minecraft-mod
git diff --cached --name-only | findstr /B "minecraft-mod/" > nul
if %errorlevel% equ 0 (
    echo Checking Minecraft mod...
    
    cd minecraft-mod
    
    REM Run tests
    echo Running tests...
    call gradlew.bat test --quiet
    if %errorlevel% neq 0 (
        echo Tests failed! Please fix before committing.
        cd ..
        exit /b 1
    )
    echo Tests passed
    
    REM Run checkstyle
    echo Running Checkstyle...
    call gradlew.bat checkstyleMain --quiet
    if %errorlevel% neq 0 (
        echo Checkstyle warnings found. Consider fixing them.
    )
    
    REM Run SpotBugs
    echo Running SpotBugs...
    call gradlew.bat spotbugsMain --quiet
    if %errorlevel% neq 0 (
        echo SpotBugs warnings found. Consider fixing them.
    )
    
    cd ..
)

REM Check for changes in scratch-extension
git diff --cached --name-only | findstr /B "scratch-extension/" > nul
if %errorlevel% equ 0 (
    echo Checking Scratch extension...
    
    cd scratch-extension
    
    if exist "package.json" (
        REM Run lint if available
        npm run lint --if-present > nul 2>&1
        if %errorlevel% equ 0 (
            echo Running ESLint...
            call npm run lint
            if %errorlevel% neq 0 (
                echo Lint errors found! Please fix before committing.
                cd ..
                exit /b 1
            )
            echo Lint passed
        )
        
        REM Run tests if available
        npm test --if-present > nul 2>&1
        if %errorlevel% equ 0 (
            echo Running tests...
            call npm test
            if %errorlevel% neq 0 (
                echo Tests failed! Please fix before committing.
                cd ..
                exit /b 1
            )
            echo Tests passed
        )
    )
    
    cd ..
)

echo All pre-commit checks completed!
exit /b 0