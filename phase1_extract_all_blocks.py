import re
import json
from collections import defaultdict

print('='*80)
print('フェーズ1: データ整合性チェック - 全ブロック抽出')
print('='*80)

# Load current implementation
with open('scratch-client/scratch-vm/src/extensions/scratch3_minecraft/index.js', 'r', encoding='utf-8') as f:
    content = f.read()

category_names = ['BUILDING_BLOCKS', 'LIGHTING_BLOCKS', 'DECORATION_BLOCKS',
                  'NATURE_BLOCKS', 'FUNCTIONAL_BLOCKS', 'ORE_BLOCKS', 'SPECIAL_BLOCKS']

# Extract all blocks from all categories
all_blocks_by_category = {}
all_blocks_flat = []

for cat_name in category_names:
    pattern = rf"const {cat_name} = \[(.*?)\];"
    match = re.search(pattern, content, re.DOTALL)

    if match:
        array_content = match.group(1)
        blocks = []
        block_pattern = r"\{\s*text:\s*'([^']+)',\s*value:\s*'([^']+)'\s*\}"

        for i, m in enumerate(re.finditer(block_pattern, array_content)):
            text = m.group(1).replace("\\'", "'")
            value = m.group(2)

            block_info = {
                'category': cat_name,
                'index_in_category': i,
                'text': text,
                'value': value
            }

            blocks.append(block_info)
            all_blocks_flat.append(block_info)

        all_blocks_by_category[cat_name] = blocks
        print(f'\n{cat_name}: {len(blocks)}個のブロックを抽出')
    else:
        print(f'\n{cat_name}: 見つかりません（エラー）')
        all_blocks_by_category[cat_name] = []

total_blocks = len(all_blocks_flat)
print(f'\n{"="*80}')
print(f'総ブロック数: {total_blocks}個')
print(f'{"="*80}')

# Save to JSON for detailed analysis
output_data = {
    'total_blocks': total_blocks,
    'categories': all_blocks_by_category,
    'flat_list': all_blocks_flat
}

with open('phase1_all_blocks.json', 'w', encoding='utf-8') as f:
    json.dump(output_data, f, ensure_ascii=False, indent=2)

print(f'\n全ブロックデータを phase1_all_blocks.json に保存しました')

# Create a summary
print(f'\n{"="*80}')
print('カテゴリ別サマリー:')
print(f'{"="*80}')

for cat_name in category_names:
    count = len(all_blocks_by_category[cat_name])
    percentage = (count / total_blocks * 100) if total_blocks > 0 else 0
    print(f'{cat_name:25s}: {count:4d}個 ({percentage:5.1f}%)')

print(f'\n{"="*80}')
print('抽出完了')
print(f'{"="*80}')
