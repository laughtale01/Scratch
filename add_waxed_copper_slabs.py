import os
import json
import re

print('='*80)
print('Waxed銅垂直スラブの追加')
print('='*80)

# 追加するブロックの定義
waxed_copper_slabs = [
    {
        'id': 'waxed_vertical_copper_block_slab',
        'name': '錆止めされた銅ブロックのスラブ（垂直）',
        'base_block': 'COPPER_BLOCK',
        'texture': 'copper_block'
    },
    {
        'id': 'waxed_vertical_cut_copper_slab',
        'name': '錆止めされた切り込み入りの銅のスラブ（垂直）',
        'base_block': 'WAXED_CUT_COPPER',
        'texture': 'waxed_cut_copper'
    },
    {
        'id': 'waxed_vertical_exposed_cut_copper_slab',
        'name': '錆止めされた風化した切り込み入りの銅のスラブ（垂直）',
        'base_block': 'WAXED_EXPOSED_CUT_COPPER',
        'texture': 'waxed_exposed_cut_copper'
    },
    {
        'id': 'waxed_vertical_oxidized_cut_copper_slab',
        'name': '錆止めされた酸化した切り込み入りの銅のスラブ（垂直）',
        'base_block': 'WAXED_OXIDIZED_CUT_COPPER',
        'texture': 'waxed_oxidized_cut_copper'
    },
    {
        'id': 'waxed_vertical_weathered_cut_copper_slab',
        'name': '錆止めされた錆びた切り込み入りの銅のスラブ（垂直）',
        'base_block': 'WAXED_WEATHERED_CUT_COPPER',
        'texture': 'waxed_weathered_cut_copper'
    }
]

print(f'\n追加するブロック数: {len(waxed_copper_slabs)} 個\n')

# ========================================
# 1. ModBlocks.javaに追加
# ========================================
print('【1. ModBlocks.java の更新】')
print('-'*80)

modblocks_path = 'minecraft-mod/src/main/java/com/github/minecraftedu/init/ModBlocks.java'

with open(modblocks_path, 'r', encoding='utf-8') as f:
    modblocks_content = f.read()

# 最後のブロック定義の後に追加（vertical_oxidized_cut_copper_slabの後）
insertion_point = modblocks_content.find('    public static final RegistryObject<Block> VERTICAL_OXIDIZED_CUT_COPPER_SLAB')
if insertion_point == -1:
    print('[ERROR] VERTICAL_OXIDIZED_CUT_COPPER_SLAB が見つかりません')
    exit(1)

# その定義の終わりを探す
end_point = modblocks_content.find(');', insertion_point) + 2

# 新しいブロック定義を生成
new_blocks = '\n\n    // ========================================\n'
new_blocks += '    // 錆止めされた銅系垂直スラブ (Waxed Copper Vertical Slabs)\n'
new_blocks += '    // ========================================\n'

for slab in waxed_copper_slabs:
    constant_name = slab['id'].upper()
    new_blocks += f'''
    public static final RegistryObject<Block> {constant_name} = BLOCKS.register(
        "{slab['id']}",
        () -> new VerticalSlabBlock(
            BlockBehaviour.Properties.copy(Blocks.{slab['base_block']})
                .requiresCorrectToolForDrops()
        )
    );
'''

# 挿入
modblocks_content = modblocks_content[:end_point] + new_blocks + modblocks_content[end_point:]

# 保存
with open(modblocks_path, 'w', encoding='utf-8') as f:
    f.write(modblocks_content)

print(f'[OK] ModBlocks.java に {len(waxed_copper_slabs)} 個のブロックを追加')

# ========================================
# 2. ModItems.javaに追加
# ========================================
print('\n【2. ModItems.java の更新】')
print('-'*80)

moditems_path = 'minecraft-mod/src/main/java/com/github/minecraftedu/init/ModItems.java'

with open(moditems_path, 'r', encoding='utf-8') as f:
    moditems_content = f.read()

# vertical_oxidized_cut_copper_slabの後に追加
insertion_point = moditems_content.find('        "vertical_oxidized_cut_copper_slab"')
if insertion_point == -1:
    print('[ERROR] vertical_oxidized_cut_copper_slab が見つかりません')
    exit(1)

# 行の終わりを探す
end_point = moditems_content.find('\n', insertion_point) + 1

# 新しいアイテムを生成
new_items = ''
for slab in waxed_copper_slabs:
    constant_name = slab['id'].upper()
    new_items += f'        "{slab["id"]}",\n'
    new_items += f'        ModBlocks.{constant_name},\n'

# 挿入
moditems_content = moditems_content[:end_point] + new_items + moditems_content[end_point:]

# 保存
with open(moditems_path, 'w', encoding='utf-8') as f:
    f.write(moditems_content)

print(f'[OK] ModItems.java に {len(waxed_copper_slabs)} 個のアイテムを追加')

# ========================================
# 3. Blockstateファイルを作成
# ========================================
print('\n【3. Blockstate ファイルの作成】')
print('-'*80)

blockstates_dir = 'minecraft-mod/src/main/resources/assets/minecraftedu/blockstates'

