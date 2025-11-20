import re
import json

print('='*80)
print('銅系ブロック実装スクリプト')
print('='*80)

# Define missing copper blocks to add
missing_copper_blocks = {
    'ORE_BLOCKS': [
        {'text': '風化した銅', 'value': 'exposed_copper'},
        {'text': '錆止めされた風化した銅', 'value': 'waxed_exposed_copper'},
        {'text': '錆止めされた錆びた銅', 'value': 'waxed_weathered_copper'},
        {'text': '錆止めされた酸化した銅', 'value': 'waxed_oxidized_copper'},
    ],
    'BUILDING_BLOCKS': {
        'slabs': [
            {'text': '切り込み入りの銅のハーフブロック', 'value': 'cut_copper_slab'},
            {'text': '風化した切り込み入りの銅のハーフブロック', 'value': 'exposed_cut_copper_slab'},
            {'text': '錆びた切り込み入りの銅のハーフブロック', 'value': 'weathered_cut_copper_slab'},
            {'text': '酸化した切り込み入りの銅のハーフブロック', 'value': 'oxidized_cut_copper_slab'},
            {'text': '錆止めされた切り込み入りの銅のハーフブロック', 'value': 'waxed_cut_copper_slab'},
            {'text': '錆止めされた風化した切り込み入りの銅のハーフブロック', 'value': 'waxed_exposed_cut_copper_slab'},
            {'text': '錆止めされた錆びた切り込み入りの銅のハーフブロック', 'value': 'waxed_weathered_cut_copper_slab'},
            {'text': '錆止めされた酸化した切り込み入りの銅のハーフブロック', 'value': 'waxed_oxidized_cut_copper_slab'},
        ],
        'stairs': [
            {'text': '切り込み入りの銅の階段', 'value': 'cut_copper_stairs'},
            {'text': '風化した切り込み入りの銅の階段', 'value': 'exposed_cut_copper_stairs'},
            {'text': '錆びた切り込み入りの銅の階段', 'value': 'weathered_cut_copper_stairs'},
            {'text': '酸化した切り込み入りの銅の階段', 'value': 'oxidized_cut_copper_stairs'},
            {'text': '錆止めされた切り込み入りの銅の階段', 'value': 'waxed_cut_copper_stairs'},
            {'text': '錆止めされた風化した切り込み入りの銅の階段', 'value': 'waxed_exposed_cut_copper_stairs'},
            {'text': '錆止めされた錆びた切り込み入りの銅の階段', 'value': 'waxed_weathered_cut_copper_stairs'},
            {'text': '錆止めされた酸化した切り込み入りの銅の階段', 'value': 'waxed_oxidized_cut_copper_stairs'},
        ],
        'vertical_slabs': [
            {'text': '錆止めされた銅ブロックのスラブ（垂直）', 'value': 'waxed_vertical_copper_block_slab'},
            {'text': '錆止めされた切り込み入りの銅のスラブ（垂直）', 'value': 'waxed_vertical_cut_copper_slab'},
            {'text': '錆止めされた風化した切り込み入りの銅のスラブ（垂直）', 'value': 'waxed_vertical_exposed_cut_copper_slab'},
            {'text': '錆止めされた錆びた切り込み入りの銅のスラブ（垂直）', 'value': 'waxed_vertical_weathered_cut_copper_slab'},
            {'text': '錆止めされた酸化した切り込み入りの銅のスラブ（垂直）', 'value': 'waxed_vertical_oxidized_cut_copper_slab'},
        ]
    }
}

print('\n追加するブロック:')
print(f"  ORE_BLOCKS: {len(missing_copper_blocks['ORE_BLOCKS'])}個")
print(f"  BUILDING_BLOCKS (slabs): {len(missing_copper_blocks['BUILDING_BLOCKS']['slabs'])}個")
print(f"  BUILDING_BLOCKS (stairs): {len(missing_copper_blocks['BUILDING_BLOCKS']['stairs'])}個")
print(f"  BUILDING_BLOCKS (vertical_slabs): {len(missing_copper_blocks['BUILDING_BLOCKS']['vertical_slabs'])}個")
total = len(missing_copper_blocks['ORE_BLOCKS']) + len(missing_copper_blocks['BUILDING_BLOCKS']['slabs']) + len(missing_copper_blocks['BUILDING_BLOCKS']['stairs']) + len(missing_copper_blocks['BUILDING_BLOCKS']['vertical_slabs'])
print(f"  合計: {total}個\n")

