#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Comprehensive script to fix all encoding issues in Java files
"""

import os
import re
import sys

PROJECT_ROOT = r"D:\minecraft_collaboration_project\minecraft-mod"

def fix_corrupted_text(content):
    """Fix all known corrupted text patterns"""
    
    # Common Japanese corruptions
    replacements = {
        # Comments and documentation
        '繝悶Ο繝・け繝代ャ繧ｯ縺ｮ繧ｫ繝・ざ繝ｪ繧貞ｮ夂ｾｩ': 'ブロックパックのカテゴリを定義',
        '險隱槭↓蠢懊§縺溯｡ｨ遉ｺ蜷阪ｒ蜿門ｾ・': '言語に応じた表示名を取得',
        '教育逶ｮ逧・↓蠢懊§縺溘き繧ｹ繧ｿ繝繝悶Ο繝・け繝代ャ繧ｯ縺ｮ邂｡逅・す繧ｹ繝・Β': '教育目的に応じたカスタムブロックパックの管理システム',
        '繝・ヵ繧ｩ繝ｫ繝医ヶ繝ｭ繝・け繝代ャ繧ｯ縺ｮ蛻晄悄蛹・': 'デフォルトブロックパックの初期化',
        
        # Method names and variables that shouldn't have been corrupted
        '譁ｹ蝮・': '方块"',
        '譁ｹ蝪・': '方塊"',
        '・罷｡・': '블록"',
        
        # Fix unclosed strings
        '蛻晏ｿ・・': '初心者"',
        '繧ｯ繝ｪ繧ｨ繧､繝・ぅ繝・': 'クリエイティブ"',
        '蟒ｺ遽・': '建築"',
        '荳顔ｴ・': '上級"',
        '蛻晏ｭｦ閠・': '初学者"',
        '蟒ｺ遲・': '建筑"',
        '郛也ｨ・': '编程"',
        '閾ｪ螳壻ｹ・': '自定义"',
        '蝓ｺ遉・': '基礎"',
        '蛻晏ｭｸ閠・': '初學者"',
        '蜑ｵ諢・': '創意"',
        '邱ｨ遞・': '編程"',
        '鬮倡ｴ・': '高級"',
        '・壱ｳｴ・・': '초급자"',
        '・ｽ・們・': '창의"',
        '・ｴ・・': '건축"',
        '嵓・｡懋ｷｸ・俯ｰ・': '프로그래밍"',
        '・・・': '고급"',
        
        # Long corrupted descriptions
        '譛繧ょ渕譛ｬ逧・↑蟒ｺ遽峨ヶ繝ｭ繝・け縺ｮ繧ｻ繝・ヨ縺ｧ縺吶ょ・蠢・・↓驕ｩ縺励※縺・∪縺吶・': '最も基本的な建築ブロックのセットです。初心者に適しています。"',
        '譛基本逧・ｻｺ遲第婿蝮鈴寔蜷茨ｼ碁ょ粋初学者"ｽｿ逕ｨ縲・': '最基本的建筑方块集合，适合初学者使用。"',
        '譛基本逧・ｻｺ遽画婿蝪企寔蜷茨ｼ碁←蜷亥・蟄ｸ閠・ｽｿ逕ｨ縲・': '最基本的建築方塊集合，適合初學者使用。"',
        
        # Fix section markers
        'ﾂｧa': '§a',
        'ﾂｧe': '§e',
        'ﾂｧc': '§c',
        'ﾂｧ6': '§6',
        'ﾂｧr': '§r',
        
        # Fix brackets
        'ｼ・': '）',
        '・・': '（',
        
        # More specific replacements
        '基础譁ｹ蝮・': '基础方块"',
        '邏・浹譁ｹ蝪・': '紅石方塊"',
        '郤｢遏ｳ譁ｹ蝮・': '红石方块"',
    }
    
    for corrupted, correct in replacements.items():
        content = content.replace(corrupted, correct)
    
    return content

def fix_unclosed_strings(content):
    """Fix unclosed string literals in various contexts"""
    
    # Fix return statements with unclosed strings
    content = re.sub(r'(return\s+"[^";\r\n]+)(?![";\)])(\s*;)?(\s*$)', r'\1"\3', content, flags=re.MULTILINE)
    
    # Fix put statements in maps
    content = re.sub(r'(\.put\([^,]+,\s*"[^"]+)(?![";\)])(\s*\))', r'\1"\2', content, flags=re.MULTILINE)
    
    # Fix string literals that end with corrupted characters
    content = re.sub(r'"([^"]+)・\s*;', r'"\1";', content)
    
    return content

def fix_java_file(filepath):
    """Fix a single Java file"""
    try:
        # Read file
        with open(filepath, 'r', encoding='utf-8', errors='replace') as f:
            content = f.read()
        
        original_content = content
        
        # Apply fixes
        content = fix_corrupted_text(content)
        content = fix_unclosed_strings(content)
        
        # Additional specific fixes for common patterns
        # Fix Korean text patterns
        content = re.sub(r'기본\s*・罷｡・', '기본 블록"', content)
        content = re.sub(r'・川乱', '에게', content)
        content = re.sub(r'・鮒﨑ｩ・壱共', '적합합니다', content)
        
        # Fix Chinese patterns
        content = re.sub(r'譁ｹ蝮・', '方块"', content)
        content = re.sub(r'譁ｹ蝪・', '方塊"', content)
        
        # Fix Japanese patterns
        content = re.sub(r'繝悶Ο繝・け', 'ブロック', content)
        content = re.sub(r'繝代ャ繧ｯ', 'パック', content)
        
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
    print(f"Scanning directory: {PROJECT_ROOT}")
    
    # Process all Java files
    for root, dirs, files in os.walk(PROJECT_ROOT):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                try:
                    if fix_java_file(filepath):
                        print(f"Fixed: {file}")
                        fixed_count += 1
                except Exception as e:
                    print(f"Error with {file}: {e}")
                    error_count += 1
    
    print(f"\nEncoding fix complete!")
    print(f"Files fixed: {fixed_count}")
    print(f"Errors: {error_count}")
    
    # Also fix gradle files
    print("\nFixing Gradle files...")
    gradle_files = [
        os.path.join(PROJECT_ROOT, "build.gradle"),
        os.path.join(PROJECT_ROOT, "settings.gradle")
    ]
    
    for gradle_file in gradle_files:
        if os.path.exists(gradle_file):
            try:
                with open(gradle_file, 'r', encoding='utf-8', errors='replace') as f:
                    content = f.read()
                
                # Remove BOM if present
                if content.startswith('\ufeff'):
                    content = content[1:]
                    with open(gradle_file, 'w', encoding='utf-8') as f:
                        f.write(content)
                    print(f"Removed BOM from {os.path.basename(gradle_file)}")
            except Exception as e:
                print(f"Error fixing {gradle_file}: {e}")

if __name__ == "__main__":
    main()