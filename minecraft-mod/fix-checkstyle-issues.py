#!/usr/bin/env python3
"""
Checkstyle自動修正スクリプト
主要な警告を自動的に修正します
"""
import os
import re
import sys

def fix_wildcard_imports(content):
    """ワイルドカードインポートを修正"""
    # java.util.* を個別インポートに変換
    if 'import java.util.*;' in content:
        # 使用されているjava.utilクラスを検出
        util_classes = set()
        
        # 一般的なjava.utilクラスのパターン
        patterns = [
            r'\bList\b', r'\bArrayList\b', r'\bMap\b', r'\bHashMap\b',
            r'\bSet\b', r'\bHashSet\b', r'\bCollections\b', r'\bArrays\b',
            r'\bOptional\b', r'\bStream\b', r'\bCollectors\b', r'\bDate\b',
            r'\bCalendar\b', r'\bRandom\b', r'\bScanner\b', r'\bIterator\b',
            r'\bQueue\b', r'\bLinkedList\b', r'\bTreeMap\b', r'\bTreeSet\b'
        ]
        
        for pattern in patterns:
            if re.search(pattern, content):
                class_name = pattern.replace(r'\b', '')
                util_classes.add(class_name)
        
        # ワイルドカードインポートを個別インポートに置換
        if util_classes:
            imports = '\n'.join([f'import java.util.{cls};' for cls in sorted(util_classes)])
            content = content.replace('import java.util.*;', imports)
    
    return content

def add_braces_to_if(content):
    """if文にブレースを追加"""
    lines = content.split('\n')
    new_lines = []
    
    for i, line in enumerate(lines):
        new_lines.append(line)
        
        # 単一行if文のパターン
        if_match = re.match(r'^(\s*)if\s*\(.*\)\s*(.+)$', line)
        if if_match and '{' not in line:
            indent = if_match.group(1)
            statement = if_match.group(2).strip()
            
            # ブレース付きに変換
            new_lines[-1] = f"{indent}if{line[len(indent)+2:line.find(')') + 1]} {{"
            new_lines.append(f"{indent}    {statement}")
            new_lines.append(f"{indent}}}")
    
    return '\n'.join(new_lines)

def fix_operator_wrap(content):
    """演算子の位置を修正（行末から行頭へ）"""
    # &&と||を次の行の先頭に移動
    content = re.sub(r'(\s*)(.+?)\s*(&&|\|\|)\s*\n(\s*)(.+)', 
                     r'\1\2\n\4\3 \5', content)
    return content

def fix_constant_naming(content):
    """定数の命名規則を修正"""
    # private static final Gson gson -> private static final Gson GSON
    content = re.sub(r'(private static final \w+) gson\b', r'\1 GSON', content)
    return content

def add_final_to_utility_class(content):
    """ユーティリティクラスにfinalを追加"""
    # public class XXXUtils -> public final class XXXUtils
    content = re.sub(r'(public\s+)(class\s+\w+Utils\b)', r'\1final \2', content)
    return content

def add_private_constructor(content, class_name):
    """ユーティリティクラスにプライベートコンストラクタを追加"""
    if 'Utils' in class_name and f'private {class_name}()' not in content:
        # クラス定義の後にプライベートコンストラクタを追加
        class_pattern = rf'(public\s+(?:final\s+)?class\s+{class_name}\s*{{)'
        replacement = rf'\1\n\n    private {class_name}() {{\n        // Utility class\n    }}'
        content = re.sub(class_pattern, replacement, content)
    
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
            import_match = re.match(r'import\s+([\w.]+)\.(\w+);', line.strip())
            if import_match:
                class_name = import_match.group(2)
                # クラスが実際に使用されているかチェック
                if re.search(rf'\b{class_name}\b', content.replace(line, '')):
                    import_lines.append(line)
            else:
                import_lines.append(line)
        elif imports_section and line.strip() == '':
            # インポートセクションの終了
            imports_section = False
            other_lines.append(line)
        else:
            other_lines.append(line)
    
    # インポートをソートして再構築
    if import_lines:
        sorted_imports = sorted(import_lines)
        # パッケージごとにグループ化
        java_imports = [i for i in sorted_imports if i.startswith('import java.')]
        javax_imports = [i for i in sorted_imports if i.startswith('import javax.')]
        other_imports = [i for i in sorted_imports if not i.startswith('import java.') and not i.startswith('import javax.')]
        
        new_imports = []
        if java_imports:
            new_imports.extend(java_imports)
            new_imports.append('')
        if javax_imports:
            new_imports.extend(javax_imports)
            new_imports.append('')
        if other_imports:
            new_imports.extend(other_imports)
            new_imports.append('')
        
        # コンテンツを再構築
        return '\n'.join(new_imports + other_lines[1:])
    
    return content

def process_file(file_path):
    """ファイルを処理"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # ファイル名からクラス名を取得
        class_name = os.path.basename(file_path).replace('.java', '')
        
        # 各種修正を適用
        content = fix_wildcard_imports(content)
        content = add_braces_to_if(content)
        content = fix_operator_wrap(content)
        content = fix_constant_naming(content)
        content = add_final_to_utility_class(content)
        content = add_private_constructor(content, class_name)
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
        sys.exit(1)
    
    fixed_count = 0
    total_count = 0
    
    for root, dirs, files in os.walk(src_dir):
        for file in files:
            if file.endswith('.java'):
                total_count += 1
                file_path = os.path.join(root, file)
                if process_file(file_path):
                    fixed_count += 1
                    print(f"Fixed: {file_path}")
    
    print(f"\nSummary: Fixed {fixed_count} out of {total_count} files")

if __name__ == "__main__":
    main()