# Read current index.js
with open('scratch-client/scratch-vm/src/extensions/scratch3_minecraft/index.js', 'r', encoding='utf-8') as f:
    content = f.read()

category_names = ['BUILDING_BLOCKS', 'LIGHTING_BLOCKS', 'DECORATION_BLOCKS',
                  'NATURE_BLOCKS', 'FUNCTIONAL_BLOCKS', 'ORE_BLOCKS', 'SPECIAL_BLOCKS']

# Extract current blocks
categories = {}
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
            blocks.append({'text': text, 'value': value})
        categories[cat_name] = blocks

print('現在のブロック数:')
for cat_name in category_names:
    print(f"  {cat_name}: {len(categories[cat_name])}個")

# Add missing blocks to appropriate categories
print('\n銅系ブロックを追加中...')

# Add to ORE_BLOCKS
print('\n[ORE_BLOCKS]')
for block in missing_copper_blocks['ORE_BLOCKS']:
    # Check if already exists
    if not any(b['value'] == block['value'] for b in categories['ORE_BLOCKS']):
        categories['ORE_BLOCKS'].append(block)
        print(f"  追加: {block['value']:40s} - {block['text']}")
    else:
        print(f"  既存: {block['value']:40s} - {block['text']}")

# Add to BUILDING_BLOCKS (slabs, stairs, vertical_slabs)
print('\n[BUILDING_BLOCKS - Slabs]')
for block in missing_copper_blocks['BUILDING_BLOCKS']['slabs']:
    if not any(b['value'] == block['value'] for b in categories['BUILDING_BLOCKS']):
        categories['BUILDING_BLOCKS'].append(block)
        print(f"  追加: {block['value']:40s} - {block['text']}")
    else:
        print(f"  既存: {block['value']:40s} - {block['text']}")

print('\n[BUILDING_BLOCKS - Stairs]')
for block in missing_copper_blocks['BUILDING_BLOCKS']['stairs']:
    if not any(b['value'] == block['value'] for b in categories['BUILDING_BLOCKS']):
        categories['BUILDING_BLOCKS'].append(block)
        print(f"  追加: {block['value']:40s} - {block['text']}")
    else:
        print(f"  既存: {block['value']:40s} - {block['text']}")

print('\n[BUILDING_BLOCKS - Vertical Slabs (waxed)]')
for block in missing_copper_blocks['BUILDING_BLOCKS']['vertical_slabs']:
    if not any(b['value'] == block['value'] for b in categories['BUILDING_BLOCKS']):
        categories['BUILDING_BLOCKS'].append(block)
        print(f"  追加: {block['value']:40s} - {block['text']}")
    else:
        print(f"  既存: {block['value']:40s} - {block['text']}")

print('\n更新後のブロック数:')
for cat_name in category_names:
    print(f"  {cat_name}: {len(categories[cat_name])}個")

# Now we need to re-sort the categories with the shape-based grouping
# We'll use the same sorting logic from reorder_blocks_shape_based.py

WOOD_ORDER = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'bamboo', 'crimson', 'warped']

def get_wood_index(value):
    for i, wood in enumerate(WOOD_ORDER):
        if value.startswith(wood):
            return i
    return 999

