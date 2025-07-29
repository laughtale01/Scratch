# Safe Package Refactoring Script for Minecraft Collaboration Project
# This script creates a backup before making changes

param(
    [switch]$DryRun = $false,
    [switch]$NoBackup = $false
)

$ErrorActionPreference = "Stop"

# Configuration
$projectRoot = "D:\minecraft_collaboration_project"
$modDir = "$projectRoot\minecraft-mod"
$oldPackage = "com.yourname.minecraftcollaboration"
$newPackage = "edu.minecraft.collaboration"
$oldPath = "com\yourname\minecraftcollaboration"
$newPath = "edu\minecraft\collaboration"

# Colors for output
function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args.Count -eq 0) {
        Write-Output ""
    } else {
        Write-Output $args[0]
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

Write-ColorOutput Green "========================================="
Write-ColorOutput Green "Safe Package Refactoring Script"
Write-ColorOutput Green "========================================="
Write-ColorOutput Yellow "Old package: $oldPackage"
Write-ColorOutput Cyan "New package: $newPackage"
if ($DryRun) {
    Write-ColorOutput Magenta "DRY RUN MODE - No changes will be made"
}
Write-ColorOutput Green "========================================="
Write-Host ""

# Step 1: Create backup
if (-not $NoBackup -and -not $DryRun) {
    $backupDir = "$projectRoot\backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
    Write-ColorOutput Cyan "Step 1: Creating backup..."
    
    try {
        Copy-Item -Path $modDir -Destination $backupDir -Recurse -Force
        Write-ColorOutput Green "Backup created: $backupDir"
    } catch {
        Write-ColorOutput Red "Failed to create backup: $_"
        exit 1
    }
} else {
    Write-ColorOutput Yellow "Step 1: Skipping backup (DryRun or NoBackup flag set)"
}

# Step 2: Analyze files to be changed
Write-Host ""
Write-ColorOutput Cyan "Step 2: Analyzing files..."

$javaFiles = Get-ChildItem -Path $modDir -Filter "*.java" -Recurse
$gradleFiles = Get-ChildItem -Path $modDir -Filter "*.gradle" -File
$propertyFiles = Get-ChildItem -Path $modDir -Filter "*.properties" -Recurse
$tomlFiles = Get-ChildItem -Path $modDir -Filter "*.toml" -Recurse

$allFiles = $javaFiles + $gradleFiles + $propertyFiles + $tomlFiles

Write-Host "Found files to process:"
Write-Host "  Java files: $($javaFiles.Count)"
Write-Host "  Gradle files: $($gradleFiles.Count)"
Write-Host "  Property files: $($propertyFiles.Count)"
Write-Host "  TOML files: $($tomlFiles.Count)"
Write-Host "  Total: $($allFiles.Count)"

# Step 3: Update file contents
Write-Host ""
Write-ColorOutput Cyan "Step 3: Updating file contents..."

$updatedFiles = 0
$errors = @()

foreach ($file in $allFiles) {
    try {
        $content = Get-Content $file.FullName -Raw
        $originalContent = $content
        
        # Replace package declarations
        $content = $content -replace "package\s+$oldPackage", "package $newPackage"
        
        # Replace imports
        $content = $content -replace "import\s+$oldPackage", "import $newPackage"
        
        # Replace string references
        $content = $content -replace [regex]::Escape($oldPackage), $newPackage
        
        # Replace path references (for resources)
        $content = $content -replace [regex]::Escape($oldPath.Replace('\', '/')), $newPath.Replace('\', '/')
        
        if ($content -ne $originalContent) {
            if (-not $DryRun) {
                Set-Content -Path $file.FullName -Value $content -NoNewline -Encoding UTF8
            }
            Write-Host "  Updated: $($file.Name)" -ForegroundColor Green
            $updatedFiles++
        }
    } catch {
        $errors += "Error updating $($file.FullName): $_"
        Write-Host "  Error: $($file.Name) - $_" -ForegroundColor Red
    }
}

Write-Host "Updated $updatedFiles files"

# Step 4: Create new directory structure
Write-Host ""
Write-ColorOutput Cyan "Step 4: Creating new directory structure..."

$newMainDir = "$modDir\src\main\java\$newPath"
$newTestDir = "$modDir\src\test\java\$newPath"

if (-not $DryRun) {
    New-Item -ItemType Directory -Path $newMainDir -Force | Out-Null
    New-Item -ItemType Directory -Path $newTestDir -Force | Out-Null
    Write-ColorOutput Green "Created directory structure"
}

# Step 5: Move files
Write-Host ""
Write-ColorOutput Cyan "Step 5: Moving files to new package structure..."

$oldMainDir = "$modDir\src\main\java\$oldPath"
$oldTestDir = "$modDir\src\test\java\$oldPath"

$movedFiles = 0

# Move main source files
if (Test-Path $oldMainDir) {
    $mainFiles = Get-ChildItem -Path $oldMainDir -Recurse -File
    foreach ($file in $mainFiles) {
        $relativePath = $file.FullName.Substring($oldMainDir.Length + 1)
        $newFilePath = Join-Path $newMainDir $relativePath
        
        if (-not $DryRun) {
            $newFileDir = Split-Path $newFilePath -Parent
            if (-not (Test-Path $newFileDir)) {
                New-Item -ItemType Directory -Path $newFileDir -Force | Out-Null
            }
            Move-Item -Path $file.FullName -Destination $newFilePath -Force
        }
        Write-Host "  Moved: $($file.Name)" -ForegroundColor Green
        $movedFiles++
    }
}

# Move test files
if (Test-Path $oldTestDir) {
    $testFiles = Get-ChildItem -Path $oldTestDir -Recurse -File
    foreach ($file in $testFiles) {
        $relativePath = $file.FullName.Substring($oldTestDir.Length + 1)
        $newFilePath = Join-Path $newTestDir $relativePath
        
        if (-not $DryRun) {
            $newFileDir = Split-Path $newFilePath -Parent
            if (-not (Test-Path $newFileDir)) {
                New-Item -ItemType Directory -Path $newFileDir -Force | Out-Null
            }
            Move-Item -Path $file.FullName -Destination $newFilePath -Force
        }
        Write-Host "  Moved: $($file.Name)" -ForegroundColor Green
        $movedFiles++
    }
}

Write-Host "Moved $movedFiles files"

# Step 6: Clean up old directories
if (-not $DryRun -and $movedFiles -gt 0) {
    Write-Host ""
    Write-ColorOutput Cyan "Step 6: Cleaning up old directories..."
    
    if (Test-Path "$modDir\src\main\java\com") {
        Remove-Item -Path "$modDir\src\main\java\com" -Recurse -Force
        Write-ColorOutput Green "Removed old main package directory"
    }
    
    if (Test-Path "$modDir\src\test\java\com") {
        Remove-Item -Path "$modDir\src\test\java\com" -Recurse -Force
        Write-ColorOutput Green "Removed old test package directory"
    }
}

# Summary
Write-Host ""
Write-ColorOutput Green "========================================="
Write-ColorOutput Green "Refactoring Summary"
Write-ColorOutput Green "========================================="
Write-Host "Files updated: $updatedFiles"
Write-Host "Files moved: $movedFiles"
Write-Host "Errors: $($errors.Count)"

if ($errors.Count -gt 0) {
    Write-Host ""
    Write-ColorOutput Red "Errors encountered:"
    foreach ($error in $errors) {
        Write-ColorOutput Red "  $error"
    }
}

if ($DryRun) {
    Write-Host ""
    Write-ColorOutput Magenta "DRY RUN COMPLETE - No actual changes were made"
    Write-ColorOutput Yellow "Run without -DryRun flag to apply changes"
} else {
    Write-Host ""
    Write-ColorOutput Green "Refactoring complete!"
    if (-not $NoBackup) {
        Write-ColorOutput Yellow "Backup saved to: $backupDir"
    }
    Write-ColorOutput Cyan "Next steps:"
    Write-ColorOutput White "1. Run 'gradle clean build' to verify the changes"
    Write-ColorOutput White "2. Run tests to ensure everything works correctly"
    Write-ColorOutput White "3. Update any external references to the old package name"
}

Write-Host ""