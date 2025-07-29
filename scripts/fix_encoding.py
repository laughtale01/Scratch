#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script to fix encoding issues in Java files
"""

import os
import re
import codecs

PROJECT_ROOT = r"D:\minecraft_collaboration_project\minecraft-mod"

# Mapping of corrupted text to correct text
REPLACEMENTS = {
    # Japanese
    '蛻晏ｿ・・': '初心者"',
    '繧ｯ繝ｪ繧ｨ繧､繝・ぅ繝・': 'クリエイティブ"',
    '蟒ｺ遽・': '建築"',
    '荳顔ｴ・': '上級"',
    '繧ｫ繧ｹ繧ｿ繝': 'カスタム',
    '蝓ｺ譛ｬ': '基本',
    '謨呵ご': '教育',
    '閾ｪ辟ｶ': '自然',
    '繝励Ο繧ｰ繝ｩ繝溘Φ繧ｰ': 'プログラミング',
    
    # Chinese Simplified
    '蝓ｺ遑': '基础',
    '蛻晏ｭｦ閠・': '初学者"',
    '蜑ｵ諢・': '创意"',
    '蟒ｺ遲・': '建筑"',
    '郛也ｨ・': '编程"',
    '鬮倡ｺｧ': '高级',
    '閾ｪ螳壻ｹ・': '自定义"',
    '蛻帶э': '创意',
    
    # Chinese Traditional  
    '蝓ｺ遉・': '基礎"',
    '蛻晏ｭｸ閠・': '初學者"',
    '邱ｨ遞・': '編程"',
    '鬮倡ｴ・': '高級"',
    '閾ｪ螳夂ｾｩ': '自訂',
    '閾ｪ險・': '自訂"',
    
    # Korean
    '・ｰ・ｸ': '기본',
    '・壱ｳｴ・・': '초급자"',
    '・川悖': '교육',
    '・川硫': '자연',
    '・ｽ・們・': '창의"',
    '・ｴ・・': '건축"',
    '嵓・｡懋ｷｸ・俯ｰ・': '프로그래밍"',
    '・・・': '고급"',
    '・ｬ・ｩ・・・菩攪': '사용자 정의',
    
    # Section markers
    'ﾂｧa': '§a',
    'ﾂｧe': '§e',
    'ﾂｧc': '§c',
    'ﾂｧ6': '§6',
    'ﾂｧr': '§r',
    
    # Brackets
    'ｼ・': '）',
    '・・': '（',
    
    # Common corrupted patterns
    '蝓ｺ遑譁ｹ蝮・': '基础方块"',
    '蝓ｺ遉取婿蝪・': '基礎方塊"',
    
    # Comments
    '繝悶Ο繝・け繝代ャ繧ｯ縺ｮ繧ｫ繝・ざ繝ｪ繧貞ｮ夂ｾｩ': 'ブロックパックのカテゴリを定義',
    '險隱槭↓蠢懊§縺溯｡ｨ遉ｺ蜷阪ｒ蜿門ｾ・': '言語に応じた表示名を取得',
    
    # Spanish/French/German accents
    'ﾃｩ': 'é',
    'ﾃｧ': 'ç',
    'ﾃｱ': 'ñ',
    'ﾃｳ': 'ó',
    'ﾃｭ': 'í',
    'ﾃ｡': 'á',
    'ﾃｼ': 'ü',
    'ﾃ､': 'ä',
    'ﾃｶ': 'ö',
    'ﾃ嬰': 'É',
}

def fix_file(filepath):
    """Fix encoding issues in a single file"""
    try:
        # Read file
        with open(filepath, 'r', encoding='utf-8', errors='replace') as f:
            content = f.read()
        
        original_content = content
        
        # Apply replacements
        for corrupted, correct in REPLACEMENTS.items():
            content = content.replace(corrupted, correct)
        
        # Fix unclosed string literals
        # Pattern: return "text<end of line>
        content = re.sub(r'(return\s+"[^";\r\n]+);?\s*$', r'\1";', content, flags=re.MULTILINE)
        
        # Save if changed
        if content != original_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
        return False
    except Exception as e:
        print(f"Error processing {filepath}: {e}")
        return False

def main():
    fixed_count = 0
    error_count = 0
    
    print("Starting comprehensive encoding fix...")
    
    # Process all Java files
    for root, dirs, files in os.walk(PROJECT_ROOT):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                if fix_file(filepath):
                    print(f"Fixed: {file}")
                    fixed_count += 1
    
    print(f"\nEncoding fix complete!")
    print(f"Files fixed: {fixed_count}")
    print(f"Errors: {error_count}")

if __name__ == "__main__":
    main()