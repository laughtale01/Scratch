import re

print('='*80)
print('Phase 4: 最適化対象の特定')
print('='*80)

target_file = 'scratch-client/scratch-vm/src/extensions/scratch3_minecraft/index.js'

with open(target_file, 'r', encoding='utf-8') as f:
    content = f.read()
    lines = content.split('\n')

print(f'\n対象ファイル: {target_file}')
print(f'総行数: {len(lines):,} 行\n')

# Find trivial comments
print('='*80)
print('【1. 自明/簡潔なコメントの分析】')
print('='*80)

trivial_comments = []
for i, line in enumerate(lines, 1):
    stripped = line.strip()
    if stripped.startswith('//'):
        comment_text = stripped[2:].strip()
        # Check if it's trivial
        if len(comment_text) < 15:  # Very short comments
            trivial_comments.append({
                'line': i,
                'text': comment_text,
                'full_line': line
            })

print(f'\n簡潔なコメント（15文字未満）: {len(trivial_comments)} 個\n')

# Categorize trivial comments
section_markers = []
truly_trivial = []
semi_useful = []

for comment in trivial_comments:
    text = comment['text']
    if not text:  # Empty comment
        truly_trivial.append(comment)
    elif any(char in text for char in ['=', '-', '*', '#']):  # Section divider
        section_markers.append(comment)
    elif len(text) < 5:  # Very short
        truly_trivial.append(comment)
    else:
        semi_useful.append(comment)

print(f'カテゴリ分類:')
print(f'  - セクション区切り: {len(section_markers)} 個')
print(f'  - 完全に自明/空: {len(truly_trivial)} 個')
print(f'  - やや有用: {len(semi_useful)} 個')

if truly_trivial:
    print(f'\n完全に自明なコメント（削除候補）:')
    for i, comment in enumerate(truly_trivial[:10], 1):
        print(f'  行{comment["line"]}: // {comment["text"]}')
    if len(truly_trivial) > 10:
        print(f'  ... 他 {len(truly_trivial) - 10} 個')

# Find duplicated code patterns
print(f'\n{"="*80}')
print('【2. 重複コードパターンの分析】')
print('='*80)

# Find repeated function patterns
function_defs = []
for i, line in enumerate(lines, 1):
    # Match function/method definitions
    match = re.search(r'(\w+)\s*\([^)]*\)\s*\{', line)
    if match and not any(keyword in line for keyword in ['if', 'for', 'while', 'switch', 'catch']):
        func_name = match.group(1)
        function_defs.append({
            'name': func_name,
            'line': i,
            'full_line': line.strip()
        })

# Count function occurrences
from collections import Counter
func_counter = Counter([f['name'] for f in function_defs])

print(f'\n関数/メソッド定義数: {len(function_defs)} 個')
print(f'ユニークな関数名: {len(func_counter)} 個')

duplicates = {name: count for name, count in func_counter.items() if count > 1}
if duplicates:
    print(f'\n重複する関数名（同名の関数が複数ある）:')
    for name, count in sorted(duplicates.items(), key=lambda x: x[1], reverse=True):
        print(f'  - {name}(): {count}回')
        # Show lines where this function appears
        lines_with_func = [f['line'] for f in function_defs if f['name'] == name]
        print(f'    行: {lines_with_func}')

# Find unused imports/variables
print(f'\n{"="*80}')
print('【3. Import文と変数の分析】')
print('='*80)

# Find all imports
imports = []
for i, line in enumerate(lines, 1):
    if line.strip().startswith('import ') or line.strip().startswith('const ') and ' = require(' in line:
        imports.append({
            'line': i,
            'text': line.strip()
        })

print(f'\nImport/Require文: {len(imports)} 個')
for imp in imports:
    print(f'  行{imp["line"]}: {imp["text"]}')

# Find top-level constants that might be unused
const_defs = []
for i, line in enumerate(lines[0:100], 1):  # Check first 100 lines for const definitions
    if re.match(r'^const \w+', line.strip()):
        match = re.match(r'^const (\w+)', line.strip())
        if match:
            const_name = match.group(1)
            const_defs.append({
                'name': const_name,
                'line': i,
                'text': line.strip()
            })

print(f'\nトップレベル定数（最初100行）: {len(const_defs)} 個')

