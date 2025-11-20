import json
import re

# Load current implementation
with open('scratch-client/scratch-vm/src/extensions/scratch3_minecraft/index.js', 'r', encoding='utf-8') as f:
    content = f.read()

category_names = ['BUILDING_BLOCKS', 'LIGHTING_BLOCKS', 'DECORATION_BLOCKS',
                  'NATURE_BLOCKS', 'FUNCTIONAL_BLOCKS', 'ORE_BLOCKS', 'SPECIAL_BLOCKS']

# Extract all blocks
all_blocks = {}
for cat_name in category_names:
    pattern = rf"const {cat_name} = \[(.*?)\];"
    match = re.search(pattern, content, re.DOTALL)

    if match:
        array_content = match.group(1)
        blocks = []
        block_pattern = r"\{\s*text:\s*'([^']+)',\s*value:\s*'([^']+)'\s*\}"
        for m in re.finditer(block_pattern, array_content):
            text = m.group(1)
            value = m.group(2)
            blocks.append({'text': text, 'value': value, 'category': cat_name})
        all_blocks[cat_name] = blocks

print('='*80)
print('現在実装されている銅系ブロックの完全リスト')
print('='*80)

# Find all copper blocks
copper_blocks = []
for cat_name, blocks in all_blocks.items():
    for block in blocks:
        if 'copper' in block['value'].lower():
            block['category'] = cat_name
            copper_blocks.append(block)

print(f'\n総数: {len(copper_blocks)}個\n')

# Categorize by type
full_blocks = []
slabs = []
vertical_slabs = []
stairs = []
other = []

for block in copper_blocks:
    value = block['value']
    if value.startswith('vertical_') and '_slab' in value:
        vertical_slabs.append(block)
    elif '_slab' in value and not value.startswith('vertical_'):
        slabs.append(block)
    elif '_stairs' in value:
        stairs.append(block)
    elif '_ore' in value or value.endswith('_block') or value in ['cut_copper', 'oxidized_copper', 'weathered_copper', 'exposed_copper', 'waxed_copper_block', 'waxed_cut_copper', 'waxed_exposed_copper', 'waxed_weathered_copper', 'waxed_oxidized_copper']:
        full_blocks.append(block)
    else:
        other.append(block)

print('【1. フルブロック】')
for b in full_blocks:
    print(f"  [{b['category']:20s}] {b['value']:40s} - {b['text']}")

print(f'\n【2. 通常のハーフブロック（horizontal slab）】')
if slabs:
    for b in slabs:
        print(f"  [{b['category']:20s}] {b['value']:40s} - {b['text']}")
else:
    print('  （実装なし）')

print(f'\n【3. 垂直スラブ（vertical slab）】')
for b in vertical_slabs:
    print(f"  [{b['category']:20s}] {b['value']:40s} - {b['text']}")

print(f'\n【4. 階段（stairs）】')
if stairs:
    for b in stairs:
        print(f"  [{b['category']:20s}] {b['value']:40s} - {b['text']}")
else:
    print('  （実装なし）')

if other:
    print(f'\n【5. その他】')
    for b in other:
        print(f"  [{b['category']:20s}] {b['value']:40s} - {b['text']}")

print('\n' + '='*80)
print('集計:')
print('='*80)
print(f'フルブロック: {len(full_blocks)}個')
print(f'通常ハーフブロック: {len(slabs)}個')
print(f'垂直スラブ: {len(vertical_slabs)}個')
print(f'階段: {len(stairs)}個')
print(f'その他: {len(other)}個')
print(f'合計: {len(copper_blocks)}個')
