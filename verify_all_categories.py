import json

with open('sorted_categories.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

print('='*80)
print('COMPREHENSIVE SHAPE-BASED GROUPING VERIFICATION')
print('='*80)

# BUILDING_BLOCKS - Show shape groups
print('\n1. BUILDING_BLOCKS (403 blocks) - Shape-based grouping:')
print('-'*80)
blocks = data['BUILDING_BLOCKS']
# Show a few examples from each shape group
print(f"  Basic blocks (first 10):")
for i in range(10):
    print(f"    {blocks[i]['value']:35s} - {blocks[i]['text']}")

print(f"\n  Stairs group (showing first 5 and last 5 of 41):")
stairs = [b for b in blocks if '_stairs' in b['value']]
for i in range(5):
    print(f"    {stairs[i]['value']:35s} - {stairs[i]['text']}")
print("    ...")
for i in range(-5, 0):
    print(f"    {stairs[i]['value']:35s} - {stairs[i]['text']}")

print(f"\n  Slabs group (showing first 5 and last 5 of 44):")
slabs = [b for b in blocks if '_slab' in b['value'] and not b['value'].startswith('vertical_')]
for i in range(5):
    print(f"    {slabs[i]['value']:35s} - {slabs[i]['text']}")
print("    ...")
for i in range(-5, 0):
    print(f"    {slabs[i]['value']:35s} - {slabs[i]['text']}")

print(f"\n  Vertical slabs group (showing first 3 and last 3 of 34):")
vslabs = [b for b in blocks if b['value'].startswith('vertical_') and '_slab' in b['value']]
for i in range(3):
    print(f"    {vslabs[i]['value']:35s} - {vslabs[i]['text']}")
print("    ...")
for i in range(-3, 0):
    print(f"    {vslabs[i]['value']:35s} - {vslabs[i]['text']}")

print(f"\n  Walls group (showing first 3 and last 3 of 21):")
walls = [b for b in blocks if '_wall' in b['value']]
for i in range(3):
    print(f"    {walls[i]['value']:35s} - {walls[i]['text']}")
print("    ...")
for i in range(-3, 0):
    print(f"    {walls[i]['value']:35s} - {walls[i]['text']}")

print(f"\n  Fences group (all 12):")
fences = [b for b in blocks if '_fence' in b['value'] and '_gate' not in b['value']]
for fence in fences:
    print(f"    {fence['value']:35s} - {fence['text']}")

# LIGHTING_BLOCKS - Show type groups
print('\n2. LIGHTING_BLOCKS (30 blocks) - Grouped by light source type:')
print('-'*80)
blocks = data['LIGHTING_BLOCKS']
print(f"  Torches (first 2):")
for i in range(2):
    print(f"    {blocks[i]['value']:35s} - {blocks[i]['text']}")

candles = [b for b in blocks if 'candle' in b['value']]
print(f"\n  Candles group (showing first 5 and last 3 of {len(candles)}):")
for i in range(5):
    print(f"    {candles[i]['value']:35s} - {candles[i]['text']}")
print("    ...")
for i in range(-3, 0):
    print(f"    {candles[i]['value']:35s} - {candles[i]['text']}")

# DECORATION_BLOCKS - Show type groups
print('\n3. DECORATION_BLOCKS (81 blocks) - Grouped by decoration type:')
print('-'*80)
blocks = data['DECORATION_BLOCKS']

wool = [b for b in blocks if '_wool' in b['value']]
print(f"  Wool group (showing first 5 and last 3 of {len(wool)}):")
for i in range(5):
    print(f"    {wool[i]['value']:35s} - {wool[i]['text']}")
print("    ...")
for i in range(-3, 0):
    print(f"    {wool[i]['value']:35s} - {wool[i]['text']}")

carpet = [b for b in blocks if '_carpet' in b['value'] or b['value'] == 'moss_carpet']
print(f"\n  Carpet group (showing first 5 of {len(carpet)}):")
for i in range(5):
    print(f"    {carpet[i]['value']:35s} - {carpet[i]['text']}")

# NATURE_BLOCKS - Show natural type groups
print('\n4. NATURE_BLOCKS (78 blocks) - Grouped by natural category:')
print('-'*80)
blocks = data['NATURE_BLOCKS']

print(f"  Dirt variants (first 7):")
for i in range(7):
    print(f"    {blocks[i]['value']:35s} - {blocks[i]['text']}")

logs = [b for b in blocks if ('_log' in b['value'] or '_stem' in b['value']) and 'stripped' not in b['value']]
print(f"\n  Logs/stems group (showing first 5 of {len(logs)}):")
for i in range(5):
    print(f"    {logs[i]['value']:35s} - {logs[i]['text']}")

leaves = [b for b in blocks if '_leaves' in b['value']]
print(f"\n  Leaves group (showing first 5 of {len(leaves)}):")
for i in range(5):
    print(f"    {leaves[i]['value']:35s} - {leaves[i]['text']}")

# FUNCTIONAL_BLOCKS - Show function groups
print('\n5. FUNCTIONAL_BLOCKS (52 blocks) - Grouped by function:')
print('-'*80)
blocks = data['FUNCTIONAL_BLOCKS']

print(f"  Crafting stations (first 8):")
for i in range(8):
    print(f"    {blocks[i]['value']:35s} - {blocks[i]['text']}")

rails = [b for b in blocks if 'rail' in b['value']]
print(f"\n  Rails group (all {len(rails)}):")
for rail in rails:
    print(f"    {rail['value']:35s} - {rail['text']}")

# ORE_BLOCKS - Show ore type groups
print('\n6. ORE_BLOCKS (38 blocks) - Grouped by ore type:')
print('-'*80)
blocks = data['ORE_BLOCKS']

print(f"  Overworld ores (first 8):")
for i in range(8):
    print(f"    {blocks[i]['value']:35s} - {blocks[i]['text']}")

deepslate = [b for b in blocks if 'deepslate_' in b['value'] and '_ore' in b['value']]
print(f"\n  Deepslate ores (all {len(deepslate)}):")
for ore in deepslate:
    print(f"    {ore['value']:35s} - {ore['text']}")

# SPECIAL_BLOCKS - Show special type groups
print('\n7. SPECIAL_BLOCKS (31 blocks) - Grouped by special category:')
print('-'*80)
blocks = data['SPECIAL_BLOCKS']

print(f"  Air/barriers (first 4):")
for i in range(4):
    print(f"    {blocks[i]['value']:35s} - {blocks[i]['text']}")

sculk = [b for b in blocks if 'sculk' in b['value']]
print(f"\n  Sculk blocks (all {len(sculk)}):")
for block in sculk:
    print(f"    {block['value']:35s} - {block['text']}")

print('\n' + '='*80)
print('SUMMARY:')
print('='*80)
for cat_name, blocks in data.items():
    print(f"{cat_name:25s}: {len(blocks):3d} blocks")

print('\nTotal blocks across all categories:', sum(len(blocks) for blocks in data.values()))
print('='*80)
