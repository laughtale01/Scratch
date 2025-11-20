import json

print('='*80)
print('銅系ブロックの完全性チェック')
print('='*80)

# Load all blocks
with open('phase1_all_blocks.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

all_blocks = data['flat_list']

# Extract all copper-related blocks
copper_blocks = [b for b in all_blocks if 'copper' in b['value'].lower()]

print(f'\n銅系ブロック総数: {len(copper_blocks)}個\n')

# Organize by category
from collections import defaultdict
by_category = defaultdict(list)
for block in copper_blocks:
    by_category[block['category']].append(block)

for cat in sorted(by_category.keys()):
    print(f'\n{cat}: {len(by_category[cat])}個')
    for block in sorted(by_category[cat], key=lambda x: x['index_in_category']):
        print(f'  [{block["index_in_category"]:3d}] {block["value"]:40s} - {block["text"]}')

# Define correct copper block names from Minecraft Wiki
print(f'\n{"="*80}')
print('Minecraft Wiki 公式の銅系ブロック日本語名（参照）')
print(f'{"="*80}')

correct_names = {
    # Ore blocks
    'copper_ore': '銅鉱石',
    'deepslate_copper_ore': 'ディープスレート銅鉱石',
    'raw_copper_block': '銅の原石ブロック',

    # Full blocks - 酸化段階
    'copper_block': '銅ブロック',
    'exposed_copper': '風化した銅',
    'weathered_copper': '錆びた銅',
    'oxidized_copper': '酸化した銅',

    # Cut copper full blocks
    'cut_copper': '切り込み入りの銅',
    'exposed_cut_copper': '風化した切り込み入りの銅',
    'weathered_cut_copper': '錆びた切り込み入りの銅',
    'oxidized_cut_copper': '酸化した切り込み入りの銅',

    # Waxed full blocks
    'waxed_copper_block': '錆止めされた銅ブロック',
    'waxed_exposed_copper': '錆止めされた風化した銅',
    'waxed_weathered_copper': '錆止めされた錆びた銅',
    'waxed_oxidized_copper': '錆止めされた酸化した銅',

    # Waxed cut copper
    'waxed_cut_copper': '錆止めされた切り込み入りの銅',
    'waxed_exposed_cut_copper': '錆止めされた風化した切り込み入りの銅',
    'waxed_weathered_cut_copper': '錆止めされた錆びた切り込み入りの銅',
    'waxed_oxidized_cut_copper': '錆止めされた酸化した切り込み入りの銅',

    # Stairs
    'cut_copper_stairs': '切り込み入りの銅の階段',
    'exposed_cut_copper_stairs': '風化した切り込み入りの銅の階段',
    'weathered_cut_copper_stairs': '錆びた切り込み入りの銅の階段',
    'oxidized_cut_copper_stairs': '酸化した切り込み入りの銅の階段',
    'waxed_cut_copper_stairs': '錆止めされた切り込み入りの銅の階段',
    'waxed_exposed_cut_copper_stairs': '錆止めされた風化した切り込み入りの銅の階段',
    'waxed_weathered_cut_copper_stairs': '錆止めされた錆びた切り込み入りの銅の階段',
    'waxed_oxidized_cut_copper_stairs': '錆止めされた酸化した切り込み入りの銅の階段',

    # Slabs
    'cut_copper_slab': '切り込み入りの銅のハーフブロック',
    'exposed_cut_copper_slab': '風化した切り込み入りの銅のハーフブロック',
    'weathered_cut_copper_slab': '錆びた切り込み入りの銅のハーフブロック',
    'oxidized_cut_copper_slab': '酸化した切り込み入りの銅のハーフブロック',
    'waxed_cut_copper_slab': '錆止めされた切り込み入りの銅のハーフブロック',
    'waxed_exposed_cut_copper_slab': '錆止めされた風化した切り込み入りの銅のハーフブロック',
    'waxed_weathered_cut_copper_slab': '錆止めされた錆びた切り込み入りの銅のハーフブロック',
    'waxed_oxidized_cut_copper_slab': '錆止めされた酸化した切り込み入りの銅のハーフブロック',

    # Vertical slabs
    'vertical_copper_block_slab': '銅ブロックのスラブ（垂直）',
    'vertical_cut_copper_slab': '切り込み入りの銅のスラブ（垂直）',
    'vertical_exposed_cut_copper_slab': '風化した切り込み入りの銅のスラブ（垂直）',
    'vertical_weathered_cut_copper_slab': '錆びた切り込み入りの銅のスラブ（垂直）',
    'vertical_oxidized_cut_copper_slab': '酸化した切り込み入りの銅のスラブ（垂直）',
    'waxed_vertical_copper_block_slab': '錆止めされた銅ブロックのスラブ（垂直）',
    'waxed_vertical_cut_copper_slab': '錆止めされた切り込み入りの銅のスラブ（垂直）',
    'waxed_vertical_exposed_cut_copper_slab': '錆止めされた風化した切り込み入りの銅のスラブ（垂直）',
    'waxed_vertical_weathered_cut_copper_slab': '錆止めされた錆びた切り込み入りの銅のスラブ（垂直）',
    'waxed_vertical_oxidized_cut_copper_slab': '錆止めされた酸化した切り込み入りの銅のスラブ（垂直）',
}

# Check for mismatches
print('\n酸化段階の日本語訳:')
print('  通常 (normal)    → そのまま')
print('  exposed          → 風化した')
print('  weathered        → 錆びた')
print('  oxidized         → 酸化した')

print(f'\n{"="*80}')
print('誤りチェック')
print(f'{"="*80}')

errors = []
for block in copper_blocks:
    value = block['value']
    current_text = block['text']

    if value in correct_names:
        correct_text = correct_names[value]

        if current_text != correct_text:
            errors.append({
                'value': value,
                'category': block['category'],
                'index': block['index_in_category'],
                'current_text': current_text,
                'correct_text': correct_text
            })

if errors:
    print(f'\n[ERROR] {len(errors)}個の誤りが見つかりました:\n')
    for error in errors:
        print(f'  {error["value"]}:')
        print(f'    カテゴリ: {error["category"]} [インデックス{error["index"]}]')
        print(f'    現在: {error["current_text"]}')
        print(f'    正解: {error["correct_text"]}')
        print()
else:
    print('\n[OK] 全ての銅系ブロックの日本語名が正しいです')

# Save errors
if errors:
    with open('phase1_copper_errors.json', 'w', encoding='utf-8') as f:
        json.dump(errors, f, ensure_ascii=False, indent=2)
    print(f'銅系ブロックの誤りを phase1_copper_errors.json に保存しました')

print(f'\n{"="*80}')
print('銅系ブロックチェック完了')
print(f'{"="*80}')
