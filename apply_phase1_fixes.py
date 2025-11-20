import re
import json

print('='*80)
print('フェーズ1: データ整合性修正の適用')
print('='*80)

# Load current implementation
with open('scratch-client/scratch-vm/src/extensions/scratch3_minecraft/index.js', 'r', encoding='utf-8') as f:
    content = f.read()

# Define all 14 fixes
# Priority: High (3 fixes)
# Priority: Medium (5 fixes)
# Priority: Low (6 fixes - 4 vertical slabs + 2 already counted above)

fixes = {
    'high_priority': [
        {
            'id': 1,
            'block_id': 'weathered_copper',
            'category': 'ORE_BLOCKS',
            'old_text': '風化した銅',
            'new_text': '錆びた銅',
            'reason': '銅の酸化段階: weathered = 錆び'
        },
        {
            'id': 2,
            'block_id': 'oxidized_copper',
            'category': 'ORE_BLOCKS',
            'old_text': '錆びた銅',
            'new_text': '酸化した銅',
            'reason': '銅の酸化段階: oxidized = 酸化'
        },
        {
            'id': 3,
            'block_id': 'azalea',
            'category': 'NATURE_BLOCKS',
            'old_text': 'ツツジの葉',
            'new_text': 'ツツジ',
            'reason': 'azalea（植物）とazalea_leaves（葉）の区別'
        },
    ],
    'medium_priority': [
        {
            'id': 4,
            'block_id': 'raw_copper_block',
            'category': 'ORE_BLOCKS',
            'old_text': '銅の原石',
            'new_text': '銅の原石ブロック',
            'reason': '「ブロック」が抜けている'
        },
        {
            'id': 5,
            'block_id': 'raw_iron_block',
            'category': 'ORE_BLOCKS',
            'old_text': '鉄の原石',
            'new_text': '鉄の原石ブロック',
            'reason': '「ブロック」が抜けている'
        },
        {
            'id': 6,
            'block_id': 'raw_gold_block',
            'category': 'ORE_BLOCKS',
            'old_text': '金の原石',
            'new_text': '金の原石ブロック',
            'reason': '「ブロック」が抜けている'
        },
        {
            'id': 7,
            'block_id': 'waxed_copper_block',
            'category': 'ORE_BLOCKS',
            'old_text': 'ワックスを塗った銅',
            'new_text': '錆止めされた銅ブロック',
            'reason': 'waxed = 錆止めされた、統一'
        },
        {
            'id': 8,
            'block_id': 'waxed_cut_copper',
            'category': 'ORE_BLOCKS',
            'old_text': 'ワックスを塗った切り込み入りの銅',
            'new_text': '錆止めされた切り込み入りの銅',
            'reason': 'waxed = 錆止めされた、統一'
        },
    ],
    'low_priority': [
        {
            'id': 9,
            'block_id': 'vertical_cut_copper_slab',
            'category': 'BUILDING_BLOCKS',
            'old_text': '切り込み入り銅のスラブ（垂直）',
            'new_text': '切り込み入りの銅のスラブ（垂直）',
            'reason': '助詞「の」が抜けている'
        },
        {
            'id': 10,
            'block_id': 'vertical_exposed_cut_copper_slab',
            'category': 'BUILDING_BLOCKS',
            'old_text': '風化した切り込み入り銅のスラブ（垂直）',
            'new_text': '風化した切り込み入りの銅のスラブ（垂直）',
            'reason': '助詞「の」が抜けている'
        },
        {
            'id': 11,
            'block_id': 'vertical_weathered_cut_copper_slab',
            'category': 'BUILDING_BLOCKS',
            'old_text': '錆びた切り込み入り銅のスラブ（垂直）',
            'new_text': '錆びた切り込み入りの銅のスラブ（垂直）',
            'reason': '助詞「の」が抜けている'
        },
        {
            'id': 12,
            'block_id': 'vertical_oxidized_cut_copper_slab',
            'category': 'BUILDING_BLOCKS',
            'old_text': '酸化した切り込み入り銅のスラブ（垂直）',
            'new_text': '酸化した切り込み入りの銅のスラブ（垂直）',
            'reason': '助詞「の」が抜けている'
        },
    ]
}

# Flatten all fixes
all_fixes = []
for priority in ['high_priority', 'medium_priority', 'low_priority']:
    all_fixes.extend(fixes[priority])

print(f'\n修正対象: {len(all_fixes)}個の誤り')

# Apply fixes one by one
print(f'\n{"="*80}')
print('修正の適用')
print(f'{"="*80}')

fixes_applied = 0
fixes_not_found = []

for fix in all_fixes:
    fix_id = fix['id']
    block_id = fix['block_id']
    old_text = fix['old_text']
    new_text = fix['new_text']
    reason = fix['reason']

    # Create the exact pattern to match (multi-line format in index.js)
    # Pattern:
    # text: 'old_text',
    # value: 'block_id'
    pattern = r"text:\s*'" + re.escape(old_text) + r"',\s*\n\s*value:\s*'" + re.escape(block_id) + r"'"
    replacement = "text: '" + new_text + "',\n  value: '" + block_id + "'"

    # Check if pattern exists
    matches = re.findall(pattern, content, re.MULTILINE)

    if matches:
        # Apply the fix
        content = re.sub(pattern, replacement, content, flags=re.MULTILINE)
        fixes_applied += 1
        print(f'\n[{fix_id:2d}] [OK] {block_id}')
        print(f'     変更前: {old_text}')
        print(f'     変更後: {new_text}')
        print(f'     理由: {reason}')
    else:
        fixes_not_found.append(fix)
        print(f'\n[{fix_id:2d}] [NOT FOUND] {block_id}')
        print(f'     探した文字列: {old_text}')
        print(f'     ※パターンが見つかりませんでした')

# Save the fixed content
with open('scratch-client/scratch-vm/src/extensions/scratch3_minecraft/index.js', 'w', encoding='utf-8') as f:
    f.write(content)

print(f'\n{"="*80}')
print('修正結果')
print(f'{"="*80}')
print(f'適用された修正: {fixes_applied}/{len(all_fixes)}個')
if fixes_not_found:
    print(f'見つからなかった修正: {len(fixes_not_found)}個')
    for fix in fixes_not_found:
        print(f'  - {fix["block_id"]}: {fix["old_text"]}')
else:
    print('[OK] 全ての修正が正常に適用されました')

# Update sorted_categories.json with the same fixes
print(f'\n{"="*80}')
print('sorted_categories.jsonの更新')
print(f'{"="*80}')

with open('sorted_categories.json', 'r', encoding='utf-8') as f:
    sorted_categories = json.load(f)

json_fixes_applied = 0
for category_name, blocks in sorted_categories.items():
    for i, block in enumerate(blocks):
        block_value = block.get('value', '')
        block_text = block.get('text', '')

        # Find if this block needs fixing
        for fix in all_fixes:
            if fix['block_id'] == block_value and fix['old_text'] == block_text:
                # Apply fix
                sorted_categories[category_name][i]['text'] = fix['new_text']
                json_fixes_applied += 1
                print(f'[OK] {block_value}: {fix["old_text"]} → {fix["new_text"]}')
                break

with open('sorted_categories.json', 'w', encoding='utf-8') as f:
    json.dump(sorted_categories, f, ensure_ascii=False, indent=2)

print(f'\n適用された修正: {json_fixes_applied}個')

print(f'\n{"="*80}')
print('修正完了')
print(f'{"="*80}')
print(f'index.js: {fixes_applied}個の修正を適用')
print(f'sorted_categories.json: {json_fixes_applied}個の修正を適用')