# Check which constants are actually used
for const in const_defs:
    name = const['name']
    # Count occurrences (excluding the definition line)
    usage_count = sum(1 for i, line in enumerate(lines, 1) if i != const['line'] and name in line)
    const['usage_count'] = usage_count

unused_constants = [c for c in const_defs if c['usage_count'] == 0]
if unused_constants:
    print(f'\n未使用の可能性がある定数:')
    for const in unused_constants:
        print(f'  行{const["line"]}: {const["text"]} (使用回数: {const["usage_count"]})')
else:
    print(f'\n未使用の定数は見つかりませんでした。')

# Find long consecutive blank lines
print(f'\n{"="*80}')
print('【4. 連続空行の分析】')
print('='*80)

consecutive_blanks = []
blank_count = 0
blank_start = 0

for i, line in enumerate(lines, 1):
    if not line.strip():
        if blank_count == 0:
            blank_start = i
        blank_count += 1
    else:
        if blank_count > 1:
            consecutive_blanks.append({
                'start': blank_start,
                'end': i - 1,
                'count': blank_count
            })
        blank_count = 0

print(f'\n連続空行の箇所: {len(consecutive_blanks)} 箇所')
print(f'2行連続: {sum(1 for b in consecutive_blanks if b["count"] == 2)} 箇所')
print(f'3行以上連続: {sum(1 for b in consecutive_blanks if b["count"] >= 3)} 箇所')

if any(b['count'] >= 3 for b in consecutive_blanks):
    print(f'\n3行以上の連続空行（削減推奨）:')
    for blank in [b for b in consecutive_blanks if b['count'] >= 3][:10]:
        print(f'  行{blank["start"]}-{blank["end"]}: {blank["count"]}行連続')

# Find long lines that could be split or optimized
print(f'\n{"="*80}')
print('【5. 長い行の分析】')
print('='*80)

long_lines = []
for i, line in enumerate(lines, 1):
    if len(line) > 120:  # Lines longer than 120 characters
        long_lines.append({
            'line': i,
            'length': len(line),
            'preview': line[:100].strip() + '...'
        })

print(f'\n120文字を超える行: {len(long_lines)} 行')
if long_lines:
    print(f'\n最も長い行（上位5）:')
    for i, line_info in enumerate(sorted(long_lines, key=lambda x: x['length'], reverse=True)[:5], 1):
        print(f'  {i}. 行{line_info["line"]}: {line_info["length"]}文字')
        print(f'      {line_info["preview"]}')

# Summary
print(f'\n{"="*80}')
print('【最適化推奨まとめ】')
print('='*80)

recommendations = []

if truly_trivial:
    recommendations.append({
        'action': f'{len(truly_trivial)}個の自明なコメントを削除',
        'estimated_savings': len(truly_trivial) * 20,  # Average 20 bytes per comment
        'risk': '極小',
        'priority': '中'
    })

if any(b['count'] >= 3 for b in consecutive_blanks):
    excessive_blanks = sum(b['count'] - 1 for b in consecutive_blanks if b['count'] >= 3)
    recommendations.append({
        'action': f'{excessive_blanks}行の過剰な空行を削減',
        'estimated_savings': excessive_blanks * 2,
        'risk': '極小',
        'priority': '低'
    })

if unused_constants:
    recommendations.append({
        'action': f'{len(unused_constants)}個の未使用定数を削除',
        'estimated_savings': sum(len(c['text']) for c in unused_constants),
        'risk': '中（要確認）',
        'priority': '中'
    })

print(f'\n優先順位付き推奨事項:')
for i, rec in enumerate(sorted(recommendations, key=lambda x: x['estimated_savings'], reverse=True), 1):
    print(f'\n{i}. {rec["action"]}')
    print(f'   推定削減: {rec["estimated_savings"]} bytes')
    print(f'   リスク: {rec["risk"]}')
    print(f'   優先度: {rec["priority"]}')

total_recommended_savings = sum(r['estimated_savings'] for r in recommendations)
print(f'\n推奨最適化の合計削減量: {total_recommended_savings} bytes')

print(f'\n{"="*80}')
print('重要な注意事項:')
print('- すべての変更は慎重に実施し、各変更後に構文チェックを実行してください')
print('- コメントの削除は可読性に影響するため、本当に不要なもののみに限定してください')
print('- 未使用と思われる定数も、動的に参照されている可能性があるため確認が必要です')
print('='*80)