def get_sort_key_building(block):
    """Sort key for BUILDING_BLOCKS - SHAPE-BASED GROUPING"""
    value = block['value']

    # GROUP 10: STAIRS
    if '_stairs' in value:
        # Copper stairs
        if 'copper' in value:
            copper_order = ['cut_copper', 'exposed_cut_copper', 'weathered_cut_copper', 'oxidized_cut_copper',
                           'waxed_cut_copper', 'waxed_exposed_cut_copper', 'waxed_weathered_cut_copper', 'waxed_oxidized_cut_copper']
            for i, copper_type in enumerate(copper_order):
                if value == f'{copper_type}_stairs':
                    return (10, 2, i, value)
            return (10, 2, 999, value)
        # Wood stairs
        if any(wood in value for wood in WOOD_ORDER):
            wood_idx = get_wood_index(value)
            return (10, 0, wood_idx, value)
        # Stone stairs
        stone_order = ['stone', 'cobblestone', 'andesite', 'diorite', 'granite', 'polished_andesite',
                       'polished_diorite', 'polished_granite', 'cobbled_deepslate', 'polished_deepslate',
                       'deepslate_brick', 'deepslate_tile', 'blackstone', 'polished_blackstone',
                       'polished_blackstone_brick', 'brick', 'end_stone_brick', 'nether_brick',
                       'red_nether_brick', 'sandstone', 'red_sandstone', 'smooth_sandstone',
                       'smooth_red_sandstone', 'prismarine', 'prismarine_brick', 'dark_prismarine',
                       'quartz', 'smooth_quartz', 'purpur', 'mud_brick']
        for i, stone in enumerate(stone_order):
            if stone in value:
                return (10, 1, i, value)
        return (10, 3, value)

    # GROUP 11: SLABS
    if '_slab' in value and not value.startswith('vertical_'):
        # Copper slabs
        if 'copper' in value:
            copper_order = ['cut_copper', 'exposed_cut_copper', 'weathered_cut_copper', 'oxidized_cut_copper',
                           'waxed_cut_copper', 'waxed_exposed_cut_copper', 'waxed_weathered_cut_copper', 'waxed_oxidized_cut_copper']
            for i, copper_type in enumerate(copper_order):
                if value == f'{copper_type}_slab':
                    return (11, 2, i, value)
            return (11, 2, 999, value)
        # Wood slabs
        if any(wood in value for wood in WOOD_ORDER):
            wood_idx = get_wood_index(value)
            return (11, 0, wood_idx, value)
        # Stone slabs
        stone_order = ['stone', 'smooth_stone', 'cobblestone', 'andesite', 'diorite', 'granite',
                       'polished_andesite', 'polished_diorite', 'polished_granite',
                       'cobbled_deepslate', 'polished_deepslate', 'deepslate_brick', 'deepslate_tile',
                       'blackstone', 'polished_blackstone', 'polished_blackstone_brick',
                       'brick', 'end_stone_brick', 'nether_brick', 'sandstone', 'cut_sandstone',
                       'red_sandstone', 'cut_red_sandstone', 'smooth_sandstone', 'smooth_red_sandstone',
                       'prismarine', 'prismarine_brick', 'dark_prismarine', 'quartz', 'smooth_quartz',
                       'purpur', 'mud_brick']
        for i, stone in enumerate(stone_order):
            if stone in value:
                return (11, 1, i, value)
        return (11, 3, value)

    # GROUP 12: VERTICAL SLABS
    if value.startswith('vertical_') and '_slab' in value:
        return (12, value)

    # GROUP 13: WALLS
    if '_wall' in value:
        wall_order = ['cobblestone', 'mossy_cobblestone', 'andesite', 'diorite', 'granite',
                      'cobbled_deepslate', 'polished_deepslate', 'deepslate_brick', 'deepslate_tile',
                      'blackstone', 'polished_blackstone', 'polished_blackstone_brick',
                      'brick', 'end_stone_brick', 'nether_brick', 'red_nether_brick',
                      'sandstone', 'red_sandstone', 'prismarine', 'mud_brick', 'stone_brick']
        for i, mat in enumerate(wall_order):
            if mat in value:
                return (13, i, value)
        return (13, 999, value)

    # GROUP 14: FENCES
    if '_fence' in value and '_gate' not in value:
        wood_idx = get_wood_index(value)
        return (14, wood_idx, value)

    # GROUP 15: FENCE GATES
    if '_fence_gate' in value:
        wood_idx = get_wood_index(value)
        return (15, wood_idx, value)

    # GROUP 16: BUTTONS
    if '_button' in value:
        if any(wood in value for wood in WOOD_ORDER):
            wood_idx = get_wood_index(value)
            return (16, 0, wood_idx, value)
        return (16, 1, value)

    # GROUP 17: PRESSURE PLATES
    if '_pressure_plate' in value or 'pressure_plate' in value:
        if any(wood in value for wood in WOOD_ORDER):
            wood_idx = get_wood_index(value)
            return (17, 0, wood_idx, value)
        if 'stone' in value:
            return (17, 1, 0, value)
        if 'heavy_weighted' in value:
            return (17, 1, 1, value)
        if 'light_weighted' in value:
            return (17, 1, 2, value)
        return (17, 2, value)

    # GROUP 18: DOORS
    if '_door' in value:
        if any(wood in value for wood in WOOD_ORDER):
            wood_idx = get_wood_index(value)
            return (18, 0, wood_idx, value)
        return (18, 1, value)

    # GROUP 19: TRAPDOORS
    if '_trapdoor' in value:
        if any(wood in value for wood in WOOD_ORDER):
            wood_idx = get_wood_index(value)
            return (19, 0, wood_idx, value)
        return (19, 1, value)

    # GROUP 1: BASIC BLOCKS
    basic = ['stone', 'cobblestone', 'terracotta']
    if value in basic:
        return (1, 1, basic.index(value), value)

    if '_planks' in value:
        wood_idx = get_wood_index(value)
        return (1, 2, wood_idx, value)

    if value.startswith('stripped_') and ('_log' in value or '_stem' in value):
        wood_idx = get_wood_index(value.replace('stripped_', ''))
        return (1, 3, wood_idx, value)

    if ('_wood' in value or '_hyphae' in value) and 'stripped' not in value:
        wood_idx = get_wood_index(value)
        return (1, 4, wood_idx, value)

    if value.startswith('stripped_') and ('_wood' in value or '_hyphae' in value):
        wood_idx = get_wood_index(value.replace('stripped_', ''))
        return (1, 5, wood_idx, value)

    stone_variants = [
        'andesite', 'polished_andesite',
        'diorite', 'polished_diorite',
        'granite', 'polished_granite',
        'calcite', 'tuff',
        'chiseled_deepslate', 'cobbled_deepslate', 'cracked_deepslate_bricks', 'cracked_deepslate_tiles',
        'deepslate', 'deepslate_bricks', 'deepslate_tiles', 'polished_deepslate',
        'basalt', 'polished_basalt',
        'blackstone', 'gilded_blackstone', 'polished_blackstone', 'polished_blackstone_bricks',
        'cracked_polished_blackstone_bricks'
    ]
    for i, stone in enumerate(stone_variants):
        if value == stone:
            return (1, 6, i, value)

    brick_blocks = ['bricks', 'end_stone_bricks', 'nether_bricks', 'red_nether_bricks',
                    'prismarine_bricks', 'quartz_bricks', 'stone_bricks', 'mossy_stone_bricks',
                    'cracked_stone_bricks', 'chiseled_stone_bricks', 'chiseled_nether_bricks',
                    'cracked_nether_bricks', 'mud_bricks']
    if value in brick_blocks:
        return (1, 7, brick_blocks.index(value), value)

    if '_concrete' in value and 'powder' not in value:
        color_order = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink',
                      'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']
        for i, color in enumerate(color_order):
            if value == f'{color}_concrete':
                return (1, 8, i, value)
        return (1, 8, 999, value)

    if 'prismarine' in value and '_slab' not in value and '_stairs' not in value and '_wall' not in value:
        prism_order = ['prismarine', 'prismarine_bricks', 'dark_prismarine']
        if value in prism_order:
            return (1, 9, prism_order.index(value), value)

    if 'quartz' in value and '_stairs' not in value and '_slab' not in value and 'ore' not in value:
        quartz_order = ['quartz_block', 'chiseled_quartz_block', 'quartz_pillar', 'quartz_bricks',
                       'smooth_quartz']
        if value in quartz_order:
            return (1, 10, quartz_order.index(value), value)

    if 'purpur' in value and '_stairs' not in value and '_slab' not in value:
        purpur_order = ['purpur_block', 'purpur_pillar']
        if value in purpur_order:
            return (1, 11, purpur_order.index(value), value)

    if value == 'bamboo_mosaic':
        return (1, 12, 0, value)

    sandstone_blocks = ['sandstone', 'chiseled_sandstone', 'cut_sandstone', 'smooth_sandstone',
                       'red_sandstone', 'chiseled_red_sandstone', 'cut_red_sandstone', 'smooth_red_sandstone']
    if value in sandstone_blocks:
        return (1, 13, sandstone_blocks.index(value), value)

    if value == 'smooth_stone':
        return (1, 14, 0, value)

    return (99, value)

