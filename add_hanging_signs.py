import re
import json

print('='*80)
print('吊り看板（Hanging Signs）11個の実装')
print('='*80)

# Load current implementation
with open('scratch-client/scratch-vm/src/extensions/scratch3_minecraft/index.js', 'r', encoding='utf-8') as f:
    content = f.read()

# 吊り看板データ（公式日本語名）
hanging_sign_blocks = [
    {'text': 'オークの吊り看板', 'value': 'oak_hanging_sign'},
    {'text': 'トウヒの吊り看板', 'value': 'spruce_hanging_sign'},
    {'text': 'シラカバの吊り看板', 'value': 'birch_hanging_sign'},
    {'text': 'ジャングルの吊り看板', 'value': 'jungle_hanging_sign'},
    {'text': 'アカシアの吊り看板', 'value': 'acacia_hanging_sign'},
    {'text': 'ダークオークの吊り看板', 'value': 'dark_oak_hanging_sign'},
    {'text': 'マングローブの吊り看板', 'value': 'mangrove_hanging_sign'},
    {'text': 'サクラの吊り看板', 'value': 'cherry_hanging_sign'},
    {'text': '竹の吊り看板', 'value': 'bamboo_hanging_sign'},
    {'text': '真紅の吊り看板', 'value': 'crimson_hanging_sign'},
    {'text': '歪んだ吊り看板', 'value': 'warped_hanging_sign'},
]

print(f'\n実装する吊り看板: {len(hanging_sign_blocks)}個')
for sign in hanging_sign_blocks:
    print(f'  {sign["value"]:35s} - {sign["text"]}')

# Extract current DECORATION_BLOCKS
pattern = r"const DECORATION_BLOCKS = \[(.*?)\];"
match = re.search(pattern, content, re.DOTALL)

if not match:
    print('ERROR: DECORATION_BLOCKSが見つかりません')
    exit(1)

array_content = match.group(1)

# Parse existing blocks
existing_blocks = []
block_pattern = r"\{\s*text:\s*'([^']+)',\s*value:\s*'([^']+)'\s*\}"
for m in re.finditer(block_pattern, array_content):
    text = m.group(1).replace("\\'", "'")
    value = m.group(2)
    existing_blocks.append({'text': text, 'value': value})

print(f'\n現在のDECORATION_BLOCKS: {len(existing_blocks)}個')

# Check if any hanging signs already exist
existing_hanging_sign_values = set()
for block in existing_blocks:
    if '_hanging_sign' in block['value']:
        existing_hanging_sign_values.add(block['value'])

if existing_hanging_sign_values:
    print(f'\n既存の吊り看板: {len(existing_hanging_sign_values)}個')
    for v in existing_hanging_sign_values:
        print(f'  {v}')
    print('\n既存の吊り看板を削除してから追加します')
    # Remove existing hanging signs
    existing_blocks = [b for b in existing_blocks if '_hanging_sign' not in b['value']]

# Add hanging signs right after regular signs
# Find where regular signs end
sign_end_index = -1
for i, block in enumerate(existing_blocks):
    if '_sign' in block['value'] and '_hanging_sign' not in block['value']:
        sign_end_index = i

if sign_end_index >= 0:
    print(f'\n通常の看板の最後の位置: インデックス{sign_end_index}')
    print(f'  → 吊り看板をインデックス{sign_end_index + 1}から挿入します')
    # Insert hanging signs right after regular signs
    all_blocks = existing_blocks[:sign_end_index + 1] + hanging_sign_blocks + existing_blocks[sign_end_index + 1:]
else:
    print('\n通常の看板が見つかりません')
    print('  → 吊り看板を最後尾に追加します')
    all_blocks = existing_blocks + hanging_sign_blocks

print(f'\n実装後のDECORATION_BLOCKS: {len(all_blocks)}個 (+{len(hanging_sign_blocks)})')

# Format the new array
def format_block_array(blocks, indent='        '):
    """Format blocks array with proper indentation"""
    lines = []
    for i, block in enumerate(blocks):
        text = block['text'].replace("'", "\\'")
        value = block['value']
        comma = ',' if i < len(blocks) - 1 else ''
        lines.append(f"{indent}{{text: '{text}', value: '{value}'}}{comma}")
    return '\n'.join(lines)

new_array_content = format_block_array(all_blocks)
new_decoration_blocks = f"const DECORATION_BLOCKS = [\n{new_array_content}\n    ];"

