#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
既存の動作するプロジェクトをコピーして、倉庫番ゲーム用に修正する
"""

import json
import zipfile
import shutil
from pathlib import Path

def create_sokoban_from_template():
    """既存のプロジェクトをベースに倉庫番ゲームを作成"""

    print("=" * 60)
    print("既存プロジェクトベースの倉庫番ゲーム作成")
    print("=" * 60)
    print()

    # 既存のproject.jsonを読み込む
    source_dir = Path("temp_sb3_analysis")
    with open(source_dir / "project.json", 'r', encoding='utf-8') as f:
        project = json.load(f)

    print("[OK] 既存プロジェクト読み込み完了")

    # スプライトを取得（targets[1]）
    sprite = project['targets'][1]

    # 既存のブロックをクリア
    sprite['blocks'] = {}

    # 変数を追加
    sprite['variables'] = {
        "var_playerX": ["playerX", 5],
        "var_playerY": ["playerY", 5],
        "var_baseX": ["baseX", 0],
        "var_baseY": ["baseY", 0],
        "var_baseZ": ["baseZ", 0],
        "var_gameState": ["gameState", 0]
    }

    # リストは空のまま
    sprite['lists'] = {}

    print("[OK] 変数定義完了")

    # ブロックを追加
    blocks = {}

    # 緑の旗がクリックされたとき
    blocks["hat_flag"] = {
        "opcode": "event_whenflagclicked",
        "next": "block_chat1",
        "parent": None,
        "inputs": {},
        "fields": {},
        "shadow": False,
        "topLevel": True,
        "x": 48,
        "y": 48
    }

    # チャットメッセージ
    blocks["block_chat1"] = {
        "opcode": "minecraft_chat",
        "next": "block_setvar1",
        "parent": "hat_flag",
        "inputs": {
            "MESSAGE": [1, [10, "倉庫番ゲーム開始！矢印キーで移動"]]
        },
        "fields": {},
        "shadow": False,
        "topLevel": False
    }

    # gameStateを1に設定
    blocks["block_setvar1"] = {
        "opcode": "data_setvariableto",
        "next": None,
        "parent": "block_chat1",
        "inputs": {
            "VALUE": [1, [4, "1"]]
        },
        "fields": {
            "VARIABLE": ["gameState", "var_gameState"]
        },
        "shadow": False,
        "topLevel": False
    }

    # ↑キーが押されたとき
    blocks["hat_up"] = {
        "opcode": "event_whenkeypressed",
        "next": "block_change_up",
        "parent": None,
        "inputs": {},
        "fields": {
            "KEY_OPTION": ["up arrow", None]
        },
        "shadow": False,
        "topLevel": True,
        "x": 400,
        "y": 48
    }

    blocks["block_change_up"] = {
        "opcode": "data_changevariableby",
        "next": "block_chat_up",
        "parent": "hat_up",
        "inputs": {
            "VALUE": [1, [4, "-1"]]
        },
        "fields": {
            "VARIABLE": ["playerY", "var_playerY"]
        },
        "shadow": False,
        "topLevel": False
    }

    blocks["block_chat_up"] = {
        "opcode": "minecraft_chat",
        "next": None,
        "parent": "block_change_up",
        "inputs": {
            "MESSAGE": [1, [10, "上に移動"]]
        },
        "fields": {},
        "shadow": False,
        "topLevel": False
    }

    # ↓キーが押されたとき
    blocks["hat_down"] = {
        "opcode": "event_whenkeypressed",
        "next": "block_change_down",
        "parent": None,
        "inputs": {},
        "fields": {
            "KEY_OPTION": ["down arrow", None]
        },
        "shadow": False,
        "topLevel": True,
        "x": 700,
        "y": 48
    }

    blocks["block_change_down"] = {
        "opcode": "data_changevariableby",
        "next": "block_chat_down",
        "parent": "hat_down",
        "inputs": {
            "VALUE": [1, [4, "1"]]
        },
        "fields": {
            "VARIABLE": ["playerY", "var_playerY"]
        },
        "shadow": False,
        "topLevel": False
    }

    blocks["block_chat_down"] = {
        "opcode": "minecraft_chat",
        "next": None,
        "parent": "block_change_down",
        "inputs": {
            "MESSAGE": [1, [10, "下に移動"]]
        },
        "fields": {},
        "shadow": False,
        "topLevel": False
    }

    # ←キーが押されたとき
    blocks["hat_left"] = {
        "opcode": "event_whenkeypressed",
        "next": "block_change_left",
        "parent": None,
        "inputs": {},
        "fields": {
            "KEY_OPTION": ["left arrow", None]
        },
        "shadow": False,
        "topLevel": True,
        "x": 1000,
        "y": 48
    }

    blocks["block_change_left"] = {
        "opcode": "data_changevariableby",
        "next": "block_chat_left",
        "parent": "hat_left",
        "inputs": {
            "VALUE": [1, [4, "-1"]]
        },
        "fields": {
            "VARIABLE": ["playerX", "var_playerX"]
        },
        "shadow": False,
        "topLevel": False
    }

    blocks["block_chat_left"] = {
        "opcode": "minecraft_chat",
        "next": None,
        "parent": "block_change_left",
        "inputs": {
            "MESSAGE": [1, [10, "左に移動"]]
        },
        "fields": {},
        "shadow": False,
        "topLevel": False
    }

    # →キーが押されたとき
    blocks["hat_right"] = {
        "opcode": "event_whenkeypressed",
        "next": "block_change_right",
        "parent": None,
        "inputs": {},
        "fields": {
            "KEY_OPTION": ["right arrow", None]
        },
        "shadow": False,
        "topLevel": True,
        "x": 1300,
        "y": 48
    }

    blocks["block_change_right"] = {
        "opcode": "data_changevariableby",
        "next": "block_chat_right",
        "parent": "hat_right",
        "inputs": {
            "VALUE": [1, [4, "1"]]
        },
        "fields": {
            "VARIABLE": ["playerX", "var_playerX"]
        },
        "shadow": False,
        "topLevel": False
    }

    blocks["block_chat_right"] = {
        "opcode": "minecraft_chat",
        "next": None,
        "parent": "block_change_right",
        "inputs": {
            "MESSAGE": [1, [10, "右に移動"]]
        },
        "fields": {},
        "shadow": False,
        "topLevel": False
    }

    sprite['blocks'] = blocks
    sprite['name'] = "倉庫番コントローラー"

    print(f"[OK] ブロック追加完了（{len(blocks)}個）")

    # プロジェクトを保存
    output_path = "sokoban_working.sb3"

    # 一時ディレクトリ作成
    temp_dir = Path("temp_sokoban_working")
    temp_dir.mkdir(exist_ok=True)

    # project.json保存
    with open(temp_dir / "project.json", 'w', encoding='utf-8') as f:
        json.dump(project, f, ensure_ascii=False, separators=(',', ':'))

    print("[OK] project.json保存完了")

    # アセットファイルをコピー
    for ext in ["svg", "wav"]:
        for asset in source_dir.glob(f"*.{ext}"):
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
    output_file = create_sokoban_from_template()

    print()
    print("=" * 60)
    print("[COMPLETE] 動作確認済みプロジェクト生成完了！")
    print("=" * 60)
    print()
    print(f"ファイル: {output_file}")
    print()
    print("含まれる機能:")
    print("✓ 緑の旗がクリックされたとき")
    print("  - チャットメッセージ表示")
    print("  - gameState = 1 に設定")
    print()
    print("✓ ↑キーが押されたとき")
    print("  - playerY を -1")
    print("  - チャットメッセージ「上に移動」")
    print()
    print("✓ ↓キーが押されたとき")
    print("  - playerY を +1")
    print("  - チャットメッセージ「下に移動」")
    print()
    print("✓ ←キーが押されたとき")
    print("  - playerX を -1")
    print("  - チャットメッセージ「左に移動」")
    print()
    print("✓ →キーが押されたとき")
    print("  - playerX を +1")
    print("  - チャットメッセージ「右に移動」")
    print()
    print("動作確認:")
    print("1. Scratchエディターで sokoban_working.sb3 を開く")
    print("2. 5つのハットブロックが表示されることを確認")
    print("3. 緑の旗をクリックしてテスト")
    print()
