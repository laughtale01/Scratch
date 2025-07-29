# Comprehensive script to fix encoding issues in all Java files

$projectRoot = "D:\minecraft_collaboration_project\minecraft-mod"
$fixedCount = 0
$errorCount = 0

Write-Host "Starting comprehensive encoding fix..." -ForegroundColor Cyan

# Function to fix common encoding issues in a file
function Fix-EncodingIssues {
    param($file)
    
    try {
        # Read file content
        $content = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)
        $originalContent = $content
        
        # Fix unclosed string literals - common patterns
        $content = $content -replace '(return\s+"[^";\r\n]+);?\s*$', '$1";' -replace '(return\s+"[^";\r\n]+)\r?\n', '$1";\r\n'
        
        # Fix specific corrupted Japanese text patterns that appear in multiple files
        $content = $content -replace '繝悶Ο繝\S+', 'ブロックパック'
        $content = $content -replace '險隱槭\S+', '言語に応じた表示名を取得'
        $content = $content -replace '蛻晏ｿ・・', '初心者"'
        $content = $content -replace '繧ｯ繝ｪ繧ｨ繧､繝・ぅ繝・', 'クリエイティブ"'
        $content = $content -replace '蟒ｺ遽・', '建築"'
        $content = $content -replace '荳顔ｴ・', '上級"'
        $content = $content -replace '蛻晏ｭｦ閠・', '初学者"'
        $content = $content -replace '蟒ｺ遲・', '建筑"'
        $content = $content -replace '郛也ｨ・', '编程"'
        $content = $content -replace '閾ｪ螳壻ｹ・', '自定义"'
        $content = $content -replace '蝓ｺ遉・', '基礎"'
        $content = $content -replace '蛻晏ｭｸ閠・', '初學者"'
        $content = $content -replace '蜑ｵ諢・', '創意"'
        $content = $content -replace '邱ｨ遞・', '編程"'
        $content = $content -replace '鬮倡ｴ・', '高級"'
        $content = $content -replace '・壱ｳｴ・・', '초급자"'
        $content = $content -replace '・ｽ・們・', '창의"'
        $content = $content -replace '・ｴ・・', '건축"'
        $content = $content -replace '嵓・｡懋ｷｸ・俯ｰ・', '프로그래밍"'
        $content = $content -replace '・・・', '고급"'
        
        # Fix section markers
        $content = $content -replace 'ﾂｧa', '§a'
        $content = $content -replace 'ﾂｧe', '§e'
        $content = $content -replace 'ﾂｧc', '§c'
        $content = $content -replace 'ﾂｧ6', '§6'
        $content = $content -replace 'ﾂｧr', '§r'
        
        # Fix corrupted brackets/parentheses
        $content = $content -replace 'ｼ・', '）'
        $content = $content -replace '・・', '（'
        
        # Fix specific method/variable names that got corrupted
        $content = $content -replace '蝓ｺ遑譁ｹ蝮・', '基础方块"'
        $content = $content -replace '蝓ｺ遉取婿蝪・', '基礎方塊"'
        
        # More comprehensive fixes for various patterns
        $content = $content -replace '縺輔ｓ縺九ｉ諡帛ｾ・′螻翫″縺ｾ縺励◆・・', 'さんから招待が届きました）"'
        $content = $content -replace '縺輔ｓ縺瑚ｨｪ蝠上ｒ蟶梧悍縺励※縺・∪縺呻ｼ・', 'さんが訪問を希望しています）"'
        
        # Check if content changed
        if ($content -ne $originalContent) {
            # Write file with UTF-8 encoding without BOM
            $utf8NoBom = New-Object System.Text.UTF8Encoding $false
            [System.IO.File]::WriteAllText($file, $content, $utf8NoBom)
            Write-Host "Fixed: $($file.Name)" -ForegroundColor Green
            return $true
        }
        return $false
    } catch {
        Write-Host "Error fixing $($file.Name): $_" -ForegroundColor Red
        $script:errorCount++
        return $false
    }
}

# Process all Java files
$javaFiles = Get-ChildItem -Path $projectRoot -Filter "*.java" -Recurse
foreach ($file in $javaFiles) {
    if (Fix-EncodingIssues -file $file.FullName) {
        $fixedCount++
    }
}

Write-Host "`nEncoding fix complete!" -ForegroundColor Green
Write-Host "Files fixed: $fixedCount" -ForegroundColor Yellow
Write-Host "Errors: $errorCount" -ForegroundColor Red

if ($fixedCount -gt 0) {
    Write-Host "`nNow running a build to check for remaining issues..." -ForegroundColor Cyan
}