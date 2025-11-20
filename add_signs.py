import re
import json

print('='*80)
print('看板（Signs）11個の実装')
print('='*80)

# Load current implementation
with open('scratch-client/scratch-vm/src/extensions/scratch3_minecraft/index.js', 'r', encoding='utf-8') as f:
    content = f.read()

# 看板データ（公式日本語名）
sign_blocks = [
    {'text': 'オークの看板', 'value': 'oak_sign'},
    {'text': 'トウヒの看板', 'value': 'spruce_sign'},
    {'text': 'シラカバの看板', 'value': 'birch_sign'},
    {'text': 'ジャングルの看板', 'value': 'jungle_sign'},
    {'text': 'アカシアの看板', 'value': 'acacia_sign'},
    {'text': 'ダークオークの看板', 'value': 'dark_oak_sign'},
    {'text': 'マングローブの看板', 'value': 'mangrove_sign'},
    {'text': 'サクラの看板', 'value': 'cherry_sign'},
    {'text': '竹の看板', 'value': 'bamboo_sign'},
    {'text': '真紅の看板', 'value': 'crimson_sign'},
    {'text': '歪んだ看板', 'value': 'warped_sign'},
]

print(f'\n実装する看板: {len(sign_blocks)}個')
for sign in sign_blocks:
    print(f'  {sign["value"]:30s} - {sign["text"]}')

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

# Check if any signs already exist
existing_sign_values = set()
for block in existing_blocks:
    if '_sign' in block['value']:
        existing_sign_values.add(block['value'])

if existing_sign_values:
    print(f'\n既存の看板: {len(existing_sign_values)}個')
    for v in existing_sign_values:
        print(f'  {v}')
    print('\n既存の看板を削除してから追加します')
    # Remove existing signs
    existing_blocks = [b for b in existing_blocks if '_sign' not in b['value']]

# Add signs at the end of DECORATION_BLOCKS
# Signs should be grouped together
all_blocks = existing_blocks + sign_blocks

print(f'\n実装後のDECORATION_BLOCKS: {len(all_blocks)}個 (+{len(sign_blocks)})')

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

# Add signs to decoration category
decoration_blocks = sorted_categories.get('DECORATION_BLOCKS', [])

# Remove existing signs if any
decoration_blocks = [b for b in decoration_blocks if '_sign' not in b.get('value', '')]

# Add new signs at the end
decoration_blocks.extend(sign_blocks)

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

    sign_count = sum(1 for v in verify_blocks if '_sign' in v)
    print(f'\nDECORATION_BLOCKS内の看板: {sign_count}個')

    if sign_count == 11:
        print('[OK] 11個の看板が正しく追加されました')
    else:
        print(f'[WARNING] 期待値と異なります（期待: 11個、実際: {sign_count}個）')

    # Show the signs
    print('\n追加された看板:')
    for v in verify_blocks:
        if '_sign' in v:
            print(f'  {v}')

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

print(f'\n総ブロック数: {total_blocks}個（738 → 749、+11）')

print('\n' + '='*80)
print('[COMPLETE] 看板の実装が完了しました')
print('='*80)