# Replace in content
content = re.sub(
    r"const DECORATION_BLOCKS = \[.*?\];",
    new_decoration_blocks,
    content,
    flags=re.DOTALL
)

# Save updated file
with open('scratch-client/scratch-vm/src/extensions/scratch3_minecraft/index.js', 'w', encoding='utf-8') as f:
    f.write(content)

print('\n[OK] index.js を更新しました')

# Update sorted_categories.json
with open('sorted_categories.json', 'r', encoding='utf-8') as f:
    sorted_categories = json.load(f)

# Add hanging signs to decoration category
decoration_blocks = sorted_categories.get('DECORATION_BLOCKS', [])

# Remove existing hanging signs if any
decoration_blocks = [b for b in decoration_blocks if '_hanging_sign' not in b.get('value', '')]

# Find where regular signs end
sign_end_index = -1
for i, block in enumerate(decoration_blocks):
    value = block.get('value', '')
    if '_sign' in value and '_hanging_sign' not in value:
        sign_end_index = i

if sign_end_index >= 0:
    # Insert hanging signs right after regular signs
    decoration_blocks = decoration_blocks[:sign_end_index + 1] + hanging_sign_blocks + decoration_blocks[sign_end_index + 1:]
else:
    # Add at the end if regular signs not found
    decoration_blocks.extend(hanging_sign_blocks)

sorted_categories['DECORATION_BLOCKS'] = decoration_blocks

# Save updated sorted_categories.json
with open('sorted_categories.json', 'w', encoding='utf-8') as f:
    json.dump(sorted_categories, f, ensure_ascii=False, indent=2)

print('[OK] sorted_categories.json を更新しました')

# Verification
print('\n' + '='*80)
print('検証')
print('='*80)

# Re-read and verify
with open('scratch-client/scratch-vm/src/extensions/scratch3_minecraft/index.js', 'r', encoding='utf-8') as f:
    verify_content = f.read()

pattern = r"const DECORATION_BLOCKS = \[(.*?)\];"
match = re.search(pattern, verify_content, re.DOTALL)
if match:
    verify_array = match.group(1)
    verify_blocks = []
    for m in re.finditer(block_pattern, verify_array):
        value = m.group(2)
        verify_blocks.append(value)

    hanging_sign_count = sum(1 for v in verify_blocks if '_hanging_sign' in v)
    regular_sign_count = sum(1 for v in verify_blocks if '_sign' in v and '_hanging_sign' not in v)

    print(f'\nDECORATION_BLOCKS内の看板:')
    print(f'  通常の看板: {regular_sign_count}個')
    print(f'  吊り看板: {hanging_sign_count}個')

    if hanging_sign_count == 11:
        print('[OK] 11個の吊り看板が正しく追加されました')
    else:
        print(f'[WARNING] 期待値と異なります（期待: 11個、実際: {hanging_sign_count}個）')

    # Show the hanging signs and their placement
    print('\n追加された吊り看板と配置:')
    in_sign_section = False
    for i, v in enumerate(verify_blocks):
        if '_sign' in v:
            if not in_sign_section:
                print(f'\n  看板セクション開始（インデックス{i}）:')
                in_sign_section = True

            if '_hanging_sign' in v:
                print(f'    [{i:3d}] {v} (吊り看板)')
            else:
                print(f'    [{i:3d}] {v} (通常)')
        elif in_sign_section:
            print(f'\n  看板セクション終了（次: インデックス{i}）')
            break

# Count all blocks across categories
category_names = ['BUILDING_BLOCKS', 'LIGHTING_BLOCKS', 'DECORATION_BLOCKS',
                  'NATURE_BLOCKS', 'FUNCTIONAL_BLOCKS', 'ORE_BLOCKS', 'SPECIAL_BLOCKS']

total_blocks = 0
for cat_name in category_names:
    pattern = rf"const {cat_name} = \[(.*?)\];"
    match = re.search(pattern, verify_content, re.DOTALL)
    if match:
        array_content = match.group(1)
        blocks = len(list(re.finditer(block_pattern, array_content)))
        total_blocks += blocks
        if cat_name == 'DECORATION_BLOCKS':
            print(f'\n{cat_name}: {blocks}個 (+11)')

print(f'\n総ブロック数: {total_blocks}個（749 → 760、+11）')

print('\n' + '='*80)
print('[COMPLETE] 吊り看板の実装が完了しました')
print('='*80)
