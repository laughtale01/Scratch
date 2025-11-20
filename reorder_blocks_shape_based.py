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

# Wood type order for consistent sorting across shape groups
WOOD_ORDER = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'bamboo', 'crimson', 'warped']

def get_wood_index(value):
    """Get the index of wood type in value, returns 999 if not found"""
    for i, wood in enumerate(WOOD_ORDER):
        if value.startswith(wood):
            return i
    return 999

def get_sort_key_building(block):
    """Sort key for BUILDING_BLOCKS - SHAPE-BASED GROUPING"""
    value = block['value']

    # ===== GROUP 10: STAIRS (all stairs together) =====
    if '_stairs' in value:
        # Within stairs, sort by material
        if any(wood in value for wood in WOOD_ORDER):
            wood_idx = get_wood_index(value)
            return (10, 0, wood_idx, value)
        # Stone and other stairs
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
        return (10, 2, value)

    # ===== GROUP 11: SLABS (all horizontal slabs together) =====
    if '_slab' in value and not value.startswith('vertical_'):
        # Within slabs, sort by material
        if any(wood in value for wood in WOOD_ORDER):
            wood_idx = get_wood_index(value)
            return (11, 0, wood_idx, value)
        # Stone and other slabs
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
        return (11, 2, value)

    # ===== GROUP 12: VERTICAL SLABS (all vertical slabs together) =====
    if value.startswith('vertical_') and '_slab' in value:
        # Sort alphabetically within group
        return (12, value)

    # ===== GROUP 13: WALLS (all walls together) =====
    if '_wall' in value:
        # Sort by material type
        wall_order = ['cobblestone', 'mossy_cobblestone', 'andesite', 'diorite', 'granite',
                      'cobbled_deepslate', 'polished_deepslate', 'deepslate_brick', 'deepslate_tile',
                      'blackstone', 'polished_blackstone', 'polished_blackstone_brick',
                      'brick', 'end_stone_brick', 'nether_brick', 'red_nether_brick',
                      'sandstone', 'red_sandstone', 'prismarine', 'mud_brick', 'stone_brick']
        for i, mat in enumerate(wall_order):
            if mat in value:
                return (13, i, value)
        return (13, 999, value)

    # ===== GROUP 14: FENCES (all fences together, not gates) =====
    if '_fence' in value and '_gate' not in value:
        # Sort by wood type
        wood_idx = get_wood_index(value)
        return (14, wood_idx, value)

    # ===== GROUP 15: FENCE GATES (all fence gates together) =====
    if '_fence_gate' in value:
        # Sort by wood type
        wood_idx = get_wood_index(value)
        return (15, wood_idx, value)

    # ===== GROUP 16: BUTTONS (all buttons together) =====
    if '_button' in value:
        # Wood buttons first, then others
        if any(wood in value for wood in WOOD_ORDER):
            wood_idx = get_wood_index(value)
            return (16, 0, wood_idx, value)
        # Stone and other buttons
        return (16, 1, value)

    # ===== GROUP 17: PRESSURE PLATES (all pressure plates together) =====
    if '_pressure_plate' in value or 'pressure_plate' in value:
        # Wood plates first, then others
        if any(wood in value for wood in WOOD_ORDER):
            wood_idx = get_wood_index(value)
            return (17, 0, wood_idx, value)
        # Stone and weighted plates
        if 'stone' in value:
            return (17, 1, 0, value)
        if 'heavy_weighted' in value:
            return (17, 1, 1, value)
        if 'light_weighted' in value:
            return (17, 1, 2, value)
        return (17, 2, value)

    # ===== GROUP 18: DOORS (all doors together) =====
    if '_door' in value:
        # Wood doors first, then others
        if any(wood in value for wood in WOOD_ORDER):
            wood_idx = get_wood_index(value)
            return (18, 0, wood_idx, value)
        # Iron and other doors
        return (18, 1, value)

    # ===== GROUP 19: TRAPDOORS (all trapdoors together) =====
    if '_trapdoor' in value:
        # Wood trapdoors first, then others
        if any(wood in value for wood in WOOD_ORDER):
            wood_idx = get_wood_index(value)
            return (19, 0, wood_idx, value)
        # Iron and other trapdoors
        return (19, 1, value)

    # ===== GROUP 1: BASIC BLOCKS (full blocks without special shapes) =====
    # Subgroup 1.1: Basic base materials
    basic = ['stone', 'cobblestone', 'terracotta']
    if value in basic:
        return (1, 1, basic.index(value), value)

    # Subgroup 1.2: Planks
    if '_planks' in value:
        wood_idx = get_wood_index(value)
        return (1, 2, wood_idx, value)

    # Subgroup 1.3: Stripped logs/stems
    if value.startswith('stripped_') and ('_log' in value or '_stem' in value):
        wood_idx = get_wood_index(value.replace('stripped_', ''))
        return (1, 3, wood_idx, value)

    # Subgroup 1.4: Wood/hyphae (6-sided bark)
    if ('_wood' in value or '_hyphae' in value) and 'stripped' not in value:
        wood_idx = get_wood_index(value)
        return (1, 4, wood_idx, value)

    # Subgroup 1.5: Stripped wood/hyphae
    if value.startswith('stripped_') and ('_wood' in value or '_hyphae' in value):
        wood_idx = get_wood_index(value.replace('stripped_', ''))
        return (1, 5, wood_idx, value)

    # Subgroup 1.6: Stone variants (andesite, diorite, granite, etc.)
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

    # Subgroup 1.7: Bricks
    brick_blocks = ['bricks', 'end_stone_bricks', 'nether_bricks', 'red_nether_bricks',
                    'prismarine_bricks', 'quartz_bricks', 'stone_bricks', 'mossy_stone_bricks',
                    'cracked_stone_bricks', 'chiseled_stone_bricks', 'chiseled_nether_bricks',
                    'cracked_nether_bricks', 'mud_bricks']
    if value in brick_blocks:
        return (1, 7, brick_blocks.index(value), value)

    # Subgroup 1.8: Concrete blocks
    if '_concrete' in value and 'powder' not in value:
        color_order = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink',
                      'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']
        for i, color in enumerate(color_order):
            if value == f'{color}_concrete':
                return (1, 8, i, value)
        return (1, 8, 999, value)

    # Subgroup 1.9: Prismarine blocks
    if 'prismarine' in value and '_slab' not in value and '_stairs' not in value and '_wall' not in value:
        prism_order = ['prismarine', 'prismarine_bricks', 'dark_prismarine']
        if value in prism_order:
            return (1, 9, prism_order.index(value), value)

    # Subgroup 1.10: Quartz blocks
    if 'quartz' in value and '_stairs' not in value and '_slab' not in value and 'ore' not in value:
        quartz_order = ['quartz_block', 'chiseled_quartz_block', 'quartz_pillar', 'quartz_bricks',
                       'smooth_quartz']
        if value in quartz_order:
            return (1, 10, quartz_order.index(value), value)

    # Subgroup 1.11: Purpur blocks
    if 'purpur' in value and '_stairs' not in value and '_slab' not in value:
        purpur_order = ['purpur_block', 'purpur_pillar']
        if value in purpur_order:
            return (1, 11, purpur_order.index(value), value)

    # Subgroup 1.12: Special decorative blocks
    if value == 'bamboo_mosaic':
        return (1, 12, 0, value)

    # Subgroup 1.13: Sandstone variants
    sandstone_blocks = ['sandstone', 'chiseled_sandstone', 'cut_sandstone', 'smooth_sandstone',
                       'red_sandstone', 'chiseled_red_sandstone', 'cut_red_sandstone', 'smooth_red_sandstone']
    if value in sandstone_blocks:
        return (1, 13, sandstone_blocks.index(value), value)

    # Subgroup 1.14: Smooth stone
    if value == 'smooth_stone':
        return (1, 14, 0, value)

    # ===== GROUP 99: OTHER MISCELLANEOUS =====
    # Carpet, glazed terracotta, concrete powder, glass panes, beds, shulker boxes, etc.
    return (99, value)

