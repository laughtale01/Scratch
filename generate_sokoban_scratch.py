#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Minecraft連携倉庫番ゲーム Scratchプロジェクト生成スクリプト

このスクリプトは、倉庫番（Sokoban）ゲームのScratchプロジェクト（.sb3ファイル）を
自動生成します。
"""

import json
import random
import string
import zipfile
import shutil
from pathlib import Path

# ランダムなブロックIDを生成
def generate_block_id(length=20):
    """Scratchブロック用のランダムなIDを生成"""
    chars = string.ascii_letters + string.digits + '!@#$%^&*()_+-=[]{}|;:,.<>?'
    return ''.join(random.choice(chars) for _ in range(length))

# 変数IDを生成（Scratchの変数ID形式）
def generate_variable_id():
    """Scratch変数用のIDを生成"""
    chars = string.ascii_letters + string.digits + '`!@#$%^&*()_+-=[]{}|;:,.<>?'
    return ''.join(random.choice(chars) for _ in range(20))

class ScratchBlockBuilder:
    """Scratchブロックを構築するクラス"""

    def __init__(self):
        self.blocks = {}
        self.variables = {}
        self.lists = {}

    def add_variable(self, name, initial_value=0):
        """変数を追加"""
        var_id = generate_variable_id()
        self.variables[var_id] = [name, initial_value]
        return var_id

    def add_list(self, name):
        """リストを追加"""
        list_id = generate_variable_id()
        self.lists[list_id] = [name, []]
        return list_id

    def create_hat_block(self, opcode, fields=None, x=0, y=0):
        """ハットブロック（緑の旗など）を作成"""
        block_id = generate_block_id()
        self.blocks[block_id] = {
            "opcode": opcode,
            "next": None,
            "parent": None,
            "inputs": {},
            "fields": fields or {},
            "shadow": False,
            "topLevel": True,
            "x": x,
            "y": y
        }
        return block_id

    def create_block(self, opcode, parent=None, inputs=None, fields=None):
        """通常のブロックを作成"""
        block_id = generate_block_id()
        self.blocks[block_id] = {
            "opcode": opcode,
            "next": None,
            "parent": parent,
            "inputs": inputs or {},
            "fields": fields or {},
            "shadow": False,
            "topLevel": False if parent else True
        }

        # 親ブロックのnextを更新
        if parent and parent in self.blocks:
            self.blocks[parent]["next"] = block_id

        return block_id

    def create_reporter_block(self, opcode, parent=None, inputs=None, fields=None):
        """レポーターブロック（値を返すブロック）を作成"""
        block_id = generate_block_id()
        self.blocks[block_id] = {
            "opcode": opcode,
            "next": None,
            "parent": parent,
            "inputs": inputs or {},
            "fields": fields or {},
            "shadow": False,
            "topLevel": False
        }
        return block_id

    def create_number_input(self, value):
        """数値入力を作成"""
        return [1, [4, str(value)]]

    def create_string_input(self, value):
        """文字列入力を作成"""
        return [1, [10, value]]

    def create_variable_input(self, var_id, var_name):
        """変数入力を作成"""
        return [3, [12, var_name, var_id], [4, "0"]]

    def create_block_input(self, block_id):
        """ブロック入力を作成"""
        return [2, block_id]

# ステージデータ（10x10の倉庫番マップ）
STAGE_DATA = [
    0,0,0,0,0,0,0,0,0,0,
    0,1,1,1,0,1,1,1,1,0,
    0,1,3,1,0,1,2,1,1,0,
    0,1,1,1,1,1,1,1,1,0,
    0,0,0,0,1,0,0,0,0,0,
    0,1,1,1,1,1,1,1,1,0,
    0,1,2,1,0,1,3,1,1,0,
    0,1,1,1,0,1,1,1,4,0,
    0,1,1,1,1,1,1,1,1,0,
    0,0,0,0,0,0,0,0,0,0
]

def build_sokoban_project():
    """倉庫番プロジェクトを構築"""

    builder = ScratchBlockBuilder()

    # ========================================
    # 変数定義
    # ========================================
    var_playerX = builder.add_variable("playerX", 0)
    var_playerY = builder.add_variable("playerY", 0)
    var_mapWidth = builder.add_variable("mapWidth", 10)
    var_mapHeight = builder.add_variable("mapHeight", 10)
    var_gameState = builder.add_variable("gameState", 0)
    var_moveCount = builder.add_variable("moveCount", 0)
    var_pushCount = builder.add_variable("pushCount", 0)
    var_tempX = builder.add_variable("tempX", 0)
    var_tempY = builder.add_variable("tempY", 0)
    var_tempIndex = builder.add_variable("tempIndex", 0)
    var_checkX = builder.add_variable("checkX", 0)
    var_checkY = builder.add_variable("checkY", 0)
    var_loopX = builder.add_variable("loopX", 0)
    var_loopY = builder.add_variable("loopY", 0)
    var_loopIndex = builder.add_variable("loopIndex", 0)
    var_baseX = builder.add_variable("baseX", 0)
    var_baseY = builder.add_variable("baseY", 0)
    var_baseZ = builder.add_variable("baseZ", 0)
    var_mapValue = builder.add_variable("mapValue", 0)
    var_boxIndex = builder.add_variable("boxIndex", 0)
    var_goalCount = builder.add_variable("goalCount", 0)
    var_clearedBoxes = builder.add_variable("clearedBoxes", 0)

    # ========================================
    # リスト定義
    # ========================================
    list_stageData = builder.add_list("stageData")
    list_boxesX = builder.add_list("boxesX")
    list_boxesY = builder.add_list("boxesY")
    list_goalsX = builder.add_list("goalsX")
    list_goalsY = builder.add_list("goalsY")

    print("[INFO] 変数とリストを定義しました")
    print(f"  変数数: {len(builder.variables)}")
    print(f"  リスト数: {len(builder.lists)}")

    # ========================================
    # ブロック構築開始
    # ========================================

    # これから各処理のブロックを作成します
    # 1. 初期化処理
    # 2. 描画処理
    # 3. キー入力処理
    # 4. 移動処理
    # 5. クリア判定

    print("[INFO] ブロック生成を開始します...")

    return builder, {
        'variables': {
            'playerX': var_playerX,
            'playerY': var_playerY,
            'mapWidth': var_mapWidth,
            'mapHeight': var_mapHeight,
            'gameState': var_gameState,
            'moveCount': var_moveCount,
            'pushCount': var_pushCount,
            'tempX': var_tempX,
            'tempY': var_tempY,
            'tempIndex': var_tempIndex,
            'checkX': var_checkX,
            'checkY': var_checkY,
            'loopX': var_loopX,
            'loopY': var_loopY,
            'loopIndex': var_loopIndex,
            'baseX': var_baseX,
            'baseY': var_baseY,
            'baseZ': var_baseZ,
            'mapValue': var_mapValue,
            'boxIndex': var_boxIndex,
            'goalCount': var_goalCount,
            'clearedBoxes': var_clearedBoxes,
        },
        'lists': {
            'stageData': list_stageData,
            'boxesX': list_boxesX,
            'boxesY': list_boxesY,
            'goalsX': list_goalsX,
            'goalsY': list_goalsY,
        }
    }

if __name__ == "__main__":
    print("=" * 60)
    print("倉庫番ゲーム Scratchプロジェクト生成スクリプト")
    print("=" * 60)
    print()

    builder, ids = build_sokoban_project()

    print()
    print(f"[OK] 変数定義完了: {len(builder.variables)}個")
    print(f"[OK] リスト定義完了: {len(builder.lists)}個")
    print()
    print("[INFO] 次のステップ: ブロック処理を追加")
