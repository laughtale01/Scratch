import re
from collections import defaultdict

print('='*80)
print('Phase 4A: 詳細最適化分析')
print('='*80)

target_file = 'scratch-client/scratch-vm/src/extensions/scratch3_minecraft/index.js'

with open(target_file, 'r', encoding='utf-8') as f:
    content = f.read()
    lines = content.split('\n')

total_bytes = len(content.encode('utf-8'))
total_lines = len(lines)

print(f'\n対象ファイル: {target_file}')
print(f'総バイト数: {total_bytes:,} bytes ({total_bytes/1024:.2f} KB)')
print(f'総行数: {total_lines:,} 行')

# Detailed whitespace analysis
print(f'\n{"="*80}')
print('【1. 空白文字分析】')
print('='*80)

# Count different types of whitespace
spaces = content.count(' ')
tabs = content.count('\t')
newlines = content.count('\n')
carriage_returns = content.count('\r')

total_whitespace_chars = spaces + tabs + newlines + carriage_returns
whitespace_bytes = total_whitespace_chars  # Approximation

print(f'スペース: {spaces:,} 個')
print(f'タブ: {tabs:,} 個')
print(f'改行: {newlines:,} 個')
print(f'復帰文字: {carriage_returns:,} 個')
print(f'総空白文字: {total_whitespace_chars:,} 個 ({total_whitespace_chars/len(content)*100:.1f}%)')

# Analyze lines with excessive whitespace
excessive_blank_lines = 0
previous_blank = False
for line in lines:
    if not line.strip():
        if previous_blank:
            excessive_blank_lines += 1
        previous_blank = True
    else:
        previous_blank = False

print(f'\n連続する空行: {excessive_blank_lines} 箇所')

# Analyze indentation
indent_spaces = 0
for line in lines:
    if line and not line.strip():
        continue
    stripped = line.lstrip()
    if stripped:
        indent = len(line) - len(stripped)
        indent_spaces += indent

print(f'インデント用スペース: {indent_spaces:,} 個 ({indent_spaces} bytes)')

# Comments analysis
print(f'\n{"="*80}')
print('【2. コメント分析】')
print('='*80)

# Detailed comment counting
single_line_comments = []
multi_line_comments = []
inline_comments = []

in_multiline = False
multiline_start = 0
multiline_content = []

for i, line in enumerate(lines, 1):
    stripped = line.strip()

    if in_multiline:
        multiline_content.append(line)
        if '*/' in line:
            multi_line_comments.append({
                'start': multiline_start,
                'end': i,
                'lines': len(multiline_content),
                'content': '\n'.join(multiline_content),
                'bytes': sum(len(l.encode('utf-8')) for l in multiline_content)
            })
            in_multiline = False
            multiline_content = []
    elif '/*' in line:
        in_multiline = True
        multiline_start = i
        multiline_content = [line]
        if '*/' in line:
            multi_line_comments.append({
                'start': i,
                'end': i,
                'lines': 1,
                'content': line,
                'bytes': len(line.encode('utf-8'))
            })
            in_multiline = False
            multiline_content = []
    elif stripped.startswith('//'):
        single_line_comments.append({
            'line': i,
            'content': line,
            'bytes': len(line.encode('utf-8'))
        })
    elif '//' in line and not stripped.startswith('//'):
        inline_comments.append({
            'line': i,
            'content': line.split('//', 1)[1],
            'bytes': len(line.split('//', 1)[1].encode('utf-8'))
        })

total_comment_bytes = (
    sum(c['bytes'] for c in single_line_comments) +
    sum(c['bytes'] for c in multi_line_comments) +
    sum(c['bytes'] for c in inline_comments)
)

print(f'単一行コメント: {len(single_line_comments)} 個')
print(f'複数行コメント: {len(multi_line_comments)} 個')
print(f'インラインコメント: {len(inline_comments)} 個')
print(f'総コメントバイト数: {total_comment_bytes:,} bytes ({total_comment_bytes/total_bytes*100:.1f}%)')

