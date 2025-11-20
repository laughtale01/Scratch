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
            text = m.group(1).replace("\\'", "'")
            value = m.group(2)
            blocks.append({'text': text, 'value': value, 'category': cat_name})
        all_blocks[cat_name] = blocks

print('='*80)
print('現在実装されている全ブロックの分析')
print('='*80)

total_blocks = sum(len(blocks) for blocks in all_blocks.values())
print(f'\n総ブロック数: {total_blocks}個\n')

for cat_name in category_names:
    blocks = all_blocks[cat_name]
    print(f'{cat_name}: {len(blocks)}個')

# Analyze by material/type patterns
print('\n' + '='*80)
print('素材・種類別の分析')
print('='*80)

# Collect all block values
all_block_values = []
for cat_name, blocks in all_blocks.items():
    for block in blocks:
        all_block_values.append(block['value'])

# Analyze patterns
print('\n【木材系ブロック】')
wood_types = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'bamboo', 'crimson', 'warped']
wood_block_types = ['_planks', '_log', '_wood', '_stairs', '_slab', '_fence', '_fence_gate', '_button', '_pressure_plate', '_door', '_trapdoor', '_sign', '_hanging_sign']

wood_blocks = {}
for wood in wood_types:
    wood_blocks[wood] = []
    for block_type in wood_block_types:
        block_name = f'{wood}{block_type}'
        if block_name in all_block_values:
            wood_blocks[wood].append(block_type)

print(f'\n木材の種類: {len(wood_types)}種')
for wood in wood_types:
    if wood_blocks[wood]:
        print(f'  {wood}: {len(wood_blocks[wood])}種類 - {", ".join(wood_blocks[wood])}')
    else:
        print(f'  {wood}: 0種類')

# Check for missing common wood items
print('\n【木材系で不足している可能性のあるブロック】')
missing_wood = {}
for wood in wood_types:
    missing = []
    for block_type in wood_block_types:
        block_name = f'{wood}{block_type}'
        if block_name not in all_block_values:
            missing.append(block_type)
    if missing:
        missing_wood[wood] = missing

for wood, missing in missing_wood.items():
    if missing:
        print(f'  {wood}: {", ".join(missing)}')

# Analyze stone variants
print('\n【石材系ブロック】')
stone_types = ['stone', 'cobblestone', 'mossy_cobblestone', 'andesite', 'diorite', 'granite',
               'deepslate', 'cobbled_deepslate', 'polished_deepslate', 'deepslate_bricks', 'deepslate_tiles',
               'blackstone', 'polished_blackstone', 'polished_blackstone_bricks',
               'basalt', 'polished_basalt', 'smooth_basalt']

stone_block_types = ['', '_stairs', '_slab', '_wall', '_pressure_plate', '_button']

stone_blocks = {}
for stone in stone_types:
    stone_blocks[stone] = []
    for block_type in stone_block_types:
        if block_type == '':
            block_name = stone
        else:
            block_name = f'{stone}{block_type}'
        if block_name in all_block_values:
            stone_blocks[stone].append(block_type if block_type else 'base')

print(f'\n石材の種類: {len(stone_types)}種')
for stone in stone_types:
    if stone_blocks[stone]:
        print(f'  {stone}: {len(stone_blocks[stone])}種類')

# Check for terracotta
print('\n【テラコッタ系】')
colors = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink',
          'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']

terracotta_types = ['_terracotta', '_glazed_terracotta']
terracotta_count = 0
for color in colors:
    for t_type in terracotta_types:
        if f'{color}{t_type}' in all_block_values:
            terracotta_count += 1

if 'terracotta' in all_block_values:
    terracotta_count += 1

print(f'テラコッタ総数: {terracotta_count}個（プレーン1 + 16色×2種類 = 33個が最大）')

# Check for concrete
print('\n【コンクリート系】')
concrete_count = sum(1 for color in colors if f'{color}_concrete' in all_block_values)
concrete_powder_count = sum(1 for color in colors if f'{color}_concrete_powder' in all_block_values)
print(f'コンクリート: {concrete_count}/16色')
print(f'コンクリートパウダー: {concrete_powder_count}/16色')

# Check for wool and carpet
print('\n【羊毛・カーペット】')
wool_count = sum(1 for color in colors if f'{color}_wool' in all_block_values)
carpet_count = sum(1 for color in colors if f'{color}_carpet' in all_block_values)
print(f'羊毛: {wool_count}/16色')
print(f'カーペット: {carpet_count}/16色')

# Check for glass
print('\n【ガラス系】')
glass_count = sum(1 for color in colors if f'{color}_stained_glass' in all_block_values)
glass_pane_count = sum(1 for color in colors if f'{color}_stained_glass_pane' in all_block_values)
plain_glass = 1 if 'glass' in all_block_values else 0
plain_pane = 1 if 'glass_pane' in all_block_values else 0
print(f'ガラス: プレーン{plain_glass} + 色付き{glass_count}/16')
print(f'ガラス板: プレーン{plain_pane} + 色付き{glass_pane_count}/16')

# Check for beds
print('\n【ベッド】')
bed_count = sum(1 for color in colors if f'{color}_bed' in all_block_values)
print(f'ベッド: {bed_count}/16色')

# Check for candles
print('\n【ろうそく】')
candle_count = sum(1 for color in colors if f'{color}_candle' in all_block_values)
plain_candle = 1 if 'candle' in all_block_values else 0
print(f'ろうそく: プレーン{plain_candle} + 色付き{candle_count}/16')

# Save all block values for further analysis
with open('current_blocks_list.txt', 'w', encoding='utf-8') as f:
    for cat_name, blocks in all_blocks.items():
        f.write(f'\n{cat_name}:\n')
        for block in blocks:
            f.write(f'  {block["value"]}\n')

print('\n全ブロックリストを current_blocks_list.txt に保存しました')
