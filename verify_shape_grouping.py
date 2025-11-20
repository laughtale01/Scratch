import json

with open('sorted_categories.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

blocks = data['BUILDING_BLOCKS']

print('SHAPE GROUPING VERIFICATION:')
print('='*80)

groups = [
    ('Stairs', lambda b: '_stairs' in b['value']),
    ('Slabs', lambda b: '_slab' in b['value'] and not b['value'].startswith('vertical_')),
    ('Vertical slabs', lambda b: b['value'].startswith('vertical_') and '_slab' in b['value']),
    ('Walls', lambda b: '_wall' in b['value']),
    ('Fences', lambda b: '_fence' in b['value'] and '_gate' not in b['value']),
    ('Fence gates', lambda b: '_fence_gate' in b['value']),
    ('Buttons', lambda b: '_button' in b['value']),
    ('Pressure plates', lambda b: '_pressure_plate' in b['value'] or 'pressure_plate' in b['value']),
    ('Doors', lambda b: '_door' in b['value']),
    ('Trapdoors', lambda b: '_trapdoor' in b['value'])
]

for name, check in groups:
    indices = [i for i, b in enumerate(blocks) if check(b)]
    if indices:
        first_block = blocks[indices[0]]
        last_block = blocks[indices[-1]]
        print(f"{name:20s}: {len(indices):2d} blocks at indices {indices[0]:3d}-{indices[-1]:3d}")
        print(f"  First: {first_block['value']:35s} - {first_block['text']}")
        print(f"  Last:  {last_block['value']:35s} - {last_block['text']}")
        print()

print('='*80)
print(f"Total BUILDING_BLOCKS: {len(blocks)}")
