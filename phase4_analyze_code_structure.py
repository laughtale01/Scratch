import re
import json
from collections import defaultdict, Counter

print('='*80)
print('Phase 4A: コード構造分析')
print('='*80)

# Target file
target_file = 'scratch-client/scratch-vm/src/extensions/scratch3_minecraft/index.js'

print(f'\n対象ファイル: {target_file}')
print('-'*80)

# Read the file
with open(target_file, 'r', encoding='utf-8') as f:
    content = f.read()
    lines = content.split('\n')

# Basic statistics
total_lines = len(lines)
total_chars = len(content)
total_bytes = len(content.encode('utf-8'))

print(f'\n【基本統計】')
print(f'総行数: {total_lines:,} 行')
print(f'総文字数: {total_chars:,} 文字')
print(f'総バイト数: {total_bytes:,} bytes ({total_bytes/1024:.2f} KB)')

# Line type analysis
comment_lines = 0
blank_lines = 0
code_lines = 0
comment_chars = 0

in_multiline_comment = False

for line in lines:
    stripped = line.strip()

    if not stripped:
        blank_lines += 1
    elif in_multiline_comment:
        comment_lines += 1
        comment_chars += len(line)
        if '*/' in line:
            in_multiline_comment = False
    elif stripped.startswith('/*'):
        comment_lines += 1
        comment_chars += len(line)
        if '*/' not in line:
            in_multiline_comment = True
    elif stripped.startswith('//'):
        comment_lines += 1
        comment_chars += len(line)
    else:
        code_lines += 1
        # Check for inline comments
        if '//' in stripped:
            comment_chars += len(stripped.split('//', 1)[1])

print(f'\n【行種別分析】')
print(f'コード行: {code_lines:,} 行 ({code_lines/total_lines*100:.1f}%)')
print(f'コメント行: {comment_lines:,} 行 ({comment_lines/total_lines*100:.1f}%)')
print(f'空行: {blank_lines:,} 行 ({blank_lines/total_lines*100:.1f}%)')
print(f'コメント文字数: {comment_chars:,} 文字 ({comment_chars/total_chars*100:.1f}%)')

# Block definitions analysis
block_defs = re.findall(r"opcode:\s*'([^']+)'", content)
block_count = len(block_defs)

# Count block types
reporter_blocks = len(re.findall(r"blockType:\s*'reporter'", content))
command_blocks = len(re.findall(r"blockType:\s*'command'", content))
hat_blocks = len(re.findall(r"blockType:\s*'hat'", content))
boolean_blocks = len(re.findall(r"blockType:\s*'Boolean'", content))

print(f'\n【ブロック定義分析】')
print(f'総ブロック定義数: {block_count:,} 個')
print(f'  - Reporter: {reporter_blocks:,} 個')
print(f'  - Command: {command_blocks:,} 個')
print(f'  - Hat: {hat_blocks:,} 個')
print(f'  - Boolean: {boolean_blocks:,} 個')

# Most common opcodes (check for duplicates)
opcode_counter = Counter(block_defs)
duplicates = {opcode: count for opcode, count in opcode_counter.items() if count > 1}

if duplicates:
    print(f'\n【警告】重複するopcode:')
    for opcode, count in duplicates.items():
        print(f'  - {opcode}: {count}回')
else:
    print(f'\n【確認】重複するopcodeなし OK')

# Menu definitions analysis
menu_defs = re.findall(r"(\w+):\s*\{\s*acceptReporters:\s*(?:true|false),\s*items:\s*\[", content)
menu_count = len(menu_defs)

print(f'\n【メニュー定義分析】')
print(f'総メニュー定義数: {menu_count:,} 個')

# Large menus (block type menu)
block_menu_match = re.search(r"blockType:\s*\{[^}]*items:\s*\[([^\]]+)\]", content, re.DOTALL)
if block_menu_match:
    block_items = re.findall(r"\{[^}]+\}", block_menu_match.group(1))
    print(f'  - blockType メニューアイテム数: {len(block_items):,} 個')

# Method definitions analysis
methods = re.findall(r"(\w+)\s*\([^)]*\)\s*\{", content)
# Filter out common keywords
keywords = {'if', 'for', 'while', 'switch', 'catch', 'function'}
actual_methods = [m for m in methods if m not in keywords]
method_count = len(set(actual_methods))  # Unique methods

print(f'\n【メソッド定義分析】')
print(f'総メソッド数（推定）: {method_count:,} 個')

# Find most common methods (potential duplicates)
method_counter = Counter(actual_methods)
common_methods = method_counter.most_common(10)
print(f'\n最も頻出するメソッド名（上位10）:')
for method, count in common_methods:
    if count > 1:
        print(f'  - {method}(): {count}回')

# String literals analysis (potential for optimization)
string_literals = re.findall(r"'([^']+)'", content)
string_literal_chars = sum(len(s) for s in string_literals)

print(f'\n【文字列リテラル分析】')
print(f'文字列リテラル数: {len(string_literals):,} 個')
print(f'文字列リテラル文字数: {string_literal_chars:,} 文字 ({string_literal_chars/total_chars*100:.1f}%)')

# Find very long strings (potential for optimization)
long_strings = [s for s in string_literals if len(s) > 100]
if long_strings:
    print(f'\n長い文字列（100文字以上）: {len(long_strings)} 個')
    for i, s in enumerate(long_strings[:5], 1):
        preview = s[:80] + '...' if len(s) > 80 else s
        print(f'  {i}. "{preview}" ({len(s)} 文字)')

