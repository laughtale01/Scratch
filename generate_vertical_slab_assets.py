#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
垂直スラブの不足しているアセットファイルを自動生成するスクリプト

このスクリプトは、ModBlocks.javaに登録されている34個の垂直スラブに対して、
必要なblockstate、model、言語ファイルを自動生成します。

生成されるファイル:
- blockstate files: minecraft-mod/src/main/resources/assets/minecraftedu/blockstates/*.json
- block model files: minecraft-mod/src/main/resources/assets/minecraftedu/models/block/*.json
- item model files: minecraft-mod/src/main/resources/assets/minecraftedu/models/item/*.json
- language file: minecraft-mod/src/main/resources/assets/minecraftedu/lang/ja_jp.json
"""

import json
import os
from pathlib import Path

# 垂直スラブの定義（ブロック名、テクスチャパス、日本語名）
VERTICAL_SLABS = [
    # 木材系
    ("vertical_oak_slab", "minecraft:block/oak_planks", "オークのスラブ（垂直）"),
    ("vertical_birch_slab", "minecraft:block/birch_planks", "シラカバのスラブ（垂直）"),
    ("vertical_spruce_slab", "minecraft:block/spruce_planks", "マツのスラブ（垂直）"),
    ("vertical_jungle_slab", "minecraft:block/jungle_planks", "ジャングルのスラブ（垂直）"),
    ("vertical_acacia_slab", "minecraft:block/acacia_planks", "アカシアのスラブ（垂直）"),
    ("vertical_dark_oak_slab", "minecraft:block/dark_oak_planks", "ダークオークのスラブ（垂直）"),
    ("vertical_cherry_slab", "minecraft:block/cherry_planks", "サクラのスラブ（垂直）"),
    ("vertical_mangrove_slab", "minecraft:block/mangrove_planks", "マングローブのスラブ（垂直）"),
    ("vertical_crimson_slab", "minecraft:block/crimson_planks", "真紅のスラブ（垂直）"),
    ("vertical_warped_slab", "minecraft:block/warped_planks", "歪んだスラブ（垂直）"),

    # 石材系
    ("vertical_stone_slab", "minecraft:block/stone", "石のスラブ（垂直）"),
    ("vertical_cobblestone_slab", "minecraft:block/cobblestone", "丸石のスラブ（垂直）"),
    ("vertical_stone_brick_slab", "minecraft:block/stone_bricks", "石レンガのスラブ（垂直）"),
    ("vertical_smooth_stone_slab", "minecraft:block/smooth_stone", "滑らかな石のスラブ（垂直）"),
    ("vertical_andesite_slab", "minecraft:block/andesite", "安山岩のスラブ（垂直）"),
    ("vertical_granite_slab", "minecraft:block/granite", "花崗岩のスラブ（垂直）"),
    ("vertical_diorite_slab", "minecraft:block/diorite", "閃緑岩のスラブ（垂直）"),
    ("vertical_sandstone_slab", "minecraft:block/sandstone_top", "砂岩のスラブ（垂直）"),
    ("vertical_brick_slab", "minecraft:block/bricks", "レンガのスラブ（垂直）"),
    ("vertical_quartz_slab", "minecraft:block/quartz_block_side", "クォーツのスラブ（垂直）"),

    # 鉱石・鉱物系
    ("vertical_iron_block_slab", "minecraft:block/iron_block", "鉄ブロックのスラブ（垂直）"),
    ("vertical_gold_block_slab", "minecraft:block/gold_block", "金ブロックのスラブ（垂直）"),
    ("vertical_diamond_block_slab", "minecraft:block/diamond_block", "ダイヤブロックのスラブ（垂直）"),
    ("vertical_emerald_block_slab", "minecraft:block/emerald_block", "エメラルドブロックのスラブ（垂直）"),
    ("vertical_copper_block_slab", "minecraft:block/copper_block", "銅ブロックのスラブ（垂直）"),
    ("vertical_lapis_block_slab", "minecraft:block/lapis_block", "ラピスラズリブロックのスラブ（垂直）"),
    ("vertical_redstone_block_slab", "minecraft:block/redstone_block", "レッドストーンブロックのスラブ（垂直）"),
    ("vertical_coal_block_slab", "minecraft:block/coal_block", "石炭ブロックのスラブ（垂直）"),
    ("vertical_netherite_block_slab", "minecraft:block/netherite_block", "ネザライトブロックのスラブ（垂直）"),
    ("vertical_amethyst_block_slab", "minecraft:block/amethyst_block", "アメジストブロックのスラブ（垂直）"),

    # 銅系（日本建築の屋根用）
    ("vertical_cut_copper_slab", "minecraft:block/cut_copper", "切り込み入り銅のスラブ（垂直）"),
    ("vertical_exposed_cut_copper_slab", "minecraft:block/exposed_cut_copper", "風化した切り込み入り銅のスラブ（垂直）"),
    ("vertical_weathered_cut_copper_slab", "minecraft:block/weathered_cut_copper", "錆びた切り込み入り銅のスラブ（垂直）"),
    ("vertical_oxidized_cut_copper_slab", "minecraft:block/oxidized_cut_copper", "酸化した切り込み入り銅のスラブ（垂直）"),
]

# ベースディレクトリ
BASE_DIR = Path("minecraft-mod/src/main/resources/assets/minecraftedu")
BLOCKSTATES_DIR = BASE_DIR / "blockstates"
BLOCK_MODELS_DIR = BASE_DIR / "models" / "block"
ITEM_MODELS_DIR = BASE_DIR / "models" / "item"
LANG_DIR = BASE_DIR / "lang"

# ディレクトリ作成
BLOCKSTATES_DIR.mkdir(parents=True, exist_ok=True)
BLOCK_MODELS_DIR.mkdir(parents=True, exist_ok=True)
ITEM_MODELS_DIR.mkdir(parents=True, exist_ok=True)
LANG_DIR.mkdir(parents=True, exist_ok=True)

def create_blockstate_file(block_name):
    """blockstate JSONファイルを生成"""
    blockstate = {
        "variants": {
            "facing=north,waterlogged=false": {
                "model": f"minecraftedu:block/{block_name}"
            },
            "facing=north,waterlogged=true": {
                "model": f"minecraftedu:block/{block_name}"
            },
            "facing=south,waterlogged=false": {
                "model": f"minecraftedu:block/{block_name}",
                "y": 180
            },
            "facing=south,waterlogged=true": {
                "model": f"minecraftedu:block/{block_name}",
                "y": 180
            },
            "facing=east,waterlogged=false": {
                "model": f"minecraftedu:block/{block_name}",
                "y": 90
            },
            "facing=east,waterlogged=true": {
                "model": f"minecraftedu:block/{block_name}",
                "y": 90
            },
            "facing=west,waterlogged=false": {
                "model": f"minecraftedu:block/{block_name}",
                "y": 270
            },
            "facing=west,waterlogged=true": {
                "model": f"minecraftedu:block/{block_name}",
                "y": 270
            }
        }
    }

    filepath = BLOCKSTATES_DIR / f"{block_name}.json"
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(blockstate, f, indent=2, ensure_ascii=False)
    print(f"[OK] Created blockstate: {filepath}")

def create_block_model_file(block_name, texture_path):
    """block model JSONファイルを生成"""
    model = {
        "parent": "block/block",
        "textures": {
            "texture": texture_path,
            "particle": texture_path
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

    filepath = BLOCK_MODELS_DIR / f"{block_name}.json"
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(model, f, indent=2, ensure_ascii=False)
    print(f"[OK] Created block model: {filepath}")

def create_item_model_file(block_name):
    """item model JSONファイルを生成"""
    model = {
        "parent": f"minecraftedu:block/{block_name}"
    }

    filepath = ITEM_MODELS_DIR / f"{block_name}.json"
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(model, f, indent=2, ensure_ascii=False)
    print(f"[OK] Created item model: {filepath}")

def update_language_file():
    """日本語言語ファイルを更新"""
    lang_file = LANG_DIR / "ja_jp.json"

    # 既存のファイルを読み込む
    if lang_file.exists():
        with open(lang_file, 'r', encoding='utf-8') as f:
            lang_data = json.load(f)
    else:
        lang_data = {}

    # 全ての垂直スラブの翻訳を追加
    for block_name, _, japanese_name in VERTICAL_SLABS:
        key = f"block.minecraftedu.{block_name}"
        lang_data[key] = japanese_name

    # ソートして保存
    lang_data = dict(sorted(lang_data.items()))

    with open(lang_file, 'w', encoding='utf-8') as f:
        json.dump(lang_data, f, indent=2, ensure_ascii=False)
        f.write('\n')  # 最後に改行を追加

    print(f"[OK] Updated language file: {lang_file}")
    print(f"  Total entries: {len(lang_data)}")

# メイン処理
def main():
    print("=" * 60)
    print("垂直スラブアセット自動生成スクリプト")
    print("=" * 60)
    print()

    # 各垂直スラブのファイルを生成
    for block_name, texture_path, japanese_name in VERTICAL_SLABS:
        print(f"\n処理中: {block_name}")
        create_blockstate_file(block_name)
        create_block_model_file(block_name, texture_path)
        create_item_model_file(block_name)

    print("\n" + "=" * 60)
    print("言語ファイル更新")
    print("=" * 60)
    update_language_file()

    print("\n" + "=" * 60)
    print("[COMPLETE] All asset files have been generated!")
    print("=" * 60)
    print()
    print("次のステップ:")
    print("1. git add minecraft-mod/src/main/resources/")
    print("2. git commit -m 'feat: Add blockstate, model, and language files for 34 vertical slabs'")
    print("3. ./gradlew build でMODをビルド")
    print("4. JAR ファイルを.minecraft/modsにコピー")
    print()

if __name__ == "__main__":
    main()