def get_sort_key_lighting(block):
    """Sort key for LIGHTING_BLOCKS - GROUP BY TYPE"""
    value = block['value']

    # Group 1: Basic torches
    if value in ['torch', 'soul_torch']:
        return (1, ['torch', 'soul_torch'].index(value), value)

    # Group 2: Lanterns
    if 'lantern' in value and 'sea' not in value and 'jack' not in value:
        return (2, value)

    # Group 3: Glow blocks (natural light sources)
    if value in ['glowstone', 'sea_lantern', 'shroomlight']:
        return (3, value)

    # Group 4: Jack o lantern
    if 'jack_o_lantern' in value:
        return (4, value)

    # Group 5: Candles (grouped together)
    if 'candle' in value:
        color_order = ['candle', 'white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink',
                      'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']
        for i, color in enumerate(color_order):
            if value == f'{color}_candle' or (value == 'candle' and color == 'candle'):
                return (5, i, value)
        return (5, 99, value)

    # Group 6: Campfires
    if 'campfire' in value:
        return (6, value)

    # Group 7: Redstone light sources
    if 'redstone' in value and 'lamp' in value:
        return (7, 0, value)
    if 'redstone' in value and 'torch' in value:
        return (7, 1, value)

    # Group 8: Soul fire
    if value == 'soul_fire':
        return (8, value)

    # Default
    return (99, value)

