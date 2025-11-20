import re
import json

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
            text = m.group(1)
            value = m.group(2)
            blocks.append({'text': text, 'value': value})
        categories[cat_name] = blocks

print("Current blocks extracted")
for cat, blocks in categories.items():
    print(f"  {cat}: {len(blocks)} blocks")

# Define logical ordering rules for each category
def get_sort_key_building(block):
    """Sort key for BUILDING_BLOCKS"""
    value = block['value']

    # Group 1: Basic blocks
    basic = ['stone', 'dirt', 'grass_block', 'cobblestone', 'bedrock', 'sand', 'red_sand', 'gravel', 'clay', 'terracotta']
    if value in basic:
        return (1, basic.index(value), value)

    # Group 2: Planks (wood types)
    if '_planks' in value:
        wood_order = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'bamboo', 'crimson', 'warped']
        for i, wood in enumerate(wood_order):
            if value.startswith(wood):
                return (2, i, value)
        return (2, 99, value)

    # Group 3: Logs
    if '_log' in value or '_stem' in value:
        return (3, value)

    # Group 4: Wood blocks
    if '_wood' in value or '_hyphae' in value:
        return (4, value)

    # Group 5: Stone variants
    stone_types = ['andesite', 'diorite', 'granite', 'calcite', 'tuff', 'deepslate', 'basalt', 'blackstone']
    for stone in stone_types:
        if stone in value:
            return (5, stone_types.index(stone), value)

    # Group 6: Bricks
    if 'brick' in value and 'stairs' not in value and 'slab' not in value and 'wall' not in value:
        return (6, value)

    # Group 7: Concrete
    if 'concrete' in value and 'powder' not in value:
        return (7, value)

    # Group 8: Prismarine
    if 'prismarine' in value:
        return (8, value)

    # Group 9: Quartz
    if 'quartz' in value and 'ore' not in value:
        return (9, value)

    # Group 10: Purpur
    if 'purpur' in value:
        return (10, value)

    # Group 11: Stairs
    if '_stairs' in value:
        return (11, value)

    # Group 12: Slabs
    if '_slab' in value:
        return (12, value)

    # Group 13: Walls
    if '_wall' in value:
        return (13, value)

    # Group 14: Fences
    if 'fence' in value:
        return (14, value)

    # Group 15: Buttons
    if '_button' in value:
        return (15, value)

    # Group 16: Pressure plates
    if 'pressure_plate' in value:
        return (16, value)

    # Group 17: Doors
    if '_door' in value:
        return (17, value)

    # Group 18: Trapdoors
    if '_trapdoor' in value:
        return (18, value)

    # Default: alphabetical
    return (99, value)

def get_sort_key_lighting(block):
    """Sort key for LIGHTING_BLOCKS"""
    value = block['value']

    # Group 1: Basic torches
    if value in ['torch', 'soul_torch']:
        return (1, value)

    # Group 2: Glow blocks
    if value in ['glowstone', 'sea_lantern', 'shroomlight']:
        return (2, value)

    # Group 3: Lanterns
    if 'lantern' in value:
        return (3, value)

    # Group 4: Jack o lantern
    if 'jack_o_lantern' in value:
        return (4, value)

    # Group 5: Candles
    if 'candle' in value:
        color_order = ['candle', 'white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink',
                      'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']
        for i, color in enumerate(color_order):
            if value == f'{color}_candle' or value == 'candle':
                return (5, i, value)
        return (5, 99, value)

    # Default
    return (99, value)

def get_sort_key_decoration(block):
    """Sort key for DECORATION_BLOCKS"""
    value = block['value']

    color_order = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink',
                  'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']

    # Group 1: Wool
    if '_wool' in value:
        for i, color in enumerate(color_order):
            if value == f'{color}_wool':
                return (1, i, value)
        return (1, 99, value)

    # Group 2: Carpet
    if '_carpet' in value:
        for i, color in enumerate(color_order):
            if value == f'{color}_carpet':
                return (2, i, value)
        return (2, 99, value)

    # Group 3: Terracotta
    if 'terracotta' in value and 'glazed' not in value:
        for i, color in enumerate(color_order):
            if value == f'{color}_terracotta':
                return (3, i, value)
        if value == 'terracotta':
            return (3, -1, value)
        return (3, 99, value)

    # Group 4: Glazed terracotta
    if 'glazed_terracotta' in value:
        for i, color in enumerate(color_order):
            if value == f'{color}_glazed_terracotta':
                return (4, i, value)
        return (4, 99, value)

    # Group 5: Stained glass
    if 'stained_glass' in value and 'pane' not in value:
        for i, color in enumerate(color_order):
            if value == f'{color}_stained_glass':
                return (5, i, value)
        return (5, 99, value)

    # Group 6: Glass
    if value == 'glass':
        return (5, -1, value)

    # Group 7: Concrete
    if 'concrete' in value:
        return (7, value)

    # Group 8: Beds
    if '_bed' in value:
        for i, color in enumerate(color_order):
            if value == f'{color}_bed':
                return (8, i, value)
        return (8, 99, value)

    # Group 9: Shulker boxes
    if 'shulker_box' in value:
        for i, color in enumerate(color_order):
            if value == f'{color}_shulker_box':
                return (9, i, value)
        if value == 'shulker_box':
            return (9, -1, value)
        return (9, 99, value)

    # Default
    return (99, value)

