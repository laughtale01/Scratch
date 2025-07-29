# Simple script to remove BOM from all Java and Gradle files

$projectRoot = "D:\minecraft_collaboration_project\minecraft-mod"
$fixedCount = 0

Write-Host "Removing BOM from Java and Gradle files..." -ForegroundColor Cyan

# Function to remove BOM from a file
function Remove-BOM {
    param($file)
    
    try {
        # Read file as bytes
        $bytes = [System.IO.File]::ReadAllBytes($file)
        
        # Check for UTF-8 BOM (EF BB BF)
        if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
            # Remove BOM and save
            $newBytes = $bytes[3..($bytes.Length-1)]
            [System.IO.File]::WriteAllBytes($file, $newBytes)
            Write-Host "Removed BOM from: $($file.Name)" -ForegroundColor Green
            return $true
        }
        return $false
    } catch {
        Write-Host "Error processing $($file.Name): $_" -ForegroundColor Red
        return $false
    }
}

# Process all Java files
$javaFiles = Get-ChildItem -Path $projectRoot -Filter "*.java" -Recurse
foreach ($file in $javaFiles) {
    if (Remove-BOM -file $file.FullName) {
        $fixedCount++
    }
}

# Process Gradle files
$gradleFiles = Get-ChildItem -Path $projectRoot -Filter "*.gradle" -File
foreach ($file in $gradleFiles) {
    if (Remove-BOM -file $file.FullName) {
        $fixedCount++
    }
}

Write-Host "`nBOM removal complete!" -ForegroundColor Green
Write-Host "Files fixed: $fixedCount" -ForegroundColor Yellow