for slab in waxed_copper_slabs:
    blockstate = {
        "variants": {
            "facing=north,waterlogged=false": {
                "model": f"minecraftedu:block/{slab['id']}"
            },
            "facing=north,waterlogged=true": {
                "model": f"minecraftedu:block/{slab['id']}"
            },
            "facing=south,waterlogged=false": {
                "model": f"minecraftedu:block/{slab['id']}",
                "y": 180
            },
            "facing=south,waterlogged=true": {
                "model": f"minecraftedu:block/{slab['id']}",
                "y": 180
            },
            "facing=east,waterlogged=false": {
                "model": f"minecraftedu:block/{slab['id']}",
                "y": 90
            },
            "facing=east,waterlogged=true": {
                "model": f"minecraftedu:block/{slab['id']}",
                "y": 90
            },
            "facing=west,waterlogged=false": {
                "model": f"minecraftedu:block/{slab['id']}",
                "y": 270
            },
            "facing=west,waterlogged=true": {
                "model": f"minecraftedu:block/{slab['id']}",
                "y": 270
            }
        }
    }

    filepath = os.path.join(blockstates_dir, f"{slab['id']}.json")
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(blockstate, f, indent=2)

    print(f'[OK] {slab["id"]}.json 作成')

# ========================================
# 4. Block Modelファイルを作成
# ========================================
print('\n【4. Block Model ファイルの作成】')
print('-'*80)

block_models_dir = 'minecraft-mod/src/main/resources/assets/minecraftedu/models/block'

for slab in waxed_copper_slabs:
    block_model = {
        "parent": "block/block",
        "textures": {
            "texture": f"minecraft:block/{slab['texture']}",
            "particle": f"minecraft:block/{slab['texture']}"
        },
        "elements": [
            {
                "from": [0, 0, 0],
                "to": [16, 16, 8],
                "faces": {
                    "north": {
                        "texture": "#texture",
                        "cullface": "north"
                    },
                    "south": {
                        "texture": "#texture"
                    },
                    "east": {
                        "texture": "#texture",
                        "cullface": "east",
                        "uv": [0, 0, 8, 16]
                    },
                    "west": {
                        "texture": "#texture",
                        "cullface": "west",
                        "uv": [0, 0, 8, 16]
                    },
                    "up": {
                        "texture": "#texture",
                        "cullface": "up",
                        "uv": [0, 0, 16, 8]
                    },
                    "down": {
                        "texture": "#texture",
                        "cullface": "down",
                        "uv": [0, 0, 16, 8]
                    }
                }
            }
        ]
    }

    filepath = os.path.join(block_models_dir, f"{slab['id']}.json")
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(block_model, f, indent=2)

    print(f'[OK] block/{slab["id"]}.json 作成')

# ========================================
# 5. Item Modelファイルを作成
# ========================================
print('\n【5. Item Model ファイルの作成】')
print('-'*80)

item_models_dir = 'minecraft-mod/src/main/resources/assets/minecraftedu/models/item'

for slab in waxed_copper_slabs:
    item_model = {
        "parent": f"minecraftedu:block/{slab['id']}"
    }

    filepath = os.path.join(item_models_dir, f"{slab['id']}.json")
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(item_model, f, indent=2)

    print(f'[OK] item/{slab["id"]}.json 作成')

# ========================================
# 6. 言語ファイルに翻訳を追加
# ========================================
print('\n【6. 言語ファイルの更新】')
print('-'*80)

lang_path = 'minecraft-mod/src/main/resources/assets/minecraftedu/lang/ja_jp.json'

with open(lang_path, 'r', encoding='utf-8') as f:
    lang_data = json.load(f)

# 新しい翻訳を追加
for slab in waxed_copper_slabs:
    key = f"block.minecraftedu.{slab['id']}"
    lang_data[key] = slab['name']

# ソートして保存
sorted_lang_data = dict(sorted(lang_data.items()))

with open(lang_path, 'w', encoding='utf-8') as f:
    json.dump(sorted_lang_data, f, indent=2, ensure_ascii=False)

print(f'[OK] ja_jp.json に {len(waxed_copper_slabs)} 個の翻訳を追加')

# ========================================
# 完了
# ========================================
print('\n' + '='*80)
print('【完了】')
print('='*80)
print(f'\n追加したファイル:')
print(f'  - ModBlocks.java: {len(waxed_copper_slabs)} 個のブロック定義')
print(f'  - ModItems.java: {len(waxed_copper_slabs)} 個のアイテム定義')
print(f'  - Blockstates: {len(waxed_copper_slabs)} 個のJSONファイル')
print(f'  - Block models: {len(waxed_copper_slabs)} 個のJSONファイル')
print(f'  - Item models: {len(waxed_copper_slabs)} 個のJSONファイル')
print(f'  - 言語ファイル: {len(waxed_copper_slabs)} 個の翻訳')

print(f'\n次のステップ:')
print(f'  1. cd minecraft-mod')
print(f'  2. ./gradlew build')
print(f'  3. 新しいJARファイルをMinecraftのmodsフォルダにコピー')

print('\n' + '='*80)
