#!/usr/bin/env python3
"""
インポート関連のCheckstyle警告を修正
"""
import os
import re

def fix_wildcard_imports(content):
    """ワイルドカードインポートを個別インポートに変換"""
    
    # java.util.* の場合
    if 'import java.util.*;' in content:
        # 使用されているクラスを検出
        util_classes = set()
        
        # 一般的なjava.utilクラスのパターン
        patterns = [
            r'\bList\b', r'\bArrayList\b', r'\bMap\b', r'\bHashMap\b',
            r'\bSet\b', r'\bHashSet\b', r'\bCollections\b', r'\bArrays\b',
            r'\bOptional\b', r'\bStream\b', r'\bCollectors\b', r'\bDate\b',
            r'\bCalendar\b', r'\bRandom\b', r'\bScanner\b', r'\bIterator\b',
            r'\bConcurrentHashMap\b', r'\bLinkedList\b', r'\bTreeMap\b',
            r'\bTreeSet\b', r'\bQueue\b', r'\bDeque\b', r'\bLinkedHashMap\b',
            r'\bProperties\b', r'\bUUID\b', r'\bObjects\b', r'\bTimerTask\b',
            r'\bTimer\b', r'\bLocale\b', r'\bTimeZone\b'
        ]
        
        # コンテンツからクラスを検出
        code_without_imports = '\n'.join([line for line in content.split('\n') 
                                         if not line.strip().startswith('import')])
        
        for pattern in patterns:
            if re.search(pattern, code_without_imports):
                class_name = pattern.replace(r'\b', '')
                util_classes.add(class_name)
        
        # ワイルドカードインポートを個別インポートに置換
        if util_classes:
            imports = '\n'.join([f'import java.util.{cls};' for cls in sorted(util_classes)])
            content = content.replace('import java.util.*;', imports)
    
    # java.io.* の場合
    if 'import java.io.*;' in content:
        io_classes = set()
        
        patterns = [
            r'\bFile\b', r'\bFileReader\b', r'\bFileWriter\b', r'\bBufferedReader\b',
            r'\bBufferedWriter\b', r'\bPrintWriter\b', r'\bIOException\b',
            r'\bInputStream\b', r'\bOutputStream\b', r'\bFileInputStream\b',
            r'\bFileOutputStream\b', r'\bObjectInputStream\b', r'\bObjectOutputStream\b'
        ]
        
        code_without_imports = '\n'.join([line for line in content.split('\n') 
                                         if not line.strip().startswith('import')])
        
        for pattern in patterns:
            if re.search(pattern, code_without_imports):
                class_name = pattern.replace(r'\b', '')
                io_classes.add(class_name)
        
        if io_classes:
            imports = '\n'.join([f'import java.io.{cls};' for cls in sorted(io_classes)])
            content = content.replace('import java.io.*;', imports)
    
    return content

def remove_unused_imports(content):
    """未使用のインポートを削除"""
    lines = content.split('\n')
    import_lines = []
    other_lines = []
    imports_section = False
    
    for line in lines:
        if line.strip().startswith('import '):
            imports_section = True
            import_match = re.match(r'import\s+(static\s+)?([\w.]+)\.(\w+);', line.strip())
            if import_match:
                full_class = import_match.group(2) + '.' + import_match.group(3)
                class_name = import_match.group(3)
                
                # インポート以外の部分でクラスが使用されているかチェック
                code_without_this_import = '\n'.join([l for l in lines if l != line])
                if re.search(rf'\b{class_name}\b', code_without_this_import):
                    import_lines.append(line)
            else:
                import_lines.append(line)
        else:
            if imports_section and line.strip() == '':
                # インポートセクションの終了
                imports_section = False
            other_lines.append(line)
    
    # インポートを整理して再構築
    if import_lines:
        # インポートをグループ化
        java_imports = sorted([i for i in import_lines if i.strip().startswith('import java.')])
        javax_imports = sorted([i for i in import_lines if i.strip().startswith('import javax.')])
        other_imports = sorted([i for i in import_lines if not i.strip().startswith('import java.') 
                               and not i.strip().startswith('import javax.')])
        
        # 最初の非インポート行を見つける
        first_non_import_idx = 0
        for i, line in enumerate(other_lines):
            if line.strip() and not line.strip().startswith('package'):
                first_non_import_idx = i
                break
        
        # 新しいコンテンツを構築
        new_lines = []
        
        # パッケージ宣言を追加
        for line in other_lines[:first_non_import_idx]:
            if line.strip().startswith('package'):
                new_lines.append(line)
                new_lines.append('')
                break
        
        # インポートを追加
        if java_imports:
            new_lines.extend(java_imports)
            if javax_imports or other_imports:
                new_lines.append('')
        
        if javax_imports:
            new_lines.extend(javax_imports)
            if other_imports:
                new_lines.append('')
        
        if other_imports:
            new_lines.extend(other_imports)
            new_lines.append('')
        
        # 残りのコードを追加
        new_lines.extend(other_lines[first_non_import_idx:])
        
        return '\n'.join(new_lines)
    
    return content

def process_file(file_path):
    """ファイルを処理"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # 修正を適用
        content = fix_wildcard_imports(content)
        content = remove_unused_imports(content)
        
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
    src_dir = "src/main/java"
    if not os.path.exists(src_dir):
        print(f"Source directory not found: {src_dir}")
        return
    
    fixed_count = 0
    total_count = 0
    
    for root, dirs, files in os.walk(src_dir):
        for file in files:
            if file.endswith('.java'):
                total_count += 1
                file_path = os.path.join(root, file)
                if process_file(file_path):
                    fixed_count += 1
                    print(f"Fixed imports in: {file_path}")
    
    print(f"\nSummary: Fixed {fixed_count} out of {total_count} files")

if __name__ == "__main__":
    main()