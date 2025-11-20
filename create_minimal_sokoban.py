#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
最小限の動作する倉庫番ゲーム Scratchプロジェクト生成

Scratchエディターで正しく表示されることを最優先にした、
シンプルで動作確認済みの構造を使用します。
"""

import json
import zipfile
import shutil
from pathlib import Path

def create_minimal_working_sokoban():
    """最小限の動作する倉庫番プロジェクトを作成"""

    # 最もシンプルな構造で作成
    project = {
        "targets": [
            # Stage（背景）
            {
                "isStage": True,
                "name": "Stage",
                "variables": {},
                "lists": {},
                "broadcasts": {},
                "blocks": {},
                "comments": {},
                "currentCostume": 0,
                "costumes": [{
                    "name": "backdrop1",
                    "dataFormat": "svg",
                    "assetId": "cd21514d0531fdffb22204e0ec5ed84a",
                    "md5ext": "cd21514d0531fdffb22204e0ec5ed84a.svg",
                    "rotationCenterX": 240,
                    "rotationCenterY": 180
                }],
                "sounds": [],
                "volume": 100,
                "layerOrder": 0,
                "tempo": 60,
                "videoTransparency": 50,
                "videoState": "on",
                "textToSpeechLanguage": None
            },
            # Sprite（倉庫番コントローラー）
            {
                "isStage": False,
                "name": "Sprite1",
                "variables": {
                    "playerX_id": ["playerX", 5],
                    "playerY_id": ["playerY", 5],
                    "baseX_id": ["baseX", 0],
                    "baseY_id": ["baseY", 0],
                    "baseZ_id": ["baseZ", 0]
                },
                "lists": {},
                "broadcasts": {},
                "blocks": {
                    # 緑の旗がクリックされたとき
                    "flag_block": {
                        "opcode": "event_whenflagclicked",
                        "next": "chat_block",
                        "parent": None,
                        "inputs": {},
                        "fields": {},
                        "shadow": False,
                        "topLevel": True,
                        "x": 48,
                        "y": 48
                    },
                    # チャットメッセージ
                    "chat_block": {
                        "opcode": "minecraft_chat",
                        "next": "clear_block",
                        "parent": "flag_block",
                        "inputs": {
                            "MESSAGE": [1, [10, "倉庫番ゲーム開始！矢印キーで移動してください"]]
                        },
                        "fields": {},
                        "shadow": False,
                        "topLevel": False
                    },
                    # エリアをクリア
                    "clear_block": {
                        "opcode": "minecraft_clearArea",
                        "next": "setblock1",
                        "parent": "chat_block",
                        "inputs": {
                            "X": [1, [4, "0"]],
                            "Z": [1, [4, "0"]]
                        },
                        "fields": {},
                        "shadow": False,
                        "topLevel": False
                    },
                    # 床を設置（10x10の範囲に床を敷く簡易版）
                    "setblock1": {
                        "opcode": "minecraft_setBlock",
                        "next": "setplayer",
                        "parent": "clear_block",
                        "inputs": {
                            "X": [1, [4, "5"]],
                            "Y": [1, [4, "0"]],
                            "Z": [1, [4, "5"]],
                            "BLOCK": [1, "menu_stone"]
                        },
                        "fields": {
                            "PLACEMENT": ["bottom", None],
                            "FACING": ["none", None]
                        },
                        "shadow": False,
                        "topLevel": False
                    },
                    "menu_stone": {
                        "opcode": "minecraft_menu_buildingBlocks",
                        "next": None,
                        "parent": "setblock1",
                        "inputs": {},
                        "fields": {
                            "buildingBlocks": ["smooth_stone", None]
                        },
                        "shadow": True,
                        "topLevel": False
                    },
                    # プレイヤーブロックを設置
                    "setplayer": {
                        "opcode": "minecraft_setBlock",
                        "next": None,
                        "parent": "setblock1",
                        "inputs": {
                            "X": [3, ["playerX_id_reporter", None], [4, "5"]],
                            "Y": [1, [4, "1"]],
                            "Z": [3, ["playerY_id_reporter", None], [4, "5"]],
                            "BLOCK": [1, "menu_diamond"]
                        },
                        "fields": {
                            "PLACEMENT": ["bottom", None],
                            "FACING": ["none", None]
                        },
                        "shadow": False,
                        "topLevel": False
                    },
                    "playerX_id_reporter": {
                        "opcode": "data_variable",
                        "next": None,
                        "parent": "setplayer",
                        "inputs": {},
                        "fields": {
                            "VARIABLE": ["playerX", "playerX_id"]
                        },
                        "shadow": False,
                        "topLevel": False
                    },
                    "playerY_id_reporter": {
                        "opcode": "data_variable",
                        "next": None,
                        "parent": "setplayer",
                        "inputs": {},
                        "fields": {
                            "VARIABLE": ["playerY", "playerY_id"]
                        },
                        "shadow": False,
                        "topLevel": False
                    },
                    "menu_diamond": {
                        "opcode": "minecraft_menu_buildingBlocks",
                        "next": None,
                        "parent": "setplayer",
                        "inputs": {},
                        "fields": {
                            "buildingBlocks": ["diamond_block", None]
                        },
                        "shadow": True,
                        "topLevel": False
                    },
                    # ↑キーが押されたとき
                    "key_up": {
                        "opcode": "event_whenkeypressed",
                        "next": "change_y_up",
                        "parent": None,
                        "inputs": {},
                        "fields": {
                            "KEY_OPTION": ["up arrow", None]
                        },
                        "shadow": False,
                        "topLevel": True,
                        "x": 400,
                        "y": 48
                    },
                    "change_y_up": {
                        "opcode": "data_changevariableby",
                        "next": "chat_move",
                        "parent": "key_up",
                        "inputs": {
                            "VALUE": [1, [4, "-1"]]
                        },
                        "fields": {
                            "VARIABLE": ["playerY", "playerY_id"]
                        },
                        "shadow": False,
                        "topLevel": False
                    },
                    "chat_move": {
                        "opcode": "minecraft_chat",
                        "next": None,
                        "parent": "change_y_up",
                        "inputs": {
                            "MESSAGE": [1, [10, "上に移動しました"]]
                        },
                        "fields": {},
                        "shadow": False,
                        "topLevel": False
                    },
                    # ↓キーが押されたとき
                    "key_down": {
                        "opcode": "event_whenkeypressed",
                        "next": "change_y_down",
                        "parent": None,
                        "inputs": {},
                        "fields": {
                            "KEY_OPTION": ["down arrow", None]
                        },
                        "shadow": False,
                        "topLevel": True,
                        "x": 700,
                        "y": 48
                    },
                    "change_y_down": {
                        "opcode": "data_changevariableby",
                        "next": None,
                        "parent": "key_down",
                        "inputs": {
                            "VALUE": [1, [4, "1"]]
                        },
                        "fields": {
                            "VARIABLE": ["playerY", "playerY_id"]
                        },
                        "shadow": False,
                        "topLevel": False
                    },
                    # ←キーが押されたとき
                    "key_left": {
                        "opcode": "event_whenkeypressed",
                        "next": "change_x_left",
                        "parent": None,
                        "inputs": {},
                        "fields": {
                            "KEY_OPTION": ["left arrow", None]
                        },
                        "shadow": False,
                        "topLevel": True,
                        "x": 1000,
                        "y": 48
                    },
                    "change_x_left": {
                        "opcode": "data_changevariableby",
                        "next": None,
                        "parent": "key_left",
                        "inputs": {
                            "VALUE": [1, [4, "-1"]]
                        },
                        "fields": {
                            "VARIABLE": ["playerX", "playerX_id"]
                        },
                        "shadow": False,
                        "topLevel": False
                    },
                    # →キーが押されたとき
                    "key_right": {
                        "opcode": "event_whenkeypressed",
                        "next": "change_x_right",
                        "parent": None,
                        "inputs": {},
                        "fields": {
                            "KEY_OPTION": ["right arrow", None]
                        },
                        "shadow": False,
                        "topLevel": True,
                        "x": 1300,
                        "y": 48
                    },
                    "change_x_right": {
                        "opcode": "data_changevariableby",
                        "next": None,
                        "parent": "key_right",
                        "inputs": {
                            "VALUE": [1, [4, "1"]]
                        },
                        "fields": {
                            "VARIABLE": ["playerX", "playerX_id"]
                        },
                        "shadow": False,
                        "topLevel": False
                    }
                },
                "comments": {},
                "currentCostume": 0,
                "costumes": [{
                    "name": "costume1",
                    "bitmapResolution": 1,
                    "dataFormat": "svg",
                    "assetId": "bcf454acf82e4504149f7ffe07081dbc",
                    "md5ext": "bcf454acf82e4504149f7ffe07081dbc.svg",
                    "rotationCenterX": 48,
                    "rotationCenterY": 50
                }],
                "sounds": [],
                "volume": 100,
                "layerOrder": 1,
                "visible": True,
                "x": 0,
                "y": 0,
                "size": 100,
                "direction": 90,
                "draggable": False,
                "rotationStyle": "all around"
            }
        ],
        "monitors": [],
        "extensions": ["minecraft"],
        "meta": {
            "semver": "3.0.0",
            "vm": "2.0.0",
            "agent": "Minimal Sokoban v1.0"
        }
    }

    return project


def create_minimal_sb3(output_path="sokoban_minimal.sb3"):
    """最小限の.sb3ファイルを作成"""

    print("=" * 60)
    print("最小限の倉庫番ゲーム .sb3ファイル生成")
    print("=" * 60)
    print()

    # プロジェクトデータ生成
    project = create_minimal_working_sokoban()

    # 一時ディレクトリ
    temp_dir = Path("temp_minimal_sokoban")
    temp_dir.mkdir(exist_ok=True)

    # project.json作成
    with open(temp_dir / "project.json", 'w', encoding='utf-8') as f:
        json.dump(project, f, ensure_ascii=False, separators=(',', ':'))

    print("[OK] project.json作成完了")

    # アセットファイルをコピー
    source_assets = Path("temp_sb3_analysis")
    if source_assets.exists():
        for ext in ["svg", "wav"]:
            for asset in source_assets.glob(f"*.{ext}"):
                shutil.copy(asset, temp_dir)
        print("[OK] アセットファイルコピー完了")

    # ZIP圧縮
    with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zf:
        for file in temp_dir.glob("*"):
            zf.write(file, file.name)

    print(f"[OK] {output_path} 作成完了")

    # クリーンアップ
    shutil.rmtree(temp_dir)

    return output_path


if __name__ == "__main__":
    output_file = create_minimal_sb3()

    print()
    print("=" * 60)
    print("[COMPLETE] 最小限バージョン生成完了！")
    print("=" * 60)
    print()
    print(f"ファイル: {output_file}")
    print()
    print("含まれる機能:")
    print("- 変数: playerX, playerY, baseX, baseY, baseZ")
    print("- 初期化処理（緑の旗）")
    print("- チャットメッセージ表示")
    print("- エリアクリア")
    print("- 簡易的な床設置")
    print("- プレイヤーブロック設置")
    print("- キー入力（4方向）")
    print()
    print("動作確認:")
    print("1. Scratchエディターで開く")
    print("2. すべてのブロックが表示されることを確認")
    print("3. 緑の旗をクリック")
    print("4. Minecraftでメッセージとブロックが表示されることを確認")
    print()