def get_sort_key_decoration(block):
    """Sort key for DECORATION_BLOCKS - GROUP BY TYPE"""
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
    if '_carpet' in value or value == 'moss_carpet':
        if value == 'moss_carpet':
            return (2, -1, value)
        for i, color in enumerate(color_order):
            if value == f'{color}_carpet':
                return (2, i, value)
        return (2, 99, value)

    # Group 3: Terracotta (plain colored, not glazed)
    if 'terracotta' in value and 'glazed' not in value:
        if value == 'terracotta':
            return (3, -1, value)
        for i, color in enumerate(color_order):
            if value == f'{color}_terracotta':
                return (3, i, value)
        return (3, 99, value)

    # Group 4: Glazed terracotta
    if 'glazed_terracotta' in value:
        for i, color in enumerate(color_order):
            if value == f'{color}_glazed_terracotta':
                return (4, i, value)
        return (4, 99, value)

    # Group 5: Glass (plain and stained, full blocks not panes)
    if 'glass' in value and 'pane' not in value:
        if value == 'glass':
            return (5, -1, value)
        if 'stained_glass' in value:
            for i, color in enumerate(color_order):
                if value == f'{color}_stained_glass':
                    return (5, i, value)
        return (5, 99, value)

    # Group 6: Beds
    if '_bed' in value:
        for i, color in enumerate(color_order):
            if value == f'{color}_bed':
                return (6, i, value)
        return (6, 99, value)

    # Group 7: Shulker boxes
    if 'shulker_box' in value:
        if value == 'shulker_box':
            return (7, -1, value)
        for i, color in enumerate(color_order):
            if value == f'{color}_shulker_box':
                return (7, i, value)
        return (7, 99, value)

    # Group 8: Other decorative blocks
    other_deco = ['bookshelf', 'budding_amethyst', 'dripstone_block', 'flower_pot',
                 'item_frame', 'moss_block', 'painting', 'smooth_basalt']
    if value in other_deco:
        return (8, other_deco.index(value), value)

    # Default
    return (99, value)

