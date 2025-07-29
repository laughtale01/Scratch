# PowerShell script to refactor package names from com.yourname to edu.minecraft.collaboration
# This script updates all Java files and related configurations

$oldPackage = "com.yourname.minecraftcollaboration"
$newPackage = "edu.minecraft.collaboration"
$oldPath = "com/yourname/minecraftcollaboration"
$newPath = "edu/minecraft/collaboration"

Write-Host "Package Name Refactoring Script" -ForegroundColor Green
Write-Host "==============================" -ForegroundColor Green
Write-Host "Old package: $oldPackage" -ForegroundColor Yellow
Write-Host "New package: $newPackage" -ForegroundColor Cyan
Write-Host ""

# Function to update file content
function Update-FileContent {
    param($file)
    
    $content = Get-Content $file -Raw
    $originalContent = $content
    
    # Replace package declaration
    $content = $content -replace "package $oldPackage", "package $newPackage"
    
    # Replace imports
    $content = $content -replace "import $oldPackage", "import $newPackage"
    
    # Replace in strings (for class names, etc.)
    $content = $content -replace "$oldPackage", "$newPackage"
    
    if ($content -ne $originalContent) {
        Set-Content -Path $file -Value $content -NoNewline
        Write-Host "Updated: $file" -ForegroundColor Green
        return $true
    }
    return $false
}

# Step 1: Create new directory structure
$baseDir = "D:\minecraft_collaboration_project\minecraft-mod"
$srcMainJava = "$baseDir\src\main\java"
$srcTestJava = "$baseDir\src\test\java"

$newMainDir = "$srcMainJava\edu\minecraft\collaboration"
$newTestDir = "$srcTestJava\edu\minecraft\collaboration"

Write-Host "`nStep 1: Creating new directory structure..." -ForegroundColor Cyan
New-Item -ItemType Directory -Path $newMainDir -Force | Out-Null
New-Item -ItemType Directory -Path $newTestDir -Force | Out-Null
Write-Host "Created: $newMainDir" -ForegroundColor Green
Write-Host "Created: $newTestDir" -ForegroundColor Green

# Step 2: Update all Java files
Write-Host "`nStep 2: Updating Java files..." -ForegroundColor Cyan
$javaFiles = Get-ChildItem -Path $baseDir -Filter "*.java" -Recurse
$updatedCount = 0

foreach ($file in $javaFiles) {
    if (Update-FileContent -file $file.FullName) {
        $updatedCount++
    }
}

Write-Host "Updated $updatedCount Java files" -ForegroundColor Green

# Step 3: Update build.gradle
Write-Host "`nStep 3: Updating build.gradle..." -ForegroundColor Cyan
$buildGradle = "$baseDir\build.gradle"
if (Test-Path $buildGradle) {
    Update-FileContent -file $buildGradle
}

# Step 4: Update configuration files
Write-Host "`nStep 4: Updating configuration files..." -ForegroundColor Cyan
$configFiles = @(
    "$baseDir\src\main\resources\META-INF\mods.toml",
    "$baseDir\gradle.properties"
)

foreach ($file in $configFiles) {
    if (Test-Path $file) {
        Update-FileContent -file $file
    }
}

# Step 5: Move files to new package structure
Write-Host "`nStep 5: Moving files to new package structure..." -ForegroundColor Cyan
Write-Host "This step requires manual execution to preserve file integrity" -ForegroundColor Yellow
Write-Host "Please run the following commands:" -ForegroundColor Yellow
Write-Host ""
Write-Host "# For main source files:" -ForegroundColor White
Write-Host "Move-Item -Path `"$srcMainJava\$oldPath\*`" -Destination `"$newMainDir`" -Force" -ForegroundColor Gray
Write-Host ""
Write-Host "# For test files:" -ForegroundColor White
Write-Host "Move-Item -Path `"$srcTestJava\$oldPath\*`" -Destination `"$newTestDir`" -Force" -ForegroundColor Gray
Write-Host ""
Write-Host "# Remove old directories:" -ForegroundColor White
Write-Host "Remove-Item -Path `"$srcMainJava\com`" -Recurse -Force" -ForegroundColor Gray
Write-Host "Remove-Item -Path `"$srcTestJava\com`" -Recurse -Force" -ForegroundColor Gray

Write-Host "`n==============================" -ForegroundColor Green
Write-Host "Package refactoring preparation complete!" -ForegroundColor Green
Write-Host "Please review the changes and run the move commands manually." -ForegroundColor Yellow