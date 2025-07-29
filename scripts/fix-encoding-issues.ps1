# Script to fix BOM and encoding issues in Java files

$projectRoot = "D:\minecraft_collaboration_project\minecraft-mod"
$fixedCount = 0
$errorCount = 0

Write-Host "Fixing encoding issues in Java files..." -ForegroundColor Cyan

# Find all Java files
$javaFiles = Get-ChildItem -Path $projectRoot -Filter "*.java" -Recurse

foreach ($file in $javaFiles) {
    try {
        # Read file content as bytes
        $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
        
        # Check for BOM (UTF-8: EF BB BF)
        $hasBOM = $false
        if ($bytes.Length -ge 3) {
            if ($bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
                $hasBOM = $true
            }
        }
        
        # Read content
        if ($hasBOM) {
            # Skip BOM bytes
            $content = [System.Text.Encoding]::UTF8.GetString($bytes, 3, $bytes.Length - 3)
        } else {
            $content = [System.Text.Encoding]::UTF8.GetString($bytes)
        }
        
        # Check if content contains corrupted characters
        $needsFix = $hasBOM -or ($content -match '[・ｿ|・ｽ|・・|・ｴ|・ｳ|ﾂｧ|ｼ・]')
        
        if ($needsFix) {
            # Fix common encoding issues
            $content = $content -replace '・ｿ・・', '修正'
            $content = $content -replace '・ｽ・們・', 'クリエイティブ'
            $content = $content -replace '・ｴ・・', '建築'
            $content = $content -replace '・ｳｴ・・', '初心者'
            $content = $content -replace 'ﾂｧa', '§a'
            $content = $content -replace 'ﾂｧe', '§e'
            $content = $content -replace 'ｼ・', '）'
            $content = $content -replace '蛻晏ｿ・・', '初心者'
            $content = $content -replace '繧ｯ繝ｪ繧ｨ繧､繝・ぅ繝・', 'クリエイティブ'
            $content = $content -replace '蟒ｺ遽・', '建築'
            $content = $content -replace '荳顔ｴ・', '上級'
            $content = $content -replace '蛻晏ｭｦ閠・', '初学者'
            $content = $content -replace '蟒ｺ遞・', '建築'
            $content = $content -replace '郛也ｨ・', '編程'
            $content = $content -replace '閾ｪ螳壻ｹ・', '自定義'
            $content = $content -replace '蝓ｺ遉・', '基礎'
            $content = $content -replace '蛻晏ｭｸ閠・', '初學者'
            $content = $content -replace '蜑ｵ諢・', '創意'
            $content = $content -replace '邱ｨ遞・', '編程'
            $content = $content -replace '鬮倡ｴ・', '高級'
            $content = $content -replace '縺輔ｓ縺九ｉ諡帛ｾ・′螻翫″縺ｾ縺励◆・・', 'さんから招待が届きました）'
            $content = $content -replace '縺輔ｓ縺瑚ｨｪ蝠上ｒ蟶梧悍縺励※縺・∪縺呻ｼ・', 'さんが訪問を希望しています）'
            
            # Write file without BOM
            $utf8NoBom = New-Object System.Text.UTF8Encoding $false
            [System.IO.File]::WriteAllText($file.FullName, $content, $utf8NoBom)
            
            Write-Host "Fixed: $($file.Name)" -ForegroundColor Green
            $fixedCount++
        }
    } catch {
        Write-Host "Error fixing $($file.Name): $_" -ForegroundColor Red
        $errorCount++
    }
}

Write-Host "`nEncoding fix complete!" -ForegroundColor Green
Write-Host "Files fixed: $fixedCount" -ForegroundColor Yellow
Write-Host "Errors: $errorCount" -ForegroundColor Red

# Also fix gradle files
Write-Host "`nFixing Gradle files..." -ForegroundColor Cyan

$gradleFiles = Get-ChildItem -Path $projectRoot -Filter "*.gradle" -File
foreach ($file in $gradleFiles) {
    try {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8
        
        # Remove BOM if present
        if ($content.StartsWith("﻿")) {
            $content = $content.Substring(1)
            $utf8NoBom = New-Object System.Text.UTF8Encoding $false
            [System.IO.File]::WriteAllText($file.FullName, $content, $utf8NoBom)
            Write-Host "Fixed BOM in: $($file.Name)" -ForegroundColor Green
        }
    } catch {
        Write-Host "Error fixing $($file.Name): $_" -ForegroundColor Red
    }
}