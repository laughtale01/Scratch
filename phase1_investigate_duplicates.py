import json

print('='*80)
print('重複日本語名の詳細調査')
print('='*80)

# Load all blocks
with open('phase1_all_blocks.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

all_blocks = data['flat_list']

# Find the specific duplicate blocks
print('\n【問題1】ツツジの葉')
print('-'*80)

azalea_blocks = [b for b in all_blocks if 'azalea' in b['value'].lower()]
for block in azalea_blocks:
    print(f"  {block['value']:30s} - {block['text']:30s} ({block['category']})")

print('\nMinecraft Wikiでの正式名称:')
print('  azalea_leaves → ツツジの葉')
print('  azalea        → ツツジ（植物ブロック）')
print('\n[ERROR] 問題: azaleaの日本語名が間違っています')
print('  現在: ツツジの葉')
print('  正解: ツツジ')

print('\n' + '='*80)
print('【問題2】風化した銅 vs 錆びた銅')
print('-'*80)

copper_blocks = [b for b in all_blocks if b['category'] == 'ORE_BLOCKS' and 'copper' in b['value']]
for block in sorted(copper_blocks, key=lambda x: x['index_in_category']):
    print(f"  [{block['index_in_category']:2d}] {block['value']:35s} - {block['text']}")

print('\nMinecraft Wikiでの銅の酸化段階（正式名称）:')
print('  copper_block      → 銅ブロック')
print('  exposed_copper    → 風化した銅')
print('  weathered_copper  → 錆びた銅')
print('  oxidized_copper   → 酸化した銅')

print('\n[ERROR] 問題: weathered_copperの日本語名が間違っています')
print('  現在: 風化した銅')
print('  正解: 錆びた銅')

print('\n' + '='*80)
print('修正が必要な箇所:')
print('='*80)
print('\n1. azalea の日本語名:')
print('   変更前: ツツジの葉')
print('   変更後: ツツジ')
print('\n2. weathered_copper の日本語名:')
print('   変更前: 風化した銅')
print('   変更後: 錆びた銅')

# Save fix recommendations
fixes = {
    'duplicate_japanese_names': {
        'azalea': {
            'current_text': 'ツツジの葉',
            'correct_text': 'ツツジ',
            'reason': 'azalea（植物）とazalea_leaves（葉）は別物',
            'category': 'NATURE_BLOCKS'
        },
        'weathered_copper': {
            'current_text': '風化した銅',
            'correct_text': '錆びた銅',
            'reason': 'exposed_copper（風化）とweathered_copper（錆び）は酸化段階が異なる',
            'category': 'ORE_BLOCKS'
        }
    }
}

with open('phase1_fixes_needed.json', 'w', encoding='utf-8') as f:
    json.dump(fixes, f, ensure_ascii=False, indent=2)

print('\n修正推奨内容を phase1_fixes_needed.json に保存しました')

print('\n' + '='*80)
print('調査完了')
print('='*80)
