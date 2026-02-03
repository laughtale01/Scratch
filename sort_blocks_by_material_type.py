# -*- coding: utf-8 -*-
"""
ブロックリストを形状カテゴリ内で素材タイプごとにソートするスクリプト
階段の例: 木製階段→石系階段→レンガ系階段→銅系階段...
"""

import re

# gui.jsを読み込み
with open('gui.js', 'r', encoding='utf-8') as f:
    content = f.read()

# 木材の種類（順序付き）
WOOD_TYPES = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'bamboo', 'bamboo_mosaic', 'crimson', 'warped']
# ネザー木材
NETHER_WOOD = ['crimson', 'warped']
# 石の種類
STONE_TYPES = ['stone', 'cobblestone', 'mossy_cobblestone', 'smooth_stone']
# 磨かれた石系
POLISHED_STONE_TYPES = ['andesite', 'polished_andesite', 'diorite', 'polished_diorite', 'granite', 'polished_granite']
# ディープスレート系
DEEPSLATE_TYPES = ['deepslate', 'cobbled_deepslate', 'polished_deepslate', 'deepslate_bricks', 'deepslate_brick', 'deepslate_tiles', 'deepslate_tile', 'chiseled_deepslate', 'cracked_deepslate_bricks', 'cracked_deepslate_tiles']
# ブラックストーン系
BLACKSTONE_TYPES = ['blackstone', 'polished_blackstone', 'polished_blackstone_bricks', 'polished_blackstone_brick', 'gilded_blackstone', 'cracked_polished_blackstone_bricks']
# レンガ系
BRICK_TYPES = ['brick', 'bricks', 'stone_brick', 'stone_bricks', 'mossy_stone_bricks', 'cracked_stone_bricks', 'chiseled_stone_bricks', 'nether_brick', 'nether_bricks', 'red_nether_brick', 'red_nether_bricks', 'chiseled_nether_bricks', 'cracked_nether_bricks', 'end_stone_brick', 'end_stone_bricks', 'mud_brick', 'mud_bricks', 'prismarine_brick', 'prismarine_bricks', 'quartz_bricks']
# 砂岩系
SANDSTONE_TYPES = ['sandstone', 'red_sandstone', 'chiseled_sandstone', 'chiseled_red_sandstone', 'cut_sandstone', 'cut_red_sandstone', 'smooth_sandstone', 'smooth_red_sandstone']
# 銅系（順序付き）
COPPER_TYPES = ['copper', 'cut_copper', 'exposed_copper', 'exposed_cut_copper', 'weathered_copper', 'weathered_cut_copper', 'oxidized_copper', 'oxidized_cut_copper', 'waxed_copper', 'waxed_cut_copper', 'waxed_exposed_copper', 'waxed_exposed_cut_copper', 'waxed_weathered_copper', 'waxed_weathered_cut_copper', 'waxed_oxidized_copper', 'waxed_oxidized_cut_copper']
# プリズマリン系
PRISMARINE_TYPES = ['prismarine', 'prismarine_brick', 'prismarine_bricks', 'dark_prismarine']
# クォーツ系
QUARTZ_TYPES = ['quartz', 'smooth_quartz', 'chiseled_quartz_block', 'quartz_pillar', 'quartz_bricks']
# パープル系
PURPUR_TYPES = ['purpur', 'purpur_block', 'purpur_pillar']
# 色順（Minecraft標準）
COLOR_ORDER = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']

def get_shape_category(block):
    """ブロック名から形状カテゴリを判定"""
    if (block.startswith('vertical_') or block.startswith('waxed_vertical_')) and '_slab' in block:
        return (4, '縦スラブ')
    if block.endswith('_stairs'):
        return (1, '階段')
    if block.endswith('_slab'):
        return (2, '横スラブ')
    if block.endswith('_wall'):
        return (5, '壁')
    if block.endswith('_fence_gate'):
        return (7, 'フェンスゲート')
    if block.endswith('_fence'):
        return (6, 'フェンス')
    if block.endswith('_button'):
        return (8, 'ボタン')
    if block.endswith('_pressure_plate'):
        return (9, '感圧板')
    if block.endswith('_trapdoor'):
        return (11, 'トラップドア')
    if block.endswith('_door'):
        return (10, 'ドア')
    if block.endswith('_carpet'):
        return (12, 'カーペット')
    if block.endswith('_concrete_powder'):
        return (13, 'コンクリートパウダー')
    if block.endswith('_glazed_terracotta'):
        return (14, '彩釉テラコッタ')
    if block.endswith('_shulker_box'):
        return (15, 'シュルカーボックス')
    if block.endswith('_stained_glass_pane'):
        return (16, '色付きガラス板')
    if block.endswith('_bed'):
        return (17, 'ベッド')
    return (0, '通常ブロック')

def get_material_type_order(block):
    """素材タイプの優先順位を取得"""
    # 形状サフィックスを除去してベース名を取得
    base = block
    for suffix in ['_stairs', '_slab', '_wall', '_fence_gate', '_fence', '_button', '_pressure_plate', '_door', '_trapdoor', '_carpet', '_concrete_powder', '_glazed_terracotta', '_shulker_box', '_stained_glass_pane', '_bed', '_concrete']:
        if base.endswith(suffix):
            base = base[:-len(suffix)]
            break

    # 縦スラブのプレフィックス除去
    if base.startswith('vertical_'):
        base = base[9:]
    if base.startswith('waxed_vertical_'):
        base = base[15:]

    # 色付きアイテム（カーペット、コンクリート、etc）
    for i, color in enumerate(COLOR_ORDER):
        if block.startswith(color + '_'):
            return (90, i, block)

    # 木製（優先順位1）
    for i, wood in enumerate(WOOD_TYPES):
        if base == wood or base.startswith(wood + '_') or block.startswith(wood + '_'):
            return (10, i, block)

    # 剥いだ原木・木材
    if base.startswith('stripped_'):
        stripped_base = base[9:]
        for i, wood in enumerate(WOOD_TYPES):
            if stripped_base == wood or stripped_base.startswith(wood + '_'):
                return (11, i, block)

    # 石系（優先順位2）
    for i, stone in enumerate(STONE_TYPES):
        if base == stone or base.startswith(stone):
            return (20, i, block)

    # 磨かれた石系
    for i, stone in enumerate(POLISHED_STONE_TYPES):
        if base == stone or base.startswith(stone):
            return (21, i, block)

    # ディープスレート系（優先順位3）
    for i, ds in enumerate(DEEPSLATE_TYPES):
        if base == ds or base.startswith(ds) or ds in base:
            return (30, i, block)

    # ブラックストーン系
    for i, bs in enumerate(BLACKSTONE_TYPES):
        if base == bs or base.startswith(bs) or bs in base:
            return (31, i, block)

    # レンガ系（優先順位4）
    for i, brick in enumerate(BRICK_TYPES):
        if base == brick or base.startswith(brick) or brick in base:
            return (40, i, block)

    # 砂岩系（優先順位5）
    for i, ss in enumerate(SANDSTONE_TYPES):
        if base == ss or base.startswith(ss):
            return (50, i, block)

    # 銅系（優先順位6）
    for i, cu in enumerate(COPPER_TYPES):
        if base == cu or base.startswith(cu) or cu in base:
            return (60, i, block)
    # waxed銅も
    if 'copper' in base or 'waxed' in block:
        for i, cu in enumerate(COPPER_TYPES):
            if cu in block:
                return (60, i, block)

    # プリズマリン系
    for i, pr in enumerate(PRISMARINE_TYPES):
        if base == pr or base.startswith(pr) or pr in base:
            return (70, i, block)

    # クォーツ系
    for i, q in enumerate(QUARTZ_TYPES):
        if base == q or base.startswith(q) or q in base:
            return (71, i, block)

    # パープル系
    for i, p in enumerate(PURPUR_TYPES):
        if base == p or base.startswith(p) or p in base:
            return (72, i, block)

    # 鉄・石ボタン
    if base == 'iron' or block.startswith('iron_'):
        return (80, 0, block)
    if base == 'stone' or block == 'stone_button' or block == 'stone_pressure_plate':
        return (80, 1, block)

    # その他
    return (99, 0, block)

def sort_blocks_by_material(blocks):
    """ブロックリストを形状カテゴリ→素材タイプでソート"""
    def sort_key(block):
        shape_order, shape_name = get_shape_category(block)
        material_order = get_material_type_order(block)
        return (shape_order, material_order)

    return sorted(blocks, key=sort_key)

# BUILDING_BLOCKS_DATA を抽出してソート
building_match = re.search(r"const BUILDING_BLOCKS_DATA = \[([^\]]+)\];", content)
if building_match:
    array_content = building_match.group(1)
    blocks = re.findall(r"'([^']+)'", array_content)
    print(f"BUILDING_BLOCKS_DATA: {len(blocks)} ブロック")

    # ソート
    sorted_blocks = sort_blocks_by_material(blocks)

    # 結果の一部を表示
    print("\n【ソート後の並び順（各カテゴリの最初の10個）】")
    current_shape = None
    count = 0
    for b in sorted_blocks:
        shape = get_shape_category(b)[1]
        if shape != current_shape:
            if current_shape:
                print("  ...")
            print(f"\n{shape}:")
            current_shape = shape
            count = 0
        if count < 10:
            print(f"  {b}")
            count += 1

    # 新しい配列文字列を生成
    new_array = "const BUILDING_BLOCKS_DATA = ['" + "', '".join(sorted_blocks) + "'];"

    # 置換
    old_array = building_match.group(0)
    content = content.replace(old_array, new_array)
    print("\n\nBUILDING_BLOCKS_DATA を更新しました")
else:
    print("BUILDING_BLOCKS_DATA が見つかりませんでした")

# DIRECTIONAL_BLOCKS_DATA も同様にソート
directional_match = re.search(r"const DIRECTIONAL_BLOCKS_DATA = \[([^\]]+)\];", content)
if directional_match:
    array_content = directional_match.group(1)
    blocks = re.findall(r"'([^']+)'", array_content)
    print(f"\nDIRECTIONAL_BLOCKS_DATA: {len(blocks)} ブロック")

    sorted_blocks = sort_blocks_by_material(blocks)

    new_array = "const DIRECTIONAL_BLOCKS_DATA = ['" + "', '".join(sorted_blocks) + "'];"
    old_array = directional_match.group(0)
    content = content.replace(old_array, new_array)
    print("DIRECTIONAL_BLOCKS_DATA を更新しました")

# 保存
with open('gui.js', 'w', encoding='utf-8') as f:
    f.write(content)

print("\nDone!")
