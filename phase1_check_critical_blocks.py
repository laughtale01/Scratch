import json

print('='*80)
print('重要ブロックカテゴリの整合性チェック')
print('='*80)

# Load all blocks
with open('phase1_all_blocks.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

all_blocks = data['flat_list']

# Check azalea blocks (already found duplicate)
print('\n【1】ツツジ関連ブロック')
print('-'*80)
azalea_blocks = [b for b in all_blocks if 'azalea' in b['value']]
for block in azalea_blocks:
    print(f"  {block['value']:30s} - {block['text']:35s} ({block['category']})")

# Check stripped logs
print('\n【2】樹皮を剥いだ原木・幹')
print('-'*80)
stripped_blocks = [b for b in all_blocks if block['value'].startswith('stripped_')]
print(f'樹皮を剥いだブロック総数: {len(stripped_blocks)}個')

# Sample check
print('\n サンプル（最初の10個）:')
for block in stripped_blocks[:10]:
    print(f"  {block['value']:40s} - {block['text']}")

# Check waxed blocks (besides copper)
print('\n【3】ワックス系ブロック（銅以外）')
print('-'*80)
waxed_non_copper = [b for b in all_blocks if 'waxed' in b['value'] and 'copper' not in b['value']]
if waxed_non_copper:
    for block in waxed_non_copper:
        print(f"  {block['value']:40s} - {block['text']}")
else:
    print('  該当なし')

# Check raw ore blocks
print('\n【4】原石ブロック')
print('-'*80)
raw_blocks = [b for b in all_blocks if b['value'].startswith('raw_') and '_block' in b['value']]
for block in raw_blocks:
    print(f"  {block['value']:30s} - {block['text']:35s} ({block['category']})")

# Correct names for raw ore blocks
raw_ore_correct = {
    'raw_copper_block': '銅の原石ブロック',
    'raw_iron_block': '鉄の原石ブロック',
    'raw_gold_block': '金の原石ブロック',
}

print('\n 正しい日本語名:')
for value, correct_text in raw_ore_correct.items():
    current_block = next((b for b in all_blocks if b['value'] == value), None)
    if current_block:
        current_text = current_block['text']
        if current_text != correct_text:
            print(f'  [ERROR] {value}:')
            print(f'    現在: {current_text}')
            print(f'    正解: {correct_text}')
        else:
            print(f'  [OK] {value}: {correct_text}')

# Check leaves blocks
print('\n【5】葉ブロック')
print('-'*80)
leaves_blocks = [b for b in all_blocks if b['value'].endswith('_leaves')]
print(f'葉ブロック総数: {len(leaves_blocks)}個')

# Check for naming pattern
print('\n 命名パターンチェック（最初の5個）:')
for block in leaves_blocks[:5]:
    print(f"  {block['value']:30s} - {block['text']}")

# Check deepslate variants
print('\n【6】ディープスレート系ブロック')
print('-'*80)
deepslate_blocks = [b for b in all_blocks if 'deepslate' in b['value']]
print(f'ディープスレート系総数: {len(deepslate_blocks)}個')

print('\n サンプル（最初の10個）:')
for block in deepslate_blocks[:10]:
    print(f"  {block['value']:40s} - {block['text']}")

# Summary
print(f'\n{"="*80}')
print('サマリー')
print(f'{"="*80}')
print(f'樹皮を剥いだブロック: {len(stripped_blocks)}個')
print(f'原石ブロック: {len(raw_blocks)}個')
print(f'葉ブロック: {len(leaves_blocks)}個')
print(f'ディープスレート系: {len(deepslate_blocks)}個')
print(f'ワックス系（銅以外）: {len(waxed_non_copper)}個')

print(f'\n{"="*80}')
print('チェック完了')
print(f'{"="*80}')