# Analyze comment content
descriptive_comments = 0
trivial_comments = 0
section_dividers = 0

for comment in single_line_comments:
    content_text = comment['content'].strip()
    if '=' in content_text or '-' in content_text or '#' in content_text:
        section_dividers += 1
    elif len(content_text) < 20:
        trivial_comments += 1
    else:
        descriptive_comments += 1

print(f'\nコメント分類:')
print(f'  - セクション区切り: {section_dividers} 個')
print(f'  - 簡潔なコメント(<20文字): {trivial_comments} 個')
print(f'  - 説明的コメント(>=20文字): {descriptive_comments} 個')

# Repetitive pattern analysis
print(f'\n{"="*80}')
print('【3. 繰り返しパターン分析】')
print('='*80)

# Common patterns in category definitions
pattern_1 = '}, {\n  text: '
pattern_2 = ',\n  value: '
pattern_3 = '\n}, {'

count_1 = content.count(pattern_1)
count_2 = content.count(pattern_2)
count_3 = content.count(pattern_3)

print(f'パターン "}},' + ' {\\n  text: ": {count_1} 回 ({count_1 * len(pattern_1)} bytes)')
print(f'パターン ",\\n  value: ": {count_2} 回 ({count_2 * len(pattern_2)} bytes)')
print(f'パターン "\\n}},' + ' {": {count_3} 回 ({count_3 * len(pattern_3)} bytes)')

# Estimate savings from pattern optimization
if count_1 > 0:
    # Original: }, {\n  text:  (9 chars including spaces/newlines)
    # Optimized: },{text: (7 chars)
    savings_1 = count_1 * 2
    print(f'\nパターン1の最適化可能性: {savings_1} bytes')

if count_2 > 0:
    # Original: ,\n  value:  (10 chars)
    # Optimized: ,value: (7 chars)
    savings_2 = count_2 * 3
    print(f'パターン2の最適化可能性: {savings_2} bytes')

# Long string analysis
print(f'\n{"="*80}')
print('【4. 長文字列分析】')
print('='*80)

# Find all string literals
strings = re.findall(r"'([^']*)'", content)
strings += re.findall(r'"([^"]*)"', content)

long_strings = [s for s in strings if len(s) > 50]
very_long_strings = [s for s in strings if len(s) > 100]

print(f'50文字以上の文字列: {len(long_strings)} 個')
print(f'100文字以上の文字列: {len(very_long_strings)} 個')

if very_long_strings:
    print(f'\n超長文字列の例（上位5個）:')
    for i, s in enumerate(sorted(very_long_strings, key=len, reverse=True)[:5], 1):
        preview = s[:80] + '...' if len(s) > 80 else s
        print(f'  {i}. {len(s)} 文字: "{preview}"')

# Code structure analysis
print(f'\n{"="*80}')
print('【5. コード構造分析】')
print('='*80)

# Find category constant sections
category_sections = {
    'BUILDING_BLOCKS': (1607, 2880),
    'LIGHTING_BLOCKS': (2880, 2971),
    'DECORATION_BLOCKS': (2971, 3076),
    'NATURE_BLOCKS': (3076, 3311),
    'FUNCTIONAL_BLOCKS': (3311, 3468),
    'ORE_BLOCKS': (3468, 3595),
    'SPECIAL_BLOCKS': (3595, 3693)
}

total_category_lines = 0
total_category_bytes = 0

for name, (start, end) in category_sections.items():
    line_count = end - start
    total_category_lines += line_count

    section_content = '\n'.join(lines[start-1:end-1])
    section_bytes = len(section_content.encode('utf-8'))
    total_category_bytes += section_bytes

    print(f'{name}: {line_count} 行 ({section_bytes:,} bytes)')