def get_sort_key_ore(block):
    """Sort key for ORE_BLOCKS - GROUP BY TYPE"""
    value = block['value']

    ore_order = ['coal', 'iron', 'copper', 'gold', 'lapis', 'redstone', 'diamond', 'emerald', 'quartz']

    # Group 1: Overworld ores
    for i, ore in enumerate(ore_order):
        if value == f'{ore}_ore':
            return (1, i, value)

    # Group 2: Deepslate ores
    for i, ore in enumerate(ore_order):
        if value == f'deepslate_{ore}_ore':
            return (2, i, value)

    # Group 3: Nether ores
    if 'nether' in value and '_ore' in value:
        return (3, value)

    # Group 4: Mineral blocks (including copper variants)
    # Copper full blocks
    if 'copper' in value and '_ore' not in value and 'raw' not in value:
        copper_order = ['copper_block', 'cut_copper', 'exposed_copper', 'weathered_copper', 'oxidized_copper',
                       'waxed_copper_block', 'waxed_cut_copper', 'waxed_exposed_copper', 'waxed_weathered_copper', 'waxed_oxidized_copper']
        if value in copper_order:
            return (4, 2, copper_order.index(value), value)
        return (4, 2, 999, value)

    # Other mineral blocks
    for i, ore in enumerate(ore_order):
        if value == f'{ore}_block':
            return (4, 0, i, value)
    if value in ['netherite_block', 'amethyst_block']:
        return (4, 1, value)

    # Group 5: Raw ore blocks
    if 'raw_' in value and '_block' in value:
        raw_order = ['raw_iron_block', 'raw_copper_block', 'raw_gold_block']
        if value in raw_order:
            return (5, raw_order.index(value), value)
        return (5, 99, value)

    # Group 6: Other ore-related blocks
    return (99, value)

