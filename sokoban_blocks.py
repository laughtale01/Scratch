#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
倉庫番ゲームのブロック生成処理
"""

from generate_sokoban_scratch import ScratchBlockBuilder

# ステージデータ
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

def build_init_script(builder, var_ids, list_ids):
    """
    初期化スクリプトを構築
    緑の旗がクリックされたときの処理
    """

    # 緑の旗ハットブロック
    hat_id = builder.create_hat_block("event_whenflagclicked", x=48, y=48)
    current_id = hat_id

    # マップサイズを設定
    block_id = builder.create_block(
        "data_setvariableto",
        parent=current_id,
        inputs={
            "VALUE": builder.create_number_input(10)
        },
        fields={
            "VARIABLE": ["mapWidth", var_ids['mapWidth']]
        }
    )
    current_id = block_id

    block_id = builder.create_block(
        "data_setvariableto",
        parent=current_id,
        inputs={
            "VALUE": builder.create_number_input(10)
        },
        fields={
            "VARIABLE": ["mapHeight", var_ids['mapHeight']]
        }
    )
    current_id = block_id

    # 基準座標を設定
    block_id = builder.create_block(
        "data_setvariableto",
        parent=current_id,
        inputs={
            "VALUE": builder.create_number_input(0)
        },
        fields={
            "VARIABLE": ["baseX", var_ids['baseX']]
        }
    )
    current_id = block_id

    block_id = builder.create_block(
        "data_setvariableto",
        parent=current_id,
        inputs={
            "VALUE": builder.create_number_input(0)
        },
        fields={
            "VARIABLE": ["baseY", var_ids['baseY']]
        }
    )
    current_id = block_id

    block_id = builder.create_block(
        "data_setvariableto",
        parent=current_id,
        inputs={
            "VALUE": builder.create_number_input(0)
        },
        fields={
            "VARIABLE": ["baseZ", var_ids['baseZ']]
        }
    )
    current_id = block_id

    # moveCountとpushCountを0に設定
    block_id = builder.create_block(
        "data_setvariableto",
        parent=current_id,
        inputs={
            "VALUE": builder.create_number_input(0)
        },
        fields={
            "VARIABLE": ["moveCount", var_ids['moveCount']]
        }
    )
    current_id = block_id

    block_id = builder.create_block(
        "data_setvariableto",
        parent=current_id,
        inputs={
            "VALUE": builder.create_number_input(0)
        },
        fields={
            "VARIABLE": ["pushCount", var_ids['pushCount']]
        }
    )
    current_id = block_id

    # リストをすべて削除
    block_id = builder.create_block(
        "data_deletealloflist",
        parent=current_id,
        fields={
            "LIST": ["stageData", list_ids['stageData']]
        }
    )
    current_id = block_id

    block_id = builder.create_block(
        "data_deletealloflist",
        parent=current_id,
        fields={
            "LIST": ["boxesX", list_ids['boxesX']]
        }
    )
    current_id = block_id

    block_id = builder.create_block(
        "data_deletealloflist",
        parent=current_id,
        fields={
            "LIST": ["boxesY", list_ids['boxesY']]
        }
    )
    current_id = block_id

    block_id = builder.create_block(
        "data_deletealloflist",
        parent=current_id,
        fields={
            "LIST": ["goalsX", list_ids['goalsX']]
        }
    )
    current_id = block_id

    block_id = builder.create_block(
        "data_deletealloflist",
        parent=current_id,
        fields={
            "LIST": ["goalsY", list_ids['goalsY']]
        }
    )
    current_id = block_id

    # ステージデータをリストに追加（100個のデータ）
    for value in STAGE_DATA:
        block_id = builder.create_block(
            "data_addtolist",
            parent=current_id,
            inputs={
                "ITEM": builder.create_number_input(value)
            },
            fields={
                "LIST": ["stageData", list_ids['stageData']]
            }
        )
        current_id = block_id

    # loopYを0に設定
    block_id = builder.create_block(
        "data_setvariableto",
        parent=current_id,
        inputs={
            "VALUE": builder.create_number_input(0)
        },
        fields={
            "VARIABLE": ["loopY", var_ids['loopY']]
        }
    )
    current_id = block_id

    # 外側のループ: Y座標（0から9まで）
    # repeat 10
    repeat_y_id = builder.create_block(
        "control_repeat",
        parent=current_id,
        inputs={
            "TIMES": builder.create_number_input(10)
        }
    )
    current_id = repeat_y_id

    # loopXを0に設定（ループ内）
    block_id = builder.create_block(
        "data_setvariableto",
        parent=None,  # サブスタック内なので親はなし
        inputs={
            "VALUE": builder.create_number_input(0)
        },
        fields={
            "VARIABLE": ["loopX", var_ids['loopX']]
        }
    )
    # このブロックをrepeat_y_idのSUBSTACK inputに設定
    builder.blocks[repeat_y_id]["inputs"]["SUBSTACK"] = [2, block_id]
    builder.blocks[block_id]["parent"] = repeat_y_id
    current_substack_id = block_id

    # 内側のループ: X座標（0から9まで）
    # repeat 10
    repeat_x_id = builder.create_block(
        "control_repeat",
        parent=current_substack_id,
        inputs={
            "TIMES": builder.create_number_input(10)
        }
    )
    current_substack_id = repeat_x_id

    # インデックスを計算: loopY * 10 + loopX
    # tempIndex = loopY * 10
    multiply_block_id = builder.create_reporter_block(
        "operator_multiply",
        inputs={
            "NUM1": builder.create_variable_input(var_ids['loopY'], "loopY"),
            "NUM2": builder.create_number_input(10)
        }
    )

    # tempIndex = (loopY * 10) + loopX
    add_block_id = builder.create_reporter_block(
        "operator_add",
        inputs={
            "NUM1": [2, multiply_block_id],
            "NUM2": builder.create_variable_input(var_ids['loopX'], "loopX")
        }
    )
    builder.blocks[multiply_block_id]["parent"] = add_block_id

    # tempIndex = ((loopY * 10) + loopX) + 1 （リストは1始まり）
    add1_block_id = builder.create_reporter_block(
        "operator_add",
        inputs={
            "NUM1": [2, add_block_id],
            "NUM2": builder.create_number_input(1)
        }
    )
    builder.blocks[add_block_id]["parent"] = add1_block_id

    # mapValue = stageData[tempIndex]
    item_block_id = builder.create_reporter_block(
        "data_itemoflist",
        inputs={
            "INDEX": [2, add1_block_id]
        },
        fields={
            "LIST": ["stageData", list_ids['stageData']]
        }
    )
    builder.blocks[add1_block_id]["parent"] = item_block_id

    block_id = builder.create_block(
        "data_setvariableto",
        parent=None,
        inputs={
            "VALUE": [2, item_block_id]
        },
        fields={
            "VARIABLE": ["mapValue", var_ids['mapValue']]
        }
    )
    builder.blocks[item_block_id]["parent"] = block_id
    builder.blocks[repeat_x_id]["inputs"]["SUBSTACK"] = [2, block_id]
    builder.blocks[block_id]["parent"] = repeat_x_id
    current_inner_id = block_id

    # mapValueが2（ゴール）の場合、goalsリストに追加
    # if mapValue = 2
    equals_2_block_id = builder.create_reporter_block(
        "operator_equals",
        inputs={
            "OPERAND1": builder.create_variable_input(var_ids['mapValue'], "mapValue"),
            "OPERAND2": builder.create_number_input(2)
        }
    )

    if_goal_id = builder.create_block(
        "control_if",
        parent=current_inner_id,
        inputs={
            "CONDITION": [2, equals_2_block_id]
        }
    )
    builder.blocks[equals_2_block_id]["parent"] = if_goal_id
    current_inner_id = if_goal_id

    # goalsXにloopXを追加
    block_id = builder.create_block(
        "data_addtolist",
        parent=None,
        inputs={
            "ITEM": builder.create_variable_input(var_ids['loopX'], "loopX")
        },
        fields={
            "LIST": ["goalsX", list_ids['goalsX']]
        }
    )
    builder.blocks[if_goal_id]["inputs"]["SUBSTACK"] = [2, block_id]
    builder.blocks[block_id]["parent"] = if_goal_id
    current_goal_id = block_id

    # goalsYにloopYを追加
    block_id = builder.create_block(
        "data_addtolist",
        parent=current_goal_id,
        inputs={
            "ITEM": builder.create_variable_input(var_ids['loopY'], "loopY")
        },
        fields={
            "LIST": ["goalsY", list_ids['goalsY']]
        }
    )
    current_goal_id = block_id

    # mapValueが3（箱）の場合、boxesリストに追加
    # if mapValue = 3
    equals_3_block_id = builder.create_reporter_block(
        "operator_equals",
        inputs={
            "OPERAND1": builder.create_variable_input(var_ids['mapValue'], "mapValue"),
            "OPERAND2": builder.create_number_input(3)
        }
    )

    if_box_id = builder.create_block(
        "control_if",
        parent=current_inner_id,
        inputs={
            "CONDITION": [2, equals_3_block_id]
        }
    )
    builder.blocks[equals_3_block_id]["parent"] = if_box_id

    # boxesXにloopXを追加
    block_id = builder.create_block(
        "data_addtolist",
        parent=None,
        inputs={
            "ITEM": builder.create_variable_input(var_ids['loopX'], "loopX")
        },
        fields={
            "LIST": ["boxesX", list_ids['boxesX']]
        }
    )
    builder.blocks[if_box_id]["inputs"]["SUBSTACK"] = [2, block_id]
    builder.blocks[block_id]["parent"] = if_box_id
    current_box_id = block_id

    # boxesYにloopYを追加
    block_id = builder.create_block(
        "data_addtolist",
        parent=current_box_id,
        inputs={
            "ITEM": builder.create_variable_input(var_ids['loopY'], "loopY")
        },
        fields={
            "LIST": ["boxesY", list_ids['boxesY']]
        }
    )
    current_box_id = block_id

    # mapValueが4（プレイヤー）の場合、プレイヤー位置を設定
    # if mapValue = 4
    equals_4_block_id = builder.create_reporter_block(
        "operator_equals",
        inputs={
            "OPERAND1": builder.create_variable_input(var_ids['mapValue'], "mapValue"),
            "OPERAND2": builder.create_number_input(4)
        }
    )

    if_player_id = builder.create_block(
        "control_if",
        parent=current_inner_id,
        inputs={
            "CONDITION": [2, equals_4_block_id]
        }
    )
    builder.blocks[equals_4_block_id]["parent"] = if_player_id

    # playerX = loopX
    block_id = builder.create_block(
        "data_setvariableto",
        parent=None,
        inputs={
            "VALUE": builder.create_variable_input(var_ids['loopX'], "loopX")
        },
        fields={
            "VARIABLE": ["playerX", var_ids['playerX']]
        }
    )
    builder.blocks[if_player_id]["inputs"]["SUBSTACK"] = [2, block_id]
    builder.blocks[block_id]["parent"] = if_player_id
    current_player_id = block_id

    # playerY = loopY
    block_id = builder.create_block(
        "data_setvariableto",
        parent=current_player_id,
        inputs={
            "VALUE": builder.create_variable_input(var_ids['loopY'], "loopY")
        },
        fields={
            "VARIABLE": ["playerY", var_ids['playerY']]
        }
    )
    current_player_id = block_id

    # loopXを1増やす（内側ループの最後）
    change_loopx_block = builder.create_block(
        "data_changevariableby",
        parent=current_inner_id,
        inputs={
            "VALUE": builder.create_number_input(1)
        },
        fields={
            "VARIABLE": ["loopX", var_ids['loopX']]
        }
    )

    # loopYを1増やす（外側ループの最後）
    change_loopy_block = builder.create_block(
        "data_changevariableby",
        parent=current_substack_id,
        inputs={
            "VALUE": builder.create_number_input(1)
        },
        fields={
            "VARIABLE": ["loopY", var_ids['loopY']]
        }
    )

    # ゲーム状態を1（プレイ中）に設定
    block_id = builder.create_block(
        "data_setvariableto",
        parent=current_id,
        inputs={
            "VALUE": builder.create_number_input(1)
        },
        fields={
            "VARIABLE": ["gameState", var_ids['gameState']]
        }
    )
    current_id = block_id

    # チャットメッセージ: "倉庫番ゲーム開始！矢印キーで移動"
    chat_block_id = builder.create_reporter_block(
        "minecraft_menu_blockTypes",
        fields={
            "blockTypes": ["stone", None]
        }
    )

    block_id = builder.create_block(
        "minecraft_chat",
        parent=current_id,
        inputs={
            "MESSAGE": builder.create_string_input("倉庫番ゲーム開始！矢印キーで移動")
        }
    )
    current_id = block_id

    # カスタムブロック「マップを描画」を呼び出し
    block_id = builder.create_block(
        "procedures_call",
        parent=current_id,
        inputs={},
        fields={}
    )
    # Note: procedures_callは後で正しいmutationを設定する必要があります
    current_id = block_id

    print(f"[OK] 初期化スクリプト作成完了（ブロック数: 多数）")

    return hat_id


def build_key_handlers(builder, var_ids, list_ids):
    """
    キー入力ハンドラーを構築
    矢印キー（上下左右）とスペースキーの処理
    """

    handlers = []

    # ↑キーが押されたとき（Y-1）
    hat_up = builder.create_hat_block(
        "event_whenkeypressed",
        fields={"KEY_OPTION": ["up arrow", None]},
        x=48,
        y=400
    )
    # movePlayer(0, -1) を呼び出し
    call_move = builder.create_block(
        "procedures_call",
        parent=hat_up
    )
    handlers.append(("up", hat_up))

    # ↓キーが押されたとき（Y+1）
    hat_down = builder.create_hat_block(
        "event_whenkeypressed",
        fields={"KEY_OPTION": ["down arrow", None]},
        x=300,
        y=400
    )
    call_move = builder.create_block(
        "procedures_call",
        parent=hat_down
    )
    handlers.append(("down", hat_down))

    # ←キーが押されたとき（X-1）
    hat_left = builder.create_hat_block(
        "event_whenkeypressed",
        fields={"KEY_OPTION": ["left arrow", None]},
        x=550,
        y=400
    )
    call_move = builder.create_block(
        "procedures_call",
        parent=hat_left
    )
    handlers.append(("left", hat_left))

    # →キーが押されたとき（X+1）
    hat_right = builder.create_hat_block(
        "event_whenkeypressed",
        fields={"KEY_OPTION": ["right arrow", None]},
        x=800,
        y=400
    )
    call_move = builder.create_block(
        "procedures_call",
        parent=hat_right
    )
    handlers.append(("right", hat_right))

    # スペースキーが押されたとき（リセット）
    hat_space = builder.create_hat_block(
        "event_whenkeypressed",
        fields={"KEY_OPTION": ["space", None]},
        x=1050,
        y=400
    )
    # 緑の旗と同じ処理を呼び出し（broadcast "reset"）
    broadcast_reset = builder.create_block(
        "event_broadcast",
        parent=hat_space,
        inputs={
            "BROADCAST_INPUT": builder.create_string_input("reset")
        }
    )
    handlers.append(("space", hat_space))

    print(f"[OK] キーハンドラー作成完了（5個）")

    return handlers


if __name__ == "__main__":
    print("このファイルは直接実行できません")
    print("generate_sokoban_scratch.py から呼び出してください")
