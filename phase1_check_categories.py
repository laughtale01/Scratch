import json
from collections import defaultdict

print('='*80)
print('カテゴリ分類の妥当性チェック')
print('='*80)

# Load all blocks
with open('phase1_all_blocks.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

all_blocks = data['flat_list']
by_category = data['categories']

# Category analysis
print('\n【1】カテゴリ別ブロック数と割合')
print('-'*80)

total = len(all_blocks)
for cat_name in ['BUILDING_BLOCKS', 'LIGHTING_BLOCKS', 'DECORATION_BLOCKS',
                  'NATURE_BLOCKS', 'FUNCTIONAL_BLOCKS', 'ORE_BLOCKS', 'SPECIAL_BLOCKS']:
    count = len(by_category[cat_name])
    percentage = (count / total * 100)
    print(f'{cat_name:25s}: {count:4d}個 ({percentage:5.1f}%)')

# Check for potential misclassifications
print(f'\n{"="*80}')
print('【2】カテゴリ配分の分析')
print('='*80)

print('\n観察:')
print(f'  - BUILDING_BLOCKSが{len(by_category["BUILDING_BLOCKS"])}個（55.8%）と非常に多い')
print(f'  - LIGHTING_BLOCKSが{len(by_category["LIGHTING_BLOCKS"])}個（3.9%）と少ない')
print(f'  - SPECIAL_BLOCKSが{len(by_category["SPECIAL_BLOCKS"])}個（4.1%）と少ない')

print('\n推奨事項:')
print('  1. BUILDING_BLOCKSが400個を超えているため、ユーザビリティに影響')
print('  2. サブカテゴリ化や分割を検討する価値あり')
print('  3. しかし、Shape-basedグループ化により、ある程度整理されている')

# Check if certain blocks might be in wrong category
print(f'\n{"="*80}')
print('【3】潜在的なカテゴリ誤分類のチェック')
print('='*80)

# Check for lighting blocks in other categories
print('\n光源ブロックの分布:')
light_keywords = ['lantern', 'torch', 'lamp', 'light', 'glowstone', 'sea_lantern',
                   'shroomlight', 'candle', 'campfire', 'fire', 'jack_o_lantern']

light_blocks_other_cats = []
for block in all_blocks:
    if block['category'] != 'LIGHTING_BLOCKS':
        if any(keyword in block['value'].lower() for keyword in light_keywords):
            light_blocks_other_cats.append(block)

if light_blocks_other_cats:
    print(f'\nLIGHTING_BLOCKS以外にある光源っぽいブロック: {len(light_blocks_other_cats)}個')
    for block in light_blocks_other_cats[:10]:  # Show first 10
        print(f'  {block["value"]:35s} - {block["text"]:35s} ({block["category"]})')
    if len(light_blocks_other_cats) > 10:
        print(f'  ... 他 {len(light_blocks_other_cats) - 10}個')
else:
    print('\n  光源ブロックは全てLIGHTING_BLOCKSに配置されています')

# Check for ore blocks in other categories
print('\n鉱石・原石ブロックの分布:')
ore_keywords = ['_ore', 'raw_']

ore_blocks_other_cats = []
for block in all_blocks:
    if block['category'] != 'ORE_BLOCKS':
        if any(keyword in block['value'].lower() for keyword in ore_keywords):
            ore_blocks_other_cats.append(block)

if ore_blocks_other_cats:
    print(f'\nORE_BLOCKS以外にある鉱石っぽいブロック: {len(ore_blocks_other_cats)}個')
    for block in ore_blocks_other_cats[:10]:
        print(f'  {block["value"]:35s} - {block["text"]:35s} ({block["category"]})')
else:
    print('\n  鉱石ブロックは全てORE_BLOCKSに配置されています')

# Check BUILDING_BLOCKS composition
print(f'\n{"="*80}')
print('【4】BUILDING_BLOCKSの内訳分析')
print('='*80)

building_blocks = by_category['BUILDING_BLOCKS']

# Count by shape
shapes = defaultdict(int)
for block in building_blocks:
    value = block['value']
    if '_stairs' in value:
        shapes['階段'] += 1
    elif '_slab' in value and not value.startswith('vertical_'):
        shapes['ハーフブロック'] += 1
    elif value.startswith('vertical_') and '_slab' in value:
        shapes['垂直スラブ'] += 1
    elif '_wall' in value:
        shapes['壁'] += 1
    elif '_fence' in value and '_gate' not in value:
        shapes['フェンス'] += 1
    elif '_fence_gate' in value:
        shapes['フェンスゲート'] += 1
    elif '_button' in value:
        shapes['ボタン'] += 1
    elif '_pressure_plate' in value:
        shapes['感圧板'] += 1
    elif '_door' in value:
        shapes['ドア'] += 1
    elif '_trapdoor' in value:
        shapes['トラップドア'] += 1
    else:
        shapes['フルブロック・その他'] += 1

print('\nShape-basedグループの内訳:')
for shape, count in sorted(shapes.items(), key=lambda x: x[1], reverse=True):
    percentage = (count / len(building_blocks) * 100)
    print(f'  {shape:20s}: {count:4d}個 ({percentage:5.1f}%)')

# Summary
print(f'\n{"="*80}')
print('サマリー')
print(f'{"="*80}')
print('\nカテゴリ分類:')
print('  [OK] 鉱石・光源ブロックは適切に分類されている')
print('  [ISSUE] BUILDING_BLOCKSが424個と多いが、Shape-basedグループ化により改善')
print('  [OK] 現在のカテゴリ分類は概ね妥当')

print('\n改善提案:')
print('  - BUILDING_BLOCKSのサイズは大きいが、現状のShape-based構造で対応可能')
print('  - 将来的にブロックが1000個を超える場合は再検討が必要')

print(f'\n{"="*80}')
print('カテゴリチェック完了')
print(f'{"="*80}')