# Sort categories
print('\n形状別グループでソート中...')
sorted_categories = {}
for cat_name, blocks in categories.items():
    if cat_name == 'BUILDING_BLOCKS':
        sorted_blocks = sorted(blocks, key=get_sort_key_building)
        sorted_categories[cat_name] = sorted_blocks
    elif cat_name == 'ORE_BLOCKS':
        sorted_blocks = sorted(blocks, key=get_sort_key_ore)
        sorted_categories[cat_name] = sorted_blocks
    else:
        sorted_categories[cat_name] = blocks

print('ソート完了')

# Format arrays
def format_array(cat_name, blocks):
    lines = [f"const {cat_name} = ["]
    for i, block in enumerate(blocks):
        text = block['text'].replace("'", "\\'")
        value = block['value']
        if i < len(blocks) - 1:
            lines.append(f"{{\n  text: '{text}',\n  value: '{value}'\n}}, ")
        else:
            lines.append(f"{{\n  text: '{text}',\n  value: '{value}'\n}}")
    lines.append("];")
    return ''.join(lines)

# Update content
new_content = content

for cat_name in category_names:
    if cat_name in sorted_categories:
        blocks = sorted_categories[cat_name]
        new_array = format_array(cat_name, blocks)

        pattern = rf"const {cat_name} = \[.*?\];"
        new_content = re.sub(pattern, new_array, new_content, flags=re.DOTALL)

# Write back
with open('scratch-client/scratch-vm/src/extensions/scratch3_minecraft/index.js', 'w', encoding='utf-8') as f:
    f.write(new_content)

print('\n' + '='*80)
print('index.js更新完了！')
print('='*80)

# Save for gh-pages
with open('sorted_categories.json', 'w', encoding='utf-8') as f:
    json.dump(sorted_categories, f, ensure_ascii=False, indent=2)

print('sorted_categories.json保存完了')

print('\n最終ブロック数:')
for cat_name in category_names:
    print(f"  {cat_name}: {len(sorted_categories[cat_name])}個")

print(f"\n総ブロック数: {sum(len(blocks) for blocks in sorted_categories.values())}個")