# Pattern repetition analysis
print(f'\n【パターン分析】')

# Count similar block definition patterns
simple_reporter_pattern = r"\{\s*opcode:\s*'[^']+',\s*blockType:\s*'reporter',\s*text:\s*'[^']+'\s*\}"
simple_reporters = len(re.findall(simple_reporter_pattern, content))
print(f'シンプルなReporterブロック（引数なし）: {simple_reporters} 個')

complex_reporter_pattern = r"\{\s*opcode:\s*'[^']+',\s*blockType:\s*'reporter',\s*text:\s*'[^']+',\s*arguments:\s*\{"
complex_reporters = len(re.findall(complex_reporter_pattern, content))
print(f'複雑なReporterブロック（引数あり）: {complex_reporters} 個')

simple_command_pattern = r"\{\s*opcode:\s*'[^']+',\s*blockType:\s*'command',\s*text:\s*'[^']+'\s*\}"
simple_commands = len(re.findall(simple_command_pattern, content))
print(f'シンプルなCommandブロック（引数なし）: {simple_commands} 個')

complex_command_pattern = r"\{\s*opcode:\s*'[^']+',\s*blockType:\s*'command',\s*text:\s*'[^']+',\s*arguments:\s*\{"
complex_commands = len(re.findall(complex_command_pattern, content))
print(f'複雑なCommandブロック（引数あり）: {complex_commands} 個')

# Import statements
imports = re.findall(r"^import .+$", content, re.MULTILINE)
print(f'\n【Import文分析】')
print(f'Import文の数: {len(imports)} 個')
for imp in imports:
    print(f'  - {imp}')

# TODO/FIXME comments
todos = re.findall(r"//\s*TODO:?\s*(.+)$", content, re.MULTILINE)
fixmes = re.findall(r"//\s*FIXME:?\s*(.+)$", content, re.MULTILINE)

if todos or fixmes:
    print(f'\n【未対応事項】')
    if todos:
        print(f'TODO コメント: {len(todos)} 個')
        for i, todo in enumerate(todos, 1):
            print(f'  {i}. {todo.strip()}')
    if fixmes:
        print(f'FIXME コメント: {len(fixmes)} 個')
        for i, fixme in enumerate(fixmes, 1):
            print(f'  {i}. {fixme.strip()}')

# Optimization opportunities summary
print(f'\n{"="*80}')
print('【最適化機会の推定】')
print('='*80)

potential_savings = 0
opportunities = []

# Comment optimization
if comment_chars > total_chars * 0.15:  # More than 15% comments
    comment_saving = int(comment_chars * 0.3)  # Could reduce by 30%
    potential_savings += comment_saving
    opportunities.append(f'コメント最適化: 約{comment_saving:,}文字削減可能（過剰なコメントの整理）')

# Blank line optimization
blank_char_estimate = blank_lines * 2  # Assuming average of 2 chars per blank line (newline + potential whitespace)
if blank_lines > total_lines * 0.1:  # More than 10% blank lines
    blank_saving = int(blank_char_estimate * 0.5)  # Could reduce by 50%
    potential_savings += blank_saving
    opportunities.append(f'空行最適化: 約{blank_saving:,}文字削減可能（過剰な空行の削除）')

# String literal optimization (if many repeated strings)
string_counter = Counter(string_literals)
repeated_strings = {s: count for s, count in string_counter.items() if count > 10 and len(s) > 10}
if repeated_strings:
    string_saving = sum(len(s) * (count - 1) for s, count in repeated_strings.items())
    string_saving = int(string_saving * 0.8)  # Conservative estimate
    potential_savings += string_saving
    opportunities.append(f'文字列リテラル最適化: 約{string_saving:,}文字削減可能（繰り返し文字列の定数化）')
    print(f'\n繰り返し文字列（10回以上、10文字以上）:')
    for s, count in sorted(repeated_strings.items(), key=lambda x: x[1], reverse=True)[:10]:
        preview = s[:40] + '...' if len(s) > 40 else s
        saving = len(s) * (count - 1)
        print(f'  - "{preview}": {count}回（削減可能: {saving:,}文字）')

print(f'\n最適化機会:')
for i, opp in enumerate(opportunities, 1):
    print(f'{i}. {opp}')

if potential_savings > 0:
    print(f'\n推定削減可能サイズ: {potential_savings:,} 文字 ({potential_savings/total_chars*100:.1f}%)')
    print(f'最適化後の推定サイズ: {total_chars - potential_savings:,} 文字 ({(total_chars - potential_savings)/1024:.2f} KB)')
else:
    print(f'\nコードは既に高度に最適化されています。')

# Save analysis results to JSON
analysis_results = {
    'file': target_file,
    'total_lines': total_lines,
    'total_chars': total_chars,
    'total_bytes': total_bytes,
    'code_lines': code_lines,
    'comment_lines': comment_lines,
    'blank_lines': blank_lines,
    'comment_chars': comment_chars,
    'block_count': block_count,
    'reporter_blocks': reporter_blocks,
    'command_blocks': command_blocks,
    'menu_count': menu_count,
    'method_count': method_count,
    'potential_savings_chars': potential_savings,
    'potential_savings_percent': round(potential_savings/total_chars*100, 2) if potential_savings > 0 else 0
}

with open('phase4_analysis_results.json', 'w', encoding='utf-8') as f:
    json.dump(analysis_results, f, indent=2, ensure_ascii=False)

print(f'\n{"="*80}')
print('分析結果を phase4_analysis_results.json に保存しました')
print('='*80)
