# Script to fix corrupted Japanese text in Java files
# This replaces corrupted character sequences with proper Japanese text

$projectRoot = "D:\minecraft_collaboration_project\minecraft-mod"
$fixedCount = 0
$errorCount = 0

Write-Host "Fixing corrupted Japanese text in Java files..." -ForegroundColor Cyan

# Mapping of corrupted text to correct Japanese
$replacements = @{
    # BlockPackCategory Japanese names
    "蝓ｺ譛ｬ" = "基本"
    "蛻晏ｿ・・" = "初心者"
    "謨呵ご" = "教育"
    "閾ｪ辟ｶ" = "自然"
    "繧ｯ繝ｪ繧ｨ繧､繝・ぅ繝・" = "クリエイティブ"
    "蟒ｺ遽・" = "建築"
    "繝励Ο繧ｰ繝ｩ繝溘Φ繧ｰ" = "プログラミング"
    "荳顔ｴ・" = "上級"
    "繧ｫ繧ｹ繧ｿ繝" = "カスタム"
    
    # Chinese Simplified
    "蝓ｺ遉・" = "基础"
    "蛻晏ｭｦ閠・" = "初学者"
    "謨呵ご" = "教育"
    "閾ｪ辟ｶ" = "自然"
    "蜑ｵ諢・" = "创意"
    "蟒ｺ遲・" = "建筑"
    "郛也ｨ・" = "编程"
    "鬮倡ｴ・" = "高级"
    "閾ｪ螳壻ｹ・" = "自定义"
    
    # Chinese Traditional
    "蝓ｺ遉" = "基礎"
    "蛻晏ｭｸ閠・" = "初學者"
    "謨呵ご" = "教育"
    "閾ｪ辟ｶ" = "自然"
    "蜑ｵ諢・" = "創意"
    "蟒ｺ遽・" = "建築"
    "邱ｨ遞・" = "編程"
    "鬮倡ｴ・" = "高級"
    "閾ｪ險・" = "自訂"
    
    # Korean
    "・壱ｳｴ・・" = "기본"
    "蛻晏ｿ・・" = "초급자"
    "謨呵ご" = "교육"
    "閾ｪ辟ｶ" = "자연"
    "・ｽ・們・" = "창의"
    "・ｴ・・" = "건축"
    "嵓・｡懋ｷｸ・俯ｰ・" = "프로그래밍"
    "・・・" = "고급"
    "・ｮ・・" = "사용자"
    
    # Common issues
    "蝓ｺ遑譁ｹ蝮・" = "基础方块"
    "蝓ｺ遉取婿蝪・" = "基礎方塊"
    "・ｿ・・" = "修正"
    "・ｴ・・" = "建築"
    "・ｳｴ・・" = "初心者"
}

# Additional patterns to fix
$patterns = @{
    '"([^"]+)";' = '"`$1";' # Fix unclosed string literals
}

# Function to fix file
function Fix-JavaFile {
    param($file)
    
    try {
        # Read file content
        $content = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)
        $originalContent = $content
        
        # Apply replacements
        foreach ($corrupted in $replacements.Keys) {
            $content = $content -replace [regex]::Escape($corrupted), $replacements[$corrupted]
        }
        
        # Fix unclosed string literals - find patterns like: return "text
        $content = $content -replace 'return "([^"]+)$', 'return "$1";'
        
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
    if (Fix-JavaFile -file $file.FullName) {
        $fixedCount++
    }
}

Write-Host "`nJapanese text fix complete!" -ForegroundColor Green
Write-Host "Files fixed: $fixedCount" -ForegroundColor Yellow
Write-Host "Errors: $errorCount" -ForegroundColor Red