def get_sort_key_nature(block):
    """Sort key for NATURE_BLOCKS - GROUP BY TYPE"""
    value = block['value']

    # Group 1: Dirt variants
    dirt_types = ['dirt', 'grass_block', 'coarse_dirt', 'podzol', 'mycelium', 'farmland', 'mud', 'rooted_dirt']
    if value in dirt_types:
        return (1, dirt_types.index(value), value)

    # Group 2: Sand and gravel
    sand_types = ['sand', 'red_sand', 'gravel', 'soul_sand', 'soul_soil']
    if value in sand_types:
        return (2, sand_types.index(value), value)

    # Group 3: Logs and stems (not stripped)
    if ('_log' in value or '_stem' in value) and 'stripped' not in value:
        wood_idx = get_wood_index(value)
        return (3, wood_idx, value)

    # Group 4: Leaves
    if '_leaves' in value:
        wood_idx = get_wood_index(value)
        return (4, wood_idx, value)

    # Group 5: Flowers
    flowers = ['poppy', 'dandelion', 'blue_orchid', 'allium', 'oxeye_daisy', 'cornflower',
              'lily_of_the_valley', 'lilac', 'rose_bush', 'peony', 'sunflower']
    for i, flower in enumerate(flowers):
        if flower in value:
            return (5, i, value)

    # Group 6: Ice and snow
    if 'ice' in value or 'snow' in value:
        ice_order = ['ice', 'packed_ice', 'blue_ice', 'snow', 'snow_block', 'powder_snow']
        if value in ice_order:
            return (6, ice_order.index(value), value)
        return (6, 99, value)

    # Group 7: Water
    if value == 'water':
        return (7, value)

    # Group 8: Plants and vegetation
    plants = ['bamboo', 'bamboo_block', 'cactus', 'sugar_cane', 'kelp', 'seagrass', 'grass',
             'fern', 'dead_bush', 'vine', 'twisting_vines', 'weeping_vines', 'glow_berries']
    if value in plants:
        return (8, plants.index(value), value)

    # Group 9: Other natural blocks
    return (99, value)

def get_sort_key_functional(block):
    """Sort key for FUNCTIONAL_BLOCKS - GROUP BY FUNCTION"""
    value = block['value']

    # Group 1: Crafting stations
    craft_blocks = ['crafting_table', 'furnace', 'blast_furnace', 'smoker', 'anvil',
                   'grindstone', 'stonecutter', 'smithing_table', 'cartography_table',
                   'fletching_table', 'loom', 'brewing_stand', 'enchanting_table']
    if value in craft_blocks:
        return (1, craft_blocks.index(value), value)

    # Group 2: Storage
    storage = ['chest', 'barrel', 'shulker_box', 'hopper']
    if value in storage:
        return (2, storage.index(value), value)

    # Group 3: Doors (iron and oak functional variants)
    if value in ['iron_door', 'oak_door']:
        return (3, ['iron_door', 'oak_door'].index(value), value)

    # Group 4: Trapdoors (iron and oak functional variants)
    if value in ['iron_trapdoor', 'oak_trapdoor']:
        return (4, ['iron_trapdoor', 'oak_trapdoor'].index(value), value)

    # Group 5: Redstone components
    redstone = ['redstone_block', 'redstone_torch', 'comparator', 'repeater', 'observer',
               'lever', 'target', 'dispenser', 'dropper']
    if value in redstone or ('redstone' in value and value not in ['redstone_ore', 'deepslate_redstone_ore']):
        if value in redstone:
            return (5, redstone.index(value), value)
        return (5, 99, value)

    # Group 6: Pistons
    if 'piston' in value:
        piston_order = ['piston', 'sticky_piston']
        if value in piston_order:
            return (6, piston_order.index(value), value)
        return (6, 99, value)

    # Group 7: Rails
    if 'rail' in value:
        rail_order = ['rail', 'powered_rail', 'detector_rail', 'activator_rail']
        if value in rail_order:
            return (7, rail_order.index(value), value)
        return (7, 99, value)

    # Group 8: Buttons and pressure plates (functional variants)
    if value in ['oak_button', 'stone_button', 'oak_pressure_plate', 'stone_pressure_plate']:
        return (8, ['oak_button', 'stone_button', 'oak_pressure_plate', 'stone_pressure_plate'].index(value), value)

    # Group 9: Other functional blocks
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

    # Group 4: Mineral blocks
    for i, ore in enumerate(ore_order):
        if value == f'{ore}_block':
            return (4, i, value)
    # Special blocks
    if value in ['netherite_block', 'amethyst_block']:
        return (4, 900, value)

    # Group 5: Raw ore blocks
    if 'raw_' in value and '_block' in value:
        raw_order = ['raw_iron_block', 'raw_copper_block', 'raw_gold_block']
        if value in raw_order:
            return (5, raw_order.index(value), value)
        return (5, 99, value)

    # Group 6: Other ore-related blocks
    return (99, value)

def get_sort_key_special(block):
    """Sort key for SPECIAL_BLOCKS - GROUP BY TYPE"""
    value = block['value']

    # Group 1: Air and barriers
    if value in ['air', 'barrier', 'structure_void', 'light']:
        return (1, ['air', 'barrier', 'structure_void', 'light'].index(value), value)

    # Group 2: Indestructible blocks
    if value in ['bedrock', 'obsidian', 'crying_obsidian']:
        return (2, ['bedrock', 'obsidian', 'crying_obsidian'].index(value), value)

    # Group 3: Sponge
    if 'sponge' in value:
        return (3, ['sponge', 'wet_sponge'].index(value) if value in ['sponge', 'wet_sponge'] else 99, value)

    # Group 4: Lava
    if value == 'lava':
        return (4, value)

    # Group 5: End blocks
    if 'end' in value and 'stone' not in value:
        end_order = ['end_portal', 'end_portal_frame', 'end_gateway', 'end_rod']
        if value in end_order:
            return (5, end_order.index(value), value)
        return (5, 99, value)

    # Group 6: End stone
    if value == 'end_stone':
        return (6, value)

    # Group 7: Nether blocks
    if value in ['netherrack', 'magma_block']:
        return (7, ['netherrack', 'magma_block'].index(value), value)

    # Group 8: Sculk blocks
    if 'sculk' in value:
        sculk_order = ['sculk', 'sculk_vein', 'sculk_sensor', 'sculk_catalyst', 'sculk_shrieker']
        if value in sculk_order:
            return (8, sculk_order.index(value), value)
        return (8, 99, value)

    # Group 9: Special technical blocks
    return (99, value)

# Apply sorting with new shape-based logic
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

print("\n" + "="*80)
print("APPLYING SHAPE-BASED GROUPING")
print("="*80)

for cat_name, blocks in categories.items():
    if cat_name in sort_functions:
        print(f"\nSorting {cat_name} with shape-based grouping...")
        sorted_blocks = sorted(blocks, key=sort_functions[cat_name])
        sorted_categories[cat_name] = sorted_blocks
        print(f"  {len(sorted_blocks)} blocks sorted")
    else:
        sorted_categories[cat_name] = blocks

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

print("\n" + "="*80)
print("index.js updated with SHAPE-BASED ordering!")
print("="*80)

# Save sorted order for gh-pages
with open('sorted_categories.json', 'w', encoding='utf-8') as f:
    json.dump(sorted_categories, f, ensure_ascii=False, indent=2)

print("Sorted categories saved to sorted_categories.json")

# Print sample from BUILDING_BLOCKS to verify shape grouping
print("\n" + "="*80)
print("SAMPLE: First 50 BUILDING_BLOCKS to verify shape grouping:")
print("="*80)
for i, block in enumerate(sorted_categories['BUILDING_BLOCKS'][:50]):
    print(f"{i+1:3d}. {block['value']:40s} - {block['text']}")

print("\n... (showing first 50 of {} blocks)".format(len(sorted_categories['BUILDING_BLOCKS'])))
