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

# Collect all block values
all_block_values = set()
for cat_name, blocks in all_blocks.items():
    for block in blocks:
        all_block_values.add(block['value'])

print('='*80)
print('Minecraftの重要な欠落ブロックを調査')
print('='*80)

# Check for signs (wall signs and standing signs are same item in inventory)
print('\n【看板（Signs）】')
wood_types = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak',
              'mangrove', 'cherry', 'bamboo', 'crimson', 'warped']
missing_signs = []
for wood in wood_types:
    sign_name = f'{wood}_sign'
    if sign_name not in all_block_values:
        missing_signs.append(wood)

print(f'不足している看板: {len(missing_signs)}/11種類')
for wood in missing_signs:
    print(f'  {wood}_sign')

# Check for hanging signs (1.20+)
print('\n【吊り看板（Hanging Signs）- Minecraft 1.20+】')
missing_hanging_signs = []
for wood in wood_types:
    hanging_sign_name = f'{wood}_hanging_sign'
    if hanging_sign_name not in all_block_values:
        missing_hanging_signs.append(wood)

print(f'不足している吊り看板: {len(missing_hanging_signs)}/11種類')
for wood in missing_hanging_signs:
    print(f'  {wood}_hanging_sign')

# Check for bamboo and nether wood logs
print('\n【竹とネザーの木材の原木/幹】')
nether_bamboo_woods = [('bamboo', '竹'), ('crimson', '真紅'), ('warped', '歪んだ')]
missing_logs = []
for wood, jp_name in nether_bamboo_woods:
    log_name = f'{wood}_log' if wood == 'bamboo' else f'{wood}_stem'
    wood_name = f'{wood}_wood' if wood == 'bamboo' else f'{wood}_hyphae'

    if log_name not in all_block_values:
        missing_logs.append((log_name, f'{jp_name}の原木/幹'))
    if wood_name not in all_block_values:
        missing_logs.append((wood_name, f'{jp_name}の木材/菌糸'))

if missing_logs:
    print(f'不足: {len(missing_logs)}個')
    for block_id, jp_name in missing_logs:
        print(f'  {block_id} - {jp_name}')
else:
    print('すべて実装済み')

# Check for stripped logs
print('\n【樹皮を剥いだ原木（Stripped Logs）】')
all_woods = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak',
             'mangrove', 'cherry', 'bamboo', 'crimson', 'warped']
missing_stripped = []
for wood in all_woods:
    if wood in ['crimson', 'warped']:
        stripped_log = f'stripped_{wood}_stem'
        stripped_wood = f'stripped_{wood}_hyphae'
    else:
        stripped_log = f'stripped_{wood}_log'
        stripped_wood = f'stripped_{wood}_wood'

    if stripped_log not in all_block_values:
        missing_stripped.append(stripped_log)
    if stripped_wood not in all_block_values:
        missing_stripped.append(stripped_wood)

if missing_stripped:
    print(f'不足: {len(missing_stripped)}個')
    for block_id in missing_stripped[:10]:  # Show first 10
        print(f'  {block_id}')
    if len(missing_stripped) > 10:
        print(f'  ... 他 {len(missing_stripped) - 10}個')
else:
    print('すべて実装済み')

# Check for other important decoration blocks
print('\n【その他の重要な装飾ブロック】')
important_blocks = {
    'lectern': '書見台',
    'composter': 'コンポスター',
    'barrel': '樽',
    'smoker': '燻製器',
    'blast_furnace': '溶鉱炉',
    'grindstone': '砥石',
    'stonecutter': 'ストーンカッター',
    'loom': '機織り機',
    'cartography_table': '製図台',
    'smithing_table': '鍛冶台',
    'fletching_table': '矢細工台',
    'scaffolding': '足場',
    'chain': '鎖',
    'lantern': 'ランタン',
    'soul_lantern': 'ソウルランタン',
    'bell': '鐘',
    'anvil': '金床',
    'chipped_anvil': 'ひびの入った金床',
    'damaged_anvil': '破損した金床',
}

missing_important = []
for block_id, jp_name in important_blocks.items():
    if block_id not in all_block_values:
        missing_important.append((block_id, jp_name))

if missing_important:
    print(f'不足: {len(missing_important)}個')
    for block_id, jp_name in missing_important:
        print(f'  {block_id} - {jp_name}')
else:
    print('すべて実装済み')

# Check for banners
print('\n【旗（Banners）】')
colors = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink',
          'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']
missing_banners = []
for color in colors:
    banner_name = f'{color}_banner'
    if banner_name not in all_block_values:
        missing_banners.append(color)

if missing_banners:
    print(f'不足: {len(missing_banners)}/16色')
    if len(missing_banners) <= 5:
        for color in missing_banners:
            print(f'  {color}_banner')
else:
    print('すべて実装済み（16/16色）')

# Check for shulker boxes
print('\n【シュルカーボックス（Shulker Boxes）】')
missing_shulker = []
if 'shulker_box' not in all_block_values:
    missing_shulker.append('shulker_box（無染色）')
for color in colors:
    shulker_name = f'{color}_shulker_box'
    if shulker_name not in all_block_values:
        missing_shulker.append(color)

if missing_shulker:
    print(f'不足: {len(missing_shulker)}個')
    if len(missing_shulker) <= 5:
        for item in missing_shulker:
            print(f'  {item}')
    else:
        print(f'  すべての色が不足（0/17実装）')
else:
    print('すべて実装済み（17/17）')

# Summary
print('\n' + '='*80)
print('【総合サマリー】')
print('='*80)
total_missing = len(missing_signs) + len(missing_hanging_signs) + len(missing_logs) + len(missing_stripped) + len(missing_important) + len(missing_banners) + len(missing_shulker)
print(f'\n合計不足ブロック数（概算）: {total_missing}個以上\n')

print('優先度順のおすすめ:')
print(f'  1. 看板（Signs）: {len(missing_signs)}個 - ゲームプレイ上重要、頻繁に使用')
print(f'  2. 吊り看板（Hanging Signs）: {len(missing_hanging_signs)}個 - 1.20新機能、装飾性高い')
print(f'  3. 樹皮を剥いだ原木: {len(missing_stripped)}個 - 建築に重要')
print(f'  4. 竹/ネザー原木: {len(missing_logs)}個 - 木材系の完全性')
print(f'  5. 重要な機能ブロック: {len(missing_important)}個 - 村人職業、機能性')
print(f'  6. 旗: {len(missing_banners)}個 - 装飾、マーカー用')
print(f'  7. シュルカーボックス: {len(missing_shulker)}個 - 収納、持ち運び')