print(f'\nカテゴリ定数合計: {total_category_lines} 行 ({total_category_bytes:,} bytes)')
print(f'ファイル全体に占める割合: {total_category_lines/total_lines*100:.1f}% (行数), {total_category_bytes/total_bytes*100:.1f}% (バイト数)')

# Optimization summary
print(f'\n{"="*80}')
print('【最適化提案まとめ】')
print('='*80)

proposals = []
total_estimated_savings = 0

# Proposal 1: Remove excessive blank lines
if excessive_blank_lines > 0:
    savings = excessive_blank_lines * 2  # Each blank line is ~2 bytes (newline + potential spaces)
    proposals.append({
        'name': '連続空行の削減',
        'description': f'{excessive_blank_lines}箇所の連続空行を単一空行に',
        'savings': savings,
        'priority': '低',
        'risk': '極小'
    })
    total_estimated_savings += savings

# Proposal 2: Optimize category definitions
if count_1 > 100:  # Many category items
    # Conservative estimate: 3 bytes per item
    savings = count_1 * 3
    proposals.append({
        'name': 'カテゴリ定義の形式最適化',
        'description': f'{count_1}個のブロック定義の空白・改行最適化',
        'savings': savings,
        'priority': '中',
        'risk': '低（可読性がやや低下）'
    })
    total_estimated_savings += savings

# Proposal 3: Comment optimization
if trivial_comments > 10:
    # Estimate 30 bytes per trivial comment
    savings = trivial_comments * 30
    proposals.append({
        'name': '自明なコメントの削除',
        'description': f'{trivial_comments}個の簡潔/自明なコメントを削除',
        'savings': savings,
        'priority': '中',
        'risk': '極小'
    })
    total_estimated_savings += savings

# Proposal 4: Indentation optimization (only for minified version)
proposals.append({
    'name': 'インデント最適化（gh-pages用）',
    'description': f'{indent_spaces}個のインデントスペースを削減（minify）',
    'savings': indent_spaces,
    'priority': '低（gh-pagesのみ）',
    'risk': '極小（mainブランチでは実施しない）'
})

print(f'\n最適化提案（優先度順）:')
for i, proposal in enumerate(sorted(proposals, key=lambda x: x['savings'], reverse=True), 1):
    print(f'\n{i}. {proposal["name"]} [{proposal["priority"]}優先度]')
    print(f'   説明: {proposal["description"]}')
    print(f'   削減可能: {proposal["savings"]:,} bytes ({proposal["savings"]/total_bytes*100:.2f}%)')
    print(f'   リスク: {proposal["risk"]}')

print(f'\n{"="*80}')
print(f'総削減可能推定: {total_estimated_savings:,} bytes ({total_estimated_savings/total_bytes*100:.1f}%)')
print(f'最適化後の推定サイズ: {total_bytes - total_estimated_savings:,} bytes ({(total_bytes - total_estimated_savings)/1024:.2f} KB)')
print('='*80)

print(f'\n【推奨アプローチ】')
print('Main ブランチ: 可読性を優先し、安全な最適化のみ実施')
print('  - 連続空行の削減')
print('  - 自明なコメントの削除（慎重に）')
print('  - 未使用コードの削除（あれば）')
print('')
print('GH-Pages ブランチ: ビルドプロセスでminify')
print('  - すでにビルドファイルは圧縮されている可能性が高い')
print('  - さらなる最適化が必要かを確認')

# Save detailed analysis
import json
analysis_data = {
    'total_bytes': total_bytes,
    'total_lines': total_lines,
    'whitespace_chars': total_whitespace_chars,
    'comment_bytes': total_comment_bytes,
    'category_bytes': total_category_bytes,
    'estimated_savings': total_estimated_savings,
    'proposals': proposals
}

with open('phase4_detailed_analysis.json', 'w', encoding='utf-8') as f:
    json.dump(analysis_data, f, indent=2, ensure_ascii=False)

print(f'\n詳細分析結果を phase4_detailed_analysis.json に保存しました')
