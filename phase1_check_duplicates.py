import json
from collections import defaultdict

print('='*80)
print('フェーズ1: データ整合性チェック - 重複ブロック検出')
print('='*80)

# Load extracted blocks
with open('phase1_all_blocks.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

all_blocks = data['flat_list']
total_blocks = data['total_blocks']

print(f'\n総ブロック数: {total_blocks}個')

# Check for duplicates by block value (ID)
value_to_blocks = defaultdict(list)
for block in all_blocks:
    value_to_blocks[block['value']].append(block)

# Find duplicates
duplicates_by_value = {k: v for k, v in value_to_blocks.items() if len(v) > 1}

print(f'\n{"="*80}')
print('重複チェック結果（ブロックID基準）:')
print(f'{"="*80}')

if duplicates_by_value:
    print(f'\n[WARNING] 重複が見つかりました: {len(duplicates_by_value)}個のブロックIDが重複')
    print(f'\n重複しているブロックの詳細:\n')

    for value, blocks in sorted(duplicates_by_value.items()):
        print(f'  ブロックID: {value}')
        print(f'  出現回数: {len(blocks)}回')
        for block in blocks:
            print(f'    - {block["category"]} [インデックス{block["index_in_category"]}]: {block["text"]}')
        print()
else:
    print(f'\n[OK] 重複なし: 全てのブロックIDがユニークです')

# Check for duplicates by Japanese text
text_to_blocks = defaultdict(list)
for block in all_blocks:
    text_to_blocks[block['text']].append(block)

# Find duplicates
duplicates_by_text = {k: v for k, v in text_to_blocks.items() if len(v) > 1}

print(f'\n{"="*80}')
print('重複チェック結果（日本語名基準）:')
print(f'{"="*80}')

if duplicates_by_text:
    print(f'\n[WARNING] 重複が見つかりました: {len(duplicates_by_text)}個の日本語名が重複')
    print(f'\n重複している日本語名の詳細:\n')

    for text, blocks in sorted(duplicates_by_text.items()):
        print(f'  日本語名: {text}')
        print(f'  出現回数: {len(blocks)}回')
        for block in blocks:
            print(f'    - {block["category"]} [インデックス{block["index_in_category"]}]: {block["value"]}')
        print()
else:
    print(f'\n[OK] 重複なし: 全ての日本語名がユニークです')

# Check for near-duplicates (similar names)
print(f'\n{"="*80}')
print('類似名称チェック:')
print(f'{"="*80}')

# Group by normalized value (remove variations)
def normalize_value(value):
    """Normalize block value for similarity check"""
    # Remove common prefixes/suffixes to find base blocks
    normalized = value
    normalized = normalized.replace('stripped_', '')
    normalized = normalized.replace('waxed_', '')
    normalized = normalized.replace('vertical_', '')
    normalized = normalized.replace('polished_', '')
    normalized = normalized.replace('smooth_', '')
    normalized = normalized.replace('chiseled_', '')
    normalized = normalized.replace('cracked_', '')
    normalized = normalized.replace('mossy_', '')
    normalized = normalized.replace('infested_', '')
    normalized = normalized.replace('cut_', '')
    return normalized

normalized_groups = defaultdict(list)
for block in all_blocks:
    normalized = normalize_value(block['value'])
    if normalized != block['value']:  # Only track variations
        normalized_groups[normalized].append(block)

# Show blocks with many variations
print(f'\nブロックバリエーション（3つ以上の変種を持つベースブロック）:\n')

variation_count = 0
for base, blocks in sorted(normalized_groups.items(), key=lambda x: len(x[1]), reverse=True):
    if len(blocks) >= 3:
        variation_count += 1
        print(f'  ベース: {base} ({len(blocks)}個のバリエーション)')
        categories = defaultdict(int)
        for block in blocks:
            categories[block['category']] += 1
        print(f'    カテゴリ分布: {dict(categories)}')

if variation_count == 0:
    print('  （該当なし）')

# Summary
print(f'\n{"="*80}')
print('サマリー:')
print(f'{"="*80}')
print(f'総ブロック数: {total_blocks}個')
print(f'ユニークなブロックID: {len(value_to_blocks)}個')
print(f'ユニークな日本語名: {len(text_to_blocks)}個')
print(f'ブロックID重複: {len(duplicates_by_value)}個')
print(f'日本語名重複: {len(duplicates_by_text)}個')

# Save duplicates report
duplicates_report = {
    'summary': {
        'total_blocks': total_blocks,
        'unique_values': len(value_to_blocks),
        'unique_texts': len(text_to_blocks),
        'duplicate_values': len(duplicates_by_value),
        'duplicate_texts': len(duplicates_by_text)
    },
    'duplicate_values': duplicates_by_value,
    'duplicate_texts': duplicates_by_text
}

with open('phase1_duplicates_report.json', 'w', encoding='utf-8') as f:
    json.dump(duplicates_report, f, ensure_ascii=False, indent=2)

print(f'\n重複レポートを phase1_duplicates_report.json に保存しました')

print(f'\n{"="*80}')
print('重複チェック完了')
print(f'{"="*80}')
