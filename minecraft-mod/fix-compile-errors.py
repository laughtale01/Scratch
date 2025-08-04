#!/usr/bin/env python3
"""
コンパイルエラーを修正するスクリプト
"""
import os
import re

def fix_broken_if_statements(content):
    """壊れたif文を修正"""
    # Pattern: if (condition { -> if (condition) {
    content = re.sub(r'if\s*\(([^{]+?)\s*{', r'if (\1) {', content)
    
    # Pattern: if (x == null || getClass() { -> if (x == null || getClass() != o.getClass()) {
    content = re.sub(r'if\s*\(([^=]+==\s*null\s*\|\|\s*getClass\(\))\s*{', 
                     r'if (\1 != o.getClass()) {', content)
    content = re.sub(r'if\s*\(([^=]+==\s*null\s*\|\|\s*getClass\(\))\s*{', 
                     r'if (\1 != obj.getClass()) {', content)
    
    # Fix broken multi-line conditions
    content = re.sub(r'if\s*\(([^)]+)\s*{\s*\n\s*(&&|\|\|)\s*}', r'if (\1)', content)
    
    # Fix standalone && or || lines
    lines = content.split('\n')
    fixed_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if i + 1 < len(lines):
            next_line = lines[i + 1].strip()
            if next_line in ['&& }', '|| }']:
                # Skip the broken line
                i += 2
                continue
            elif next_line.startswith('&& ') or next_line.startswith('|| '):
                # Merge with previous line
                fixed_lines[-1] = fixed_lines[-1].rstrip() + ' ' + next_line
                i += 2
                continue
        fixed_lines.append(line)
        i += 1
    
    return '\n'.join(fixed_lines)

def fix_specific_errors(file_path, content):
    """ファイル固有のエラーを修正"""
    
    if 'ValidationUtils.java' in file_path:
        # Fix specific error in ValidationUtils
        content = re.sub(r'if\s*\(!isValidCoordinate\(x1,\s*false\)\)\s*{\s*\n\s*\|\|\s*}',
                         'if (!isValidCoordinate(x1, false))', content)
    
    if 'AgentManager.java' in file_path:
        # Fix multi-line condition
        content = re.sub(r'if\s*\(level\.getBlockState\(checkPos\.below\(\)\)\s*{\s*\n\s*&&\s*}',
                         'if (level.getBlockState(checkPos.below()).isSolidRender(level, checkPos.below()))', content)
    
    if 'BatchBlockPlacer.java' in file_path:
        # Fix comparator
        content = re.sub(r'if\s*\(a\.getY\(\)\s*{', 'if (a.getY() != b.getY()) {', content)
        content = re.sub(r'if\s*\(a\.getX\(\)\s*{', 'if (a.getX() != b.getX()) {', content)
    
    return content

def process_file(file_path):
    """ファイルを処理"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # 修正を適用
        content = fix_broken_if_statements(content)
        content = fix_specific_errors(file_path, content)
        
        # 変更があった場合のみ書き込み
        if content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
        
        return False
    
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
        return False

def main():
    """メイン処理"""
    # エラーが発生したファイルのリスト
    error_files = [
        'src/main/java/edu/minecraft/collaboration/blockpacks/BlockPack.java',
        'src/main/java/edu/minecraft/collaboration/entities/AgentManager.java',
        'src/main/java/edu/minecraft/collaboration/models/BlockPack.java',
        'src/main/java/edu/minecraft/collaboration/models/OfflineAction.java',
        'src/main/java/edu/minecraft/collaboration/models/OfflineSession.java',
        'src/main/java/edu/minecraft/collaboration/models/OfflineStudentData.java',
        'src/main/java/edu/minecraft/collaboration/offline/OfflineAction.java',
        'src/main/java/edu/minecraft/collaboration/offline/OfflineSession.java',
        'src/main/java/edu/minecraft/collaboration/offline/OfflineStudentData.java',
        'src/main/java/edu/minecraft/collaboration/performance/BatchBlockPlacer.java',
        'src/main/java/edu/minecraft/collaboration/progress/Achievement.java',
        'src/main/java/edu/minecraft/collaboration/progress/LearningMilestone.java',
        'src/main/java/edu/minecraft/collaboration/util/ValidationUtils.java'
    ]
    
    fixed_count = 0
    
    for file_path in error_files:
        if os.path.exists(file_path):
            if process_file(file_path):
                fixed_count += 1
                print(f"Fixed: {file_path}")
        else:
            print(f"File not found: {file_path}")
    
    print(f"\nFixed {fixed_count} files")

if __name__ == "__main__":
    main()