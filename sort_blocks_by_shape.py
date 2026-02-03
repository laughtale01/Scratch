# -*- coding: utf-8 -*-
"""
ブロックリストを形の種類（階段、スラブ、ドアなど）ごとにソートするスクリプト
"""

# gui.jsを読み込み
with open('gui.js', 'r', encoding='utf-8') as f:
    content = f.read()

def get_shape_category(block):
    """ブロック名から形状カテゴリを判定"""
    # 縦スラブ（vertical_ または waxed_vertical_ で始まる）
    if (block.startswith('vertical_') or block.startswith('waxed_vertical_')) and '_slab' in block:
        return 4  # 縦スラブ
    # 階段
    if block.endswith('_stairs'):
        return 1
    # 横スラブ
    if block.endswith('_slab'):
        return 2
    # 壁
    if block.endswith('_wall'):
        return 5
    # フェンスゲート（フェンスより先に判定）
    if block.endswith('_fence_gate'):
        return 7
    # フェンス
    if block.endswith('_fence'):
        return 6
    # ボタン
    if block.endswith('_button'):
        return 8
    # 感圧板
    if block.endswith('_pressure_plate'):
        return 9
    # トラップドア（ドアより先に判定）
    if block.endswith('_trapdoor'):
        return 11
    # ドア
    if block.endswith('_door'):
        return 10
    # カーペット
    if block.endswith('_carpet'):
        return 12
    # コンクリートパウダー
    if block.endswith('_concrete_powder'):
        return 13
    # 彩釉テラコッタ
    if block.endswith('_glazed_terracotta'):
        return 14
    # シュルカーボックス
    if block.endswith('_shulker_box'):
        return 15
    # 色付きガラス板
    if block.endswith('_stained_glass_pane'):
        return 16
    # ベッド
    if block.endswith('_bed'):
        return 17
    # 通常ブロック
    return 0

def sort_blocks_by_shape(blocks):
    """ブロックリストを形状カテゴリでソート"""
    return sorted(blocks, key=lambda b: (get_shape_category(b), b))

# BUILDING_BLOCKS_DATA を抽出
import re

# BUILDING_BLOCKS_DATA を抽出してソート
building_match = re.search(r"const BUILDING_BLOCKS_DATA = \[([^\]]+)\];", content)
if building_match:
    # 配列の中身を抽出
    array_content = building_match.group(1)
    # 各要素を抽出（'xxx' 形式）
    blocks = re.findall(r"'([^']+)'", array_content)
    print(f"BUILDING_BLOCKS_DATA: {len(blocks)} ブロック")

    # 形状でソート
    sorted_blocks = sort_blocks_by_shape(blocks)

    # カテゴリ別にカウント表示
    categories = {
        0: '通常ブロック',
        1: '階段',
        2: '横スラブ',
        4: '縦スラブ',
        5: '壁',
        6: 'フェンス',
        7: 'フェンスゲート',
        8: 'ボタン',
        9: '感圧板',
        10: 'ドア',
        11: 'トラップドア',
        12: 'カーペット',
        13: 'コンクリートパウダー',
        14: '彩釉テラコッタ',
        15: 'シュルカーボックス',
        16: '色付きガラス板',
        17: 'ベッド'
    }

    counts = {}
    for b in sorted_blocks:
        cat = get_shape_category(b)
        counts[cat] = counts.get(cat, 0) + 1

    print("\nBUILDING_BLOCKS_DATA カテゴリ別:")
    for cat_id in sorted(counts.keys()):
        print(f"  {categories.get(cat_id, '不明')}: {counts[cat_id]}個")

    # 新しい配列文字列を生成
    new_array = "const BUILDING_BLOCKS_DATA = ['" + "', '".join(sorted_blocks) + "'];"

    # 置換
    old_array = building_match.group(0)
    content = content.replace(old_array, new_array)
    print("\nBUILDING_BLOCKS_DATA を更新しました")
else:
    print("BUILDING_BLOCKS_DATA が見つかりませんでした")

# DIRECTIONAL_BLOCKS_DATA を抽出してソート
directional_match = re.search(r"const DIRECTIONAL_BLOCKS_DATA = \[([^\]]+)\];", content)
if directional_match:
    array_content = directional_match.group(1)
    blocks = re.findall(r"'([^']+)'", array_content)
    print(f"\nDIRECTIONAL_BLOCKS_DATA: {len(blocks)} ブロック")

    # 形状でソート
    sorted_blocks = sort_blocks_by_shape(blocks)

    # カテゴリ別カウント
    counts = {}
    for b in sorted_blocks:
        cat = get_shape_category(b)
        counts[cat] = counts.get(cat, 0) + 1

    print("\nDIRECTIONAL_BLOCKS_DATA カテゴリ別:")
    for cat_id in sorted(counts.keys()):
        print(f"  {categories.get(cat_id, '不明')}: {counts[cat_id]}個")

    # 新しい配列文字列を生成
    new_array = "const DIRECTIONAL_BLOCKS_DATA = ['" + "', '".join(sorted_blocks) + "'];"

    # 置換
    old_array = directional_match.group(0)
    content = content.replace(old_array, new_array)
    print("\nDIRECTIONAL_BLOCKS_DATA を更新しました")
else:
    print("DIRECTIONAL_BLOCKS_DATA が見つかりませんでした")

# 保存
with open('gui.js', 'w', encoding='utf-8') as f:
    f.write(content)

print("\nDone!")