def get_sort_key_nature(block):
    """Sort key for NATURE_BLOCKS"""
    value = block['value']

    # Group 1: Dirt variants
    dirt_types = ['dirt', 'grass_block', 'coarse_dirt', 'podzol', 'mycelium', 'farmland', 'mud']
    if value in dirt_types:
        return (1, dirt_types.index(value), value)

    # Group 2: Sand and gravel
    if value in ['sand', 'red_sand', 'gravel', 'soul_sand', 'soul_soil']:
        return (2, value)

    # Group 3: Logs
    if '_log' in value or '_stem' in value:
        return (3, value)

    # Group 4: Leaves
    if '_leaves' in value:
        return (4, value)

    # Group 5: Flowers and plants
    flowers = ['poppy', 'dandelion', 'orchid', 'allium', 'tulip', 'daisy', 'cornflower', 'lily']
    for flower in flowers:
        if flower in value:
            return (5, flowers.index(flower), value)

    # Group 6: Ice and snow
    if 'ice' in value or 'snow' in value:
        return (6, value)

    # Group 7: Water
    if value == 'water':
        return (7, value)

    # Default
    return (99, value)

def get_sort_key_functional(block):
    """Sort key for FUNCTIONAL_BLOCKS"""
    value = block['value']

    # Group 1: Crafting
    craft_blocks = ['crafting_table', 'furnace', 'blast_furnace', 'smoker', 'anvil', 'grindstone', 'stonecutter']
    if value in craft_blocks:
        return (1, craft_blocks.index(value), value)

    # Group 2: Storage
    if value in ['chest', 'barrel', 'shulker_box']:
        return (2, value)

    # Group 3: Doors
    if '_door' in value:
        return (3, value)

    # Group 4: Trapdoors
    if '_trapdoor' in value:
        return (4, value)

    # Group 5: Redstone
    if 'redstone' in value or 'repeater' in value or 'comparator' in value or 'observer' in value:
        return (5, value)

    # Group 6: Pistons
    if 'piston' in value:
        return (6, value)

    # Group 7: Rails
    if 'rail' in value:
        return (7, value)

    # Default
    return (99, value)

def get_sort_key_ore(block):
    """Sort key for ORE_BLOCKS"""
    value = block['value']

    ore_order = ['coal', 'iron', 'copper', 'gold', 'lapis', 'redstone', 'diamond', 'emerald', 'quartz', 'netherite', 'amethyst']

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

    # Group 4: Mineral blocks
    for i, ore in enumerate(ore_order):
        if value == f'{ore}_block':
            return (4, i, value)

    # Group 5: Raw blocks
    if 'raw_' in value:
        return (5, value)

    # Default
    return (99, value)

def get_sort_key_special(block):
    """Sort key for SPECIAL_BLOCKS"""
    value = block['value']

    # Group 1: Air and barriers
    if value in ['air', 'barrier', 'structure_void', 'light']:
        return (1, ['air', 'barrier', 'structure_void', 'light'].index(value), value)

    # Group 2: Bedrock and obsidian
    if value in ['bedrock', 'obsidian', 'crying_obsidian']:
        return (2, value)

    # Group 3: Sponge
    if 'sponge' in value:
        return (3, value)

    # Group 4: Lava
    if value == 'lava':
        return (4, value)

    # Group 5: End blocks
    if 'end_' in value:
        return (5, value)

    # Group 6: Nether blocks
    if value in ['netherrack', 'magma_block']:
        return (6, value)

    # Group 7: Sculk
    if 'sculk' in value:
        return (7, value)

    # Default
    return (99, value)

# Apply sorting
sort_functions = {
    'BUILDING_BLOCKS': get_sort_key_building,
    'LIGHTING_BLOCKS': get_sort_key_lighting,
    'DECORATION_BLOCKS': get_sort_key_decoration,
    'NATURE_BLOCKS': get_sort_key_nature,
    'FUNCTIONAL_BLOCKS': get_sort_key_functional,
    'ORE_BLOCKS': get_sort_key_ore,
    'SPECIAL_BLOCKS': get_sort_key_special
}

sorted_categories = {}

for cat_name, blocks in categories.items():
    if cat_name in sort_functions:
        sorted_blocks = sorted(blocks, key=sort_functions[cat_name])
        sorted_categories[cat_name] = sorted_blocks
    else:
        sorted_categories[cat_name] = blocks

print("\nBlocks sorted by logical groups")

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

print("\nindex.js updated with logical ordering!")

# Save sorted order for gh-pages
with open('sorted_categories.json', 'w', encoding='utf-8') as f:
    json.dump(sorted_categories, f, ensure_ascii=False, indent=2)

print("Sorted categories saved to sorted_categories.json")
