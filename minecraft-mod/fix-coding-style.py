#!/usr/bin/env python3
"""
コーディングスタイル関連のCheckstyle警告を修正
"""
import os
import re

def add_braces_to_if_statements(content):
    """if文に中括弧を追加"""
    lines = content.split('\n')
    new_lines = []
    i = 0
    
    while i < len(lines):
        line = lines[i]
        
        # 単一行if文のパターン（セミコロンで終わる）
        single_line_if = re.match(r'^(\s*)(if\s*\([^)]+\))\s+([^{].*);$', line)
        if single_line_if:
            indent = single_line_if.group(1)
            condition = single_line_if.group(2)
            statement = single_line_if.group(3)
            
            # 中括弧付きに変換
            new_lines.append(f"{indent}{condition} {{")
            new_lines.append(f"{indent}    {statement};")
            new_lines.append(f"{indent}}}")
            i += 1
            continue
        
        # 次の行にステートメントがあるif文
        if_match = re.match(r'^(\s*)(if\s*\([^)]+\))\s*$', line)
        if if_match and i + 1 < len(lines):
            next_line = lines[i + 1]
            # 次の行が中括弧で始まらない場合
            if next_line.strip() and not next_line.strip().startswith('{'):
                indent = if_match.group(1)
                condition = if_match.group(2)
                
                # 中括弧を追加
                new_lines.append(f"{indent}{condition} {{")
                new_lines.append(next_line)
                new_lines.append(f"{indent}}}")
                i += 2
                continue
        
        new_lines.append(line)
        i += 1
    
    return '\n'.join(new_lines)

def fix_operator_wrap(content):
    """演算子の位置を修正（行末から行頭へ）"""
    lines = content.split('\n')
    new_lines = []
    
    for i, line in enumerate(lines):
        # 行末に && または || がある場合
        if line.rstrip().endswith(' &&') or line.rstrip().endswith(' ||'):
            operator = '&&' if line.rstrip().endswith(' &&') else '||'
            # 演算子を削除
            line_without_op = line.rstrip()[:-3].rstrip()
            new_lines.append(line_without_op)
            
            # 次の行がある場合、演算子を追加
            if i + 1 < len(lines) and lines[i + 1].strip():
                next_line = lines[i + 1]
                indent_match = re.match(r'^(\s*)', next_line)
                if indent_match:
                    indent = indent_match.group(1)
                    # 既に演算子で始まっていない場合のみ追加
                    if not next_line.strip().startswith(operator):
                        lines[i + 1] = f"{indent}{operator} {next_line.strip()}"
        else:
            new_lines.append(line)
    
    return '\n'.join(new_lines)

def fix_constant_naming(content):
    """定数の命名規則を修正"""
    # private static final Type name = value; のパターン
    lines = content.split('\n')
    new_lines = []
    
    for line in lines:
        # 定数宣言のパターン
        const_match = re.match(r'^(\s*)(private|public|protected)?\s*(static\s+final|final\s+static)\s+(\w+)\s+(\w+)\s*=', line)
        if const_match:
            prefix = const_match.group(1) or ''
            visibility = const_match.group(2) or ''
            modifiers = const_match.group(3)
            type_name = const_match.group(4)
            var_name = const_match.group(5)
            
            # 変数名が大文字でない場合
            if not var_name.isupper():
                # キャメルケースをスネークケースの大文字に変換
                new_name = re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', var_name).upper()
                line = line.replace(var_name, new_name, 1)
                
                # ファイル内の他の参照も更新
                content = content.replace(f'.{var_name}', f'.{new_name}')
                content = content.replace(f' {var_name}', f' {new_name}')
        
        new_lines.append(line)
    
    return '\n'.join(new_lines)

def add_final_to_utility_classes(content):
    """ユーティリティクラスにfinalを追加"""
    # public class XXXUtils または public class XXXHelper のパターン
    content = re.sub(
        r'(public\s+)(class\s+\w+(Utils|Helper|Constants)\b)',
        r'\1final \2',
        content
    )
    return content

def add_private_constructor_to_utility(content):
    """ユーティリティクラスにプライベートコンストラクタを追加"""
    lines = content.split('\n')
    
    # クラス名を見つける
    class_match = re.search(r'public\s+(?:final\s+)?class\s+(\w+(Utils|Helper|Constants))\b', content)
    if class_match:
        class_name = class_match.group(1)
        
        # プライベートコンストラクタが既に存在するかチェック
        if f'private {class_name}()' not in content:
            new_lines = []
            class_found = False
            
            for line in lines:
                new_lines.append(line)
                
                # クラス定義の開始ブレースを見つける
                if not class_found and re.match(rf'.*class\s+{class_name}\s*{{', line):
                    class_found = True
                    # プライベートコンストラクタを追加
                    new_lines.append('')
                    new_lines.append(f'    private {class_name}() {{')
                    new_lines.append('        // Utility class - prevent instantiation')
                    new_lines.append('    }')
            
            return '\n'.join(new_lines)
    
    return content

def process_file(file_path):
    """ファイルを処理"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # 各種修正を適用
        content = add_braces_to_if_statements(content)
        content = fix_operator_wrap(content)
        content = fix_constant_naming(content)
        content = add_final_to_utility_classes(content)
        content = add_private_constructor_to_utility(content)
        
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
                    print(f"Fixed style in: {file_path}")
    
    print(f"\nSummary: Fixed {fixed_count} out of {total_count} files")

if __name__ == "__main__":
    main()