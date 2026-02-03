# -*- coding: utf-8 -*-
"""
ブロックリストを形状カテゴリ内で素材タイプごとにソートするスクリプト（改良版）
階段の例: 木製階段→石系階段→レンガ系階段→銅系階段...
"""

import re

# gui.jsを読み込み
with open('gui.js', 'r', encoding='utf-8') as f:
    content = f.read()

# 木材の種類（Minecraft標準順）
WOOD_ORDER = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'bamboo', 'bamboo_mosaic', 'crimson', 'warped']

# 色順（Minecraft標準）
COLOR_ORDER = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']

# 色付きブロックのサフィックス（これらのサフィックスを持つブロックのみ色でソート）
COLORED_SUFFIXES = ['_carpet', '_concrete_powder', '_concrete', '_glazed_terracotta', '_shulker_box', '_stained_glass_pane', '_stained_glass', '_bed', '_wool', '_terracotta', '_candle']

def is_colored_block(block):
    """色付きブロックかどうか判定"""
    # 色付きサフィックスを持つブロックのみ
    for suffix in COLORED_SUFFIXES:
        if block.endswith(suffix):
            # ただし、特定のプレフィックス（glazed_terracotta自体など）は除外
            for color in COLOR_ORDER:
                if block.startswith(color + '_'):
                    return True
    return False

def get_color_index(block):
    """色付きブロックの色インデックスを取得"""
    for i, color in enumerate(COLOR_ORDER):
        if block.startswith(color + '_'):
            return i
    return 99

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

def get_base_name(block):
    """形状サフィックスを除去してベース名を取得"""
    suffixes = ['_stairs', '_slab', '_wall', '_fence_gate', '_fence', '_button', '_pressure_plate', '_door', '_trapdoor', '_carpet', '_concrete_powder', '_glazed_terracotta', '_shulker_box', '_stained_glass_pane', '_bed', '_concrete']
    for suffix in suffixes:
        if block.endswith(suffix):
            return block[:-len(suffix)]
    return block

def get_material_order(block):
    """素材タイプの優先順位を取得"""
    base = get_base_name(block)

    # 縦スラブのプレフィックス除去
    original_base = base
    if base.startswith('vertical_'):
        base = base[9:]
    if base.startswith('waxed_vertical_'):
        base = base[15:]

    # 色付きブロック（カーペット、コンクリート、etc）は色順でソート
    if is_colored_block(block):
        return (90, get_color_index(block), 0, block)

    # === 木製 (10-19) ===
    for i, wood in enumerate(WOOD_ORDER):
        # 完全一致
        if base == wood:
            return (10, i, 0, block)
        # 木材の板・木
        if base == wood + '_planks' or base == wood + '_wood':
            return (10, i, 1, block)
        # 木材のプレフィックス（bamboo_mosaic含む）
        if base.startswith(wood + '_'):
            return (10, i, 2, block)

    # ネザー木材（菌糸）
    if base == 'crimson_hyphae' or base == 'warped_hyphae':
        return (10, WOOD_ORDER.index('crimson') if 'crimson' in base else WOOD_ORDER.index('warped'), 3, block)
    if base == 'stripped_crimson_hyphae' or base == 'stripped_warped_hyphae':
        return (11, WOOD_ORDER.index('crimson') if 'crimson' in base else WOOD_ORDER.index('warped'), 0, block)

    # 剥いだ原木・木材
    if base.startswith('stripped_'):
        stripped_base = base[9:]
        for i, wood in enumerate(WOOD_ORDER):
            if stripped_base == wood + '_log' or stripped_base == wood + '_wood' or stripped_base == wood + '_stem' or stripped_base.startswith(wood):
                return (11, i, 0, block)

    # === 石系 (20-29) ===
    # 基本の石
    if base in ['stone', 'smooth_stone']:
        return (20, 0, 0, block)
    if base in ['cobblestone', 'mossy_cobblestone']:
        return (20, 1, 0 if base == 'cobblestone' else 1, block)

    # 安山岩・閃緑岩・花崗岩
    stone_variants = ['andesite', 'polished_andesite', 'diorite', 'polished_diorite', 'granite', 'polished_granite']
    for i, sv in enumerate(stone_variants):
        if base == sv:
            return (21, i, 0, block)

    # === ディープスレート (30) ===
    deepslate_order = ['deepslate', 'cobbled_deepslate', 'polished_deepslate', 'deepslate_bricks', 'deepslate_brick', 'deepslate_tiles', 'deepslate_tile', 'chiseled_deepslate', 'cracked_deepslate_bricks', 'cracked_deepslate_tiles']
    for i, ds in enumerate(deepslate_order):
        if base == ds or (ds in base and 'deepslate' in base):
            return (30, i, 0, block)

    # === ブラックストーン (31) ===
    blackstone_order = ['blackstone', 'polished_blackstone', 'polished_blackstone_bricks', 'polished_blackstone_brick', 'gilded_blackstone', 'cracked_polished_blackstone_bricks']
    for i, bs in enumerate(blackstone_order):
        if base == bs or bs in base:
            return (31, i, 0, block)

    # === レンガ系 (40) ===
    # 通常レンガ
    if base in ['brick', 'bricks']:
        return (40, 0, 0, block)
    # 石レンガ
    if base in ['stone_brick', 'stone_bricks', 'mossy_stone_bricks', 'cracked_stone_bricks', 'chiseled_stone_bricks']:
        order = ['stone_brick', 'stone_bricks', 'mossy_stone_bricks', 'cracked_stone_bricks', 'chiseled_stone_bricks']
        return (40, 1, order.index(base) if base in order else 0, block)
    # ネザーレンガ
    if base in ['nether_brick', 'nether_bricks', 'red_nether_brick', 'red_nether_bricks', 'chiseled_nether_bricks', 'cracked_nether_bricks']:
        order = ['nether_brick', 'nether_bricks', 'red_nether_brick', 'red_nether_bricks', 'chiseled_nether_bricks', 'cracked_nether_bricks']
        return (40, 2, order.index(base) if base in order else 0, block)
    # エンドストーンレンガ
    if 'end_stone_brick' in base:
        return (40, 3, 0, block)
    # 泥レンガ
    if 'mud_brick' in base:
        return (40, 4, 0, block)
    # プリズマリンレンガ
    if 'prismarine_brick' in base:
        return (40, 5, 0, block)
    # クォーツレンガ
    if base == 'quartz_bricks':
        return (40, 6, 0, block)

    # === 砂岩系 (50) ===
    sandstone_order = ['sandstone', 'chiseled_sandstone', 'cut_sandstone', 'smooth_sandstone', 'red_sandstone', 'chiseled_red_sandstone', 'cut_red_sandstone', 'smooth_red_sandstone']
    for i, ss in enumerate(sandstone_order):
        if base == ss:
            return (50, i, 0, block)

    # === 銅系 (60) ===
    copper_order = ['copper', 'copper_block', 'cut_copper', 'exposed_copper', 'exposed_cut_copper', 'weathered_copper', 'weathered_cut_copper', 'oxidized_copper', 'oxidized_cut_copper']
    waxed_copper_order = ['waxed_copper', 'waxed_copper_block', 'waxed_cut_copper', 'waxed_exposed_copper', 'waxed_exposed_cut_copper', 'waxed_weathered_copper', 'waxed_weathered_cut_copper', 'waxed_oxidized_copper', 'waxed_oxidized_cut_copper']

    for i, cu in enumerate(copper_order):
        if base == cu or base.startswith(cu):
            return (60, i, 0, block)
    for i, cu in enumerate(waxed_copper_order):
        if base == cu or base.startswith(cu):
            return (60, len(copper_order) + i, 0, block)

    # 縦スラブ用の銅系（vertical_*_slab）
    if 'copper' in original_base:
        # vertical_cut_copper_slab など
        for i, cu in enumerate(['copper_block', 'cut_copper', 'exposed_cut_copper', 'weathered_cut_copper', 'oxidized_cut_copper']):
            if cu in original_base:
                if 'waxed' in original_base:
                    return (60, 20 + i, 0, block)
                return (60, 10 + i, 0, block)

    # === プリズマリン・ダークプリズマリン (70) ===
    if base in ['prismarine', 'dark_prismarine']:
        return (70, 0 if base == 'prismarine' else 1, 0, block)

    # === クォーツ系 (71) ===
    quartz_order = ['quartz', 'quartz_block', 'smooth_quartz', 'chiseled_quartz_block', 'quartz_pillar', 'quartz_bricks']
    for i, q in enumerate(quartz_order):
        if base == q:
            return (71, i, 0, block)

    # === パープル系 (72) ===
    if base in ['purpur', 'purpur_block', 'purpur_pillar']:
        return (72, 0, 0, block)

    # === 鉄・石ボタン・感圧板 (80) ===
    if base == 'iron':
        return (80, 0, 0, block)
    if base == 'stone':
        return (80, 1, 0, block)

    # === その他 (99) ===
    return (99, 0, 0, block)

def sort_blocks_by_material(blocks):
    """ブロックリストを形状カテゴリ→素材タイプでソート"""
    def sort_key(block):
        shape_order, shape_name = get_shape_category(block)
        material_order = get_material_order(block)
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
    print("\n【ソート後の階段】")
    stairs = [b for b in sorted_blocks if b.endswith('_stairs')]
    for i, b in enumerate(stairs):
        print(f"  {i+1:2}. {b}")

    # 新しい配列文字列を生成
    new_array = "const BUILDING_BLOCKS_DATA = ['" + "', '".join(sorted_blocks) + "'];"

    # 置換
    old_array = building_match.group(0)
    content = content.replace(old_array, new_array)
    print("\nBUILDING_BLOCKS_DATA を更新しました")
else:
    print("BUILDING_BLOCKS_DATA が見つかりませんでした")

# DIRECTIONAL_BLOCKS_DATA も同様にソート
directional_match = re.search(r"const DIRECTIONAL_BLOCKS_DATA = \[([^\]]+)\];", content)
if directional_match:
    array_content = directional_match.group(1)
    blocks = re.findall(r"'([^']+)'", array_content)
    print(f"\nDIRECTIONAL_BLOCKS_DATA: {len(blocks)} ブロック")

    sorted_blocks = sort_blocks_by_material(blocks)

    # 結果の一部を表示
    print("\n【ソート後の階段（DIRECTIONAL）】")
    stairs = [b for b in sorted_blocks if b.endswith('_stairs')]
    for i, b in enumerate(stairs):
        print(f"  {i+1:2}. {b}")

    new_array = "const DIRECTIONAL_BLOCKS_DATA = ['" + "', '".join(sorted_blocks) + "'];"
    old_array = directional_match.group(0)
    content = content.replace(old_array, new_array)
    print("\nDIRECTIONAL_BLOCKS_DATA を更新しました")

# 保存
with open('gui.js', 'w', encoding='utf-8') as f:
    f.write(content)

print("\nDone